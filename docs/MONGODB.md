# MongoDB 双数据库使用说明

## 1. 定位

项目同时使用 PostgreSQL 与 MongoDB，两者不是主从关系，也不是二选一。

- PostgreSQL/JPA：保存关系明确、需要约束和事务的数据，例如 QQ 用户与群配置、权限、绑定、冷却、审核和调用记录。
- MongoDB：保存结构变化较快、嵌套层级较深或按文档整体读写的数据，例如第三方接口原始快照、复杂详情缓存和后续需要扩展字段的业务文档。

MongoDB 默认关闭，避免尚未配置 MongoDB 的现有部署启动失败。设置 BOT_MONGODB_ENABLED=true 后，PostgreSQL 与 MongoDB 会同时启用。

## 2. 配置

凭证不要写入仓库内的 YAML。部署环境设置：

~~~text
BOT_MONGODB_ENABLED=true
BOT_MONGODB_URI=mongodb://<username>:<password>@<host>:27017/?authSource=admin
BOT_MONGODB_DATABASE=botjava
BOT_MONGODB_MAX_DOCUMENT_BYTES=1048576
BOT_MONGODB_ROLE_STATUS_BAG_REMAINING_FIELD=背包剩余空间
BOT_MONGODB_QUERY_FIELD_WHITELIST_ENABLED=false
BOT_MONGODB_QUERY_FIELD_ALLOWED_FIELDS=
BOT_MONGODB_QUERY_FIELD_DENIED_FIELDS=
BOT_MONGODB_QUERY_FIELD_MAX_ITEMS=200
~~~

如果密码包含特殊字符，必须先进行 URI percent-encoding。生产环境建议启用 TLS，并通过部署平台 Secret 或 JAR 同目录的外部 YAML 提供 URI。

## 3. 代码边界

MongoDB 由以下组件提供：

- BotMongoProperties：配置与启动校验。
- BotMongoConfiguration：独立创建 botMongoClient 和 botMongoTemplate。
- MongoDocumentStore：提供通用 JSON object 文档的 upsert、find、exists 和 delete。

具体业务不要把用户输入直接当集合名。每个业务 Service 应固定自己的集合名：

~~~java
@Service
@ConditionalOnProperty(prefix = "bot.mongodb", name = "enabled", havingValue = "true")
public class PriceSnapshotService {

    private static final String COLLECTION = "jx3_price_snapshot";

    private final MongoDocumentStore documentStore;

    public PriceSnapshotService(MongoDocumentStore documentStore) {
        this.documentStore = documentStore;
    }

    public void save(String itemKey, JsonNode data) {
        documentStore.upsert(COLLECTION, itemKey, data);
    }
}
~~~

实际 Mongo 文档使用稳定信封结构：

~~~json
{
  "_id": "item:1001",
  "payload": {
    "name": "金发·因陀罗",
    "server": "乾坤一掷"
  },
  "createdAt": "MongoDB BSON Date",
  "updatedAt": "MongoDB BSON Date"
}
~~~

find 只返回 payload，不会把 _id 和存储元数据混入业务 JSON。

## 4. 事务原则

项目没有提供 PostgreSQL 与 MongoDB 的分布式事务，也没有注册 Mongo 事务管理器。现有 @Transactional 继续属于 JPA/PostgreSQL。

一次业务如果需要同时修改两个数据库：

1. PostgreSQL 保存权威状态和待处理事件。
2. 事务提交后写 MongoDB。
3. MongoDB 写入失败时保留事件并重试，或由补偿任务恢复。

不要假设一个 @Transactional 能同时回滚 PostgreSQL 和 MongoDB。等具体双写业务确定后，再为该业务实现 outbox、重试和幂等键。

## 5. 当前限制

- 通用存储只接受 JSON object，不接受裸数组或标量。
- 集合名必须由业务代码固定，并满足安全格式。
- 文档 ID 最长 200 个字符且不能包含控制字符。
- 单个 payload 默认最大 1 MB，可配置范围为 1 KB 至 8 MB。
- 当前不自动创建业务索引。
## 6. Lua 脚本角色状态集合

Lua 脚本维护的角色状态集合属于外部既有数据，和 `MongoDocumentStore` 管理的信封文档不是同一种结构。Java 使用专用 `LuaRoleStatusStore` 访问，不使用 `MongoDocumentStore.upsert/delete`，也不改变既有文档身份。

约束如下：

- Java 不创建、删除或修改 MongoDB 索引，也不以新增索引作为功能运行前提。
- Java 不新增或删除 Lua 角色文档；只查询状态，或更新 PostgreSQL 白名单中标记为可写的顶层字段。
- 用户只通过“区服 + 角色名”绑定，不要求账号、MongoDB `_id`、`全局ID` 或其他内部标识；门派是可选的展示信息，不参与 MongoDB 定位。
- PostgreSQL 保存当前群 `member_openid + 区服 + 角色名` 的绑定。QQ 官方规定同一用户在不同群的 `member_openid` 不同，因此绑定不能跨群或 C2C 自动关联。
- 查询使用 MongoDB 顶层字段做区服和角色名的精确等值匹配，并接收全部匹配结果；不得调用 `first()` 或默认选择第一条。
- 0 条结果表示没有脚本状态；1 条正常渲染；多条按独立记录分块渲染，并明确显示匹配数量。
- 更新前先读取全部候选并检查数量上限，再按候选文档现有 `_id` 更新允许写入的字段。相同“区服 + 角色名”命中多条时全部更新；不会把 `_id` 返回给用户。
- `_id`、`全局ID`、`账号`、`角色ID` 以及凭证类字段禁止加入展示或写入白名单。
- 读取和更新数量由 `bot.mongodb.role-status.max-matches` 限制；超过上限时明确拒绝，不静默截取。

群指令：

- `脚本状态 [区服 角色名]`：省略参数时使用个人默认角色，返回 `脚本状态.html` 图片。
- `背包预警`：公开群指令，不检查发送者身份和角色绑定。直接扫描配置的角色集合，但 MongoDB 只投影服务器、角色名和背包剩余空间三个字段；Java 将可解析数值中小于 50 的记录按剩余空间升序生成 `背包预警.html` 图片，每行最多 5 项。标题为“角色名 - 服务器”，下方显示剩余背包空间；没有命中时返回纯文本。字段名由 `bot.mongodb.role-status.server-field`、`role-name-field` 和 `bag-remaining-field` 配置。该全量预警查询不受单角色查询的 `max-matches` 限制。
- `查询信息 区服 角色名 Mongo字段或别名`：查询当前群内本人已绑定角色的指定顶层字段，返回 `查询信息.html` 图片。真实 Mongo 字段名精确匹配优先；未命中真实字段时，以已启用字段配置的 `display_name` 或 `group_name` 作为别名，一个别名可展开为多个 Mongo 字段。图片使用紧凑的 Excel 表格布局，每个查询项上方显示字段名、下方显示字段值，每行最多 5 项并按实际内容区域裁剪；标量按单项展示，数组或集合在对应字段格内逐项展示。全部字段均无值时返回“未查询到数据”。
`角色信息` 是内置查询别名，不要求 PostgreSQL 预先存在字段配置。`查询信息 区服 角色名 角色信息` 固定按顺序读取：`背包剩余空间 -> 背包剩余空间`、`角色金币 -> 金币`、`精力 -> 精力`、`侠行点 -> 侠义点`、`威望 -> 威望`。内置映射优先于数据库别名，但展开后的每个 MongoDB 字段仍分别执行敏感字段、黑名单和可选白名单校验。五人日常和门派日常暂不属于该映射。

- `脚本设置 [区服 角色名] Mongo字段 值`：只能修改自己已绑定角色、且管理员标记为可写的字段。

主号 C2C 字段管理：

- `脚本字段列表`
- `脚本字段设置 Mongo字段 显示名 分组 TEXT|INTEGER|DECIMAL|BOOLEAN|DATETIME READ|WRITE 排序`
- `脚本字段删除 Mongo字段`

字段配置保存在 PostgreSQL `script_status_field`。`display_name` 和 `group_name` 同时作为查询别名；显示名通常定位一个字段，分组名用于展开同组的全部启用字段。例如：

```text
脚本字段设置 角色金币 金币 货币 INTEGER READ 10
脚本字段设置 侠行点 侠义 货币 INTEGER READ 20
查询信息 乾坤一掷 加菲 货币
```

最后一条会把分组别名 `货币` 展开并同时读取 `角色金币` 和 `侠行点`；`查询信息 乾坤一掷 加菲 角色金币` 仍只读取真实字段 `角色金币`。别名匹配兼容全半角、大小写以及空白、点号、下划线、连字符和间隔点差异。MongoDB 更新操作写入现有 `bot_admin_audit_log`，审计不保存字段值、角色名或消息原文。

指定字段查询不把字段名拼入 MongoDB 条件，只在按“区服 + 角色名”精确查询得到的文档中读取已校验的顶层键。命中别名后，展开出的每一个真实字段都必须分别通过同一套敏感字段、黑名单和可选白名单策略；任一字段不允许时整次请求拒绝，不做可能产生误解的部分返回。`_id`、账号、账户、用户名、全局 ID、角色 ID、OpenID、手机号、身份证、邮箱、password、secret、token、cookie、ticket、access key、private key 等内置敏感字段始终拒绝；`BOT_MONGODB_QUERY_FIELD_DENIED_FIELDS` 可用逗号继续追加精确字段名。白名单能力已预留，默认关闭；后续设置 `BOT_MONGODB_QUERY_FIELD_WHITELIST_ENABLED=true` 后，仅 `BOT_MONGODB_QUERY_FIELD_ALLOWED_FIELDS` 中的字段可查询。对象类型不会展开，避免一个顶层对象夹带未授权内部字段；单次图片最多展示 `BOT_MONGODB_QUERY_FIELD_MAX_ITEMS` 项，默认 200。MongoDB 不可用、匹配超过安全上限或驱动异常均转换为群内可读提示，并在服务端保留完整异常堆栈。

## 全库指定字段查询

群内公开指令 `查询全部信息 Mongo字段`（同样支持 `/查询全部信息 Mongo字段`）用于读取角色集合中所有包含该字段且值非空的文档，不要求发送者绑定角色。例如：

```text
查询全部信息 侠行点
查询全部信息 背包剩余空间
```

参数既可以是 MongoDB 顶层真实字段名，也可以是 PostgreSQL 字段配置中的显示名或分组别名；别名展开后的每一个真实字段都会单独经过 `RoleFieldQueryAccessPolicy`。账号、全局 ID、token 等内置敏感字段、配置黑名单及可选白名单规则与单角色 `查询信息` 完全一致，未通过校验时不会访问 MongoDB。

`LuaRoleStatusStore.findAllRoleFields` 使用字段 `exists` 条件，并只 projection 配置的服务器字段、角色名字段和已校验的目标字段，显式排除 `_id`，不会把整份 Lua 文档加载到响应模型。字段不存在、全部为 `null`、空字符串、空列表或对象值时返回 `未查询到“字段名”的数据。`；MongoDB 未启用或驱动异常时返回可读错误文本。

图片模板为 `src/main/resources/static/查询全部信息.html`。模板数据结构为 `queryName / fieldCount / recordCount / itemCount / truncated / records[]`；每个 `record` 包含 `server / roleName / fields[]`，每格上方显示“角色名 - 区服”，下方显示数据库值。网格每行最多 5 个角色并按实际内容裁剪；标量和列表均可展示。全库指令会展示所有具有可读值的匹配记录，不套用单角色 `BOT_MONGODB_QUERY_FIELD_MAX_ITEMS` 限制；该限制仍只用于 `查询信息 区服 角色名 字段`。
## 定时状态推送实现

`MongoDailyProgressPushTask` 已实现 `ScheduledGroupPushTask`，由 `ScheduledGroupPushRunner` 按配置 cron 触发，并将按群生成的图片 `BotResponse` 交给 `GroupPushDispatcher`。群订阅、线程池、主动消息权限、频控和 QQ 发送保持统一处理。

该任务只在 `bot.mongodb.enabled=true` 时加载；群订阅默认关闭。实现继续遵守现有边界：不创建或修改 MongoDB 索引，不新增/删除 Lua 角色文档，不在日志或图片中暴露 `_id`、账号、`全局ID` 或未配置字段。

## 每日 9 点背包预警推送

- `定时背包预警` 注册为 `SCHEDULED` 任务，默认使用运行服务器的系统时区每天 `09:00` 触发；不固定为 `Asia/Tokyo`。
- 群主或群管理员执行 `开启推送 定时背包预警` 后当前群才会接收；执行 `关闭推送 定时背包预警` 即停止。订阅按 `group_open_id` 隔离。
- 每次调度只扫描一次角色集合，并继续只投影服务器、角色名、背包剩余空间三个字段；同一张预警图片复用给所有已订阅群，避免订阅群数量放大 MongoDB 查询。
- 仅当存在背包剩余空间小于 50 的记录时发送；没有命中、MongoDB 未启用或查询失败时跳过本次推送。交互指令 `背包预警` 的可读空结果提示保持不变。
- 实际发送仍受群主动消息总开关、QQ 平台授权与频控约束。默认 cron 可用 `BOT_PUSH_MONGO_BAG_WARNING_CRON` 覆盖。

```yaml
bot:
  push:
    mongo-bag-warning:
      cron: 0 0 9 * * *
```

## 每日 10 点群推送

- `Mongo日常进度` 是独立的定时推送任务，默认按 `Asia/Tokyo` 每天 `10:00` 执行。
- 群必须先执行 `开启推送 Mongo日常进度`，并开启群主动消息；未订阅的群不会进入内容生成。
- 已订阅群若没有 `group_open_id` 级角色绑定，或绑定角色在 MongoDB 中没有匹配文档，不调用 QQ 发送接口。
- PostgreSQL 的 `user_role_binding` 使用 `group_open_id + member_openid + server + role_name` 隔离。MongoDB 仍严格按平铺字段 `服务器 + 角色名` 查询；相同组合命中多条时全部展示。
- 根据 `roles.js` 导入后的 384 条真实记录验证：存在 13 个区服、6 组重复“服务器 + 角色名”，单组最多 56 条；`每日签到任务` 等字段是 Unix 秒，`更新时间` 也可能是 `2026-8-12-10-4-29` 文本，两种格式都会按推送时区转为可读时间。
- 默认动态字段为 `每日签到任务 / 角色金币 / 精力 / 侠行点`，显示名分别为 `上次日常完成 / 金币 / 精力 / 侠义`；区服和角色名是固定列。
- `group_daily_push_field` 是对系统默认字段的群级覆盖：新增字段会保留其余默认列，禁用记录会移除同名默认列；逐项禁用全部动态字段后图片只显示区服和角色名。
- 图片模板为 `src/main/resources/static/日常进度.html`，数据结构为 `columns[{field,name}]` 与 `rows[{server,roleName,recordIndex,values[]}]`，`values` 顺序必须与 `columns` 一致。

配置项：

```yaml
bot:
  push:
    mongo-daily:
      cron: 0 0 10 * * *
      zone: Asia/Tokyo
```


## WSL 本地 MongoDB 验证环境

本机 WSL 使用官方 `mongo:8.0` 镜像，容器名为 `botjava-mongodb`，数据卷为 `botjava-mongodb-data`，端口映射为 `127.0.0.1:27017`，重启策略为 `unless-stopped`。应用数据库为 `toy2`，集合为 `roles`。

首次创建容器时使用官方镜像，并仅监听本机回环地址：

```bash
docker run -d \
  --name botjava-mongodb \
  --restart unless-stopped \
  -p 127.0.0.1:27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=<本地 root 用户名> \
  -e MONGO_INITDB_ROOT_PASSWORD=<本地 root 密码> \
  -v botjava-mongodb-data:/data/db \
  -v /mnt/d/Download/roles.js:/import/roles.js:ro \
  mongo:8.0
```

当前本机容器已经创建并完成应用账号、数据导入和验证，日常测试不需要重复执行 `docker run`。
常用命令：

```bash
docker start botjava-mongodb
docker stop botjava-mongodb
docker logs --tail 100 botjava-mongodb
docker exec botjava-mongodb mongosh "mongodb://qbot:<本地密码>@127.0.0.1:27017/toy2?authSource=toy2"
```

`D:/Download/roles.js` 已导入 `toy2.roles`，实测 384 条、13 个区服、317 个不同角色名。重新导入时脚本会先删除并重建 `roles` 集合：

```bash
docker exec botjava-mongodb mongosh \
  "mongodb://qbot:<本地密码>@127.0.0.1:27017/toy2?authSource=toy2" \
  /import/roles.js
```

项目提供 `DualDatabaseLiveSmokeIT`。设置 `BOT_MONGODB_URI`、`BOT_MONGODB_DATABASE=toy2` 和 PostgreSQL 连接环境变量后，可显式运行该测试；它会在同一个 Spring 上下文中检查 PostgreSQL、MongoDB 和 `LuaRoleStatusStore`。
