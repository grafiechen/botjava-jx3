# 部署指南

## 1. 运行环境

- Java 21。
- Maven 3.9 或使用构建后的可执行 JAR。
- PostgreSQL。
- MinIO 或兼容 S3 的自有文件服务仅在 `BOT_QQ_IMAGE_UPLOAD_MODE=MINIO` 或 URL 媒体验收时需要。
- Chromium 及 Playwright 系统依赖。
- 默认使用 QQ v2 WebSocket 获取消息，不要求公网 webhook 地址；切换到 `HOOK`/`WEBHOOK` 模式时才需要可从公网 HTTPS 访问的 QQ webhook 地址。媒体域名仅在 MinIO/URL 上传模式下需要。

QQ 群消息入口默认使用 QQ v2 WebSocket；`BOT_QQ_MESSAGE_INGRESS_MODE=HOOK` 或 `WEBHOOK` 时改用原 webhook 入口。JX3API WebSocket 是游戏事件推送能力，和 QQ 消息入口相互独立。

QQ 消息权限、项目频控及审核发布边界见 [QQ 能力权限、频率与发布政策](QQ_CAPABILITY_POLICY.md)。

## 2. 构建

Linux 服务器首次安装 Playwright Chromium：

```bash
mvn -B exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install --with-deps chromium"
```

执行测试并构建：

```bash
mvn -B test
mvn -B package -DskipTests
```

产物位于 `target/botjava-0.0.1.jar`。

构建 Docker 镜像：

```bash
docker build -t jx3bot:local .
```

Dockerfile 使用 Ubuntu/Jammy 系 Java 21 镜像，构建阶段执行 Maven package，运行阶段预装 Playwright Chromium、系统依赖和中文字体，支持 HTML/Vue 截图生成图片。容器运行时仍需通过环境变量注入 QQ、JX3API 和 PostgreSQL 等配置；只有选择 `BOT_QQ_IMAGE_UPLOAD_MODE=MINIO` 时才需要 MinIO 配置：

```bash
docker run --rm \
  --env-file .env \
  -p 8081:8081 \
  -p 8082:8082 \
  jx3bot:local
```

## 3. 必需配置

生产环境不得把凭证写入 `application*.yml`。使用环境变量或部署平台的 Secret 管理功能。

### QQ 机器人
QQ 私聊主号配置管理需要配置允许操作的用户 OpenID，多个值用英文逗号分隔：

```bash
BOT_QQ_ADMIN_MASTER_OPENIDS=<master-user-openid-1>,<master-user-openid-2>
```

该配置只控制 C2C 私聊里的运行时配置管理命令。运行时配置写入 `bot_runtime_config`，配置/审核操作审计写入 `bot_admin_audit_log`；首次部署或禁用 Hibernate DDL 自动更新时，执行 [bot-runtime-config.sql](database/bot-runtime-config.sql)。

```text
TX_BOT_TOKEN
TX_BOT_APP_SECRET
TX_BOT_APP_ID
TX_BOT_NUMBER
```

QQ OpenAPI 和 access token 地址可以继续使用配置文件中的官方默认值，也可以按环境覆盖。

在发送任何群消息前，可以先执行无副作用的身份鉴权 smoke test。该测试只调用官方 `GET /users/@me`，输出不包含机器人 ID、名称或凭证，普通 Maven 和 CI 不会自动运行：

```powershell
$env:QQ_OPENAPI_LIVE_SMOKE = "true"
$env:TX_BOT_APP_ID = "<app-id>"
$env:TX_BOT_APP_SECRET = "<secret>"
mvn "-Dtest=QqOpenApiLiveSmokeIT" test
```

群消息发送、文件上传、事件回复和主动消息会产生真实平台副作用，不包含在身份 smoke test 中，必须继续按本文发布检查及 `QQ_CAPABILITY_POLICY.md` 在指定测试群人工触发验收。

需要验证真实群发送时，使用单模式 runner。它要求测试群 openid、强确认短语和一种 `QQ_GROUP_LIVE_MODE`，每次最多发送一条最终群消息；普通 Maven 和 CI 不执行：

```powershell
$env:QQ_GROUP_LIVE_SMOKE = "true"
$env:QQ_GROUP_LIVE_CONFIRM = "SEND_TO_TEST_GROUP"
$env:QQ_GROUP_LIVE_MODE = "REPLY"
$env:QQ_GROUP_LIVE_OPENID = "<test-group-openid>"
$env:QQ_GROUP_LIVE_MSG_ID = "<fresh-source-message-id>"
$env:TX_BOT_APP_ID = "<app-id>"
$env:TX_BOT_APP_SECRET = "<secret>"
mvn "-Dtest=QqGroupMessageLiveSmokeIT" test
```

可选模式及额外变量：

| 模式 | 行为 | 必需额外变量 |
| --- | --- | --- |
| `REPLY` | `msg_id + msg_seq=1` 文本被动回复 | `QQ_GROUP_LIVE_MSG_ID` |
| `REFERENCE` | 引用来源消息的文本被动回复 | `QQ_GROUP_LIVE_MSG_ID` |
| `EVENT` | `event_id` 事件被动回复 | `QQ_GROUP_LIVE_EVENT_ID` |
| `ACTIVE` | 不携带被动字段的主动文本消息 | 无；仅验证 OpenAPI 传输，应用数据库授权与频控另行验收 |
| `IMAGE` | 上传已有 HTTPS 图片 URL 并作为被动媒体回复 | `QQ_GROUP_LIVE_MSG_ID`、`QQ_GROUP_LIVE_IMAGE_URL` |
| `AUDIO` | 上传 HTTPS 音频并作为被动语音回复 | `QQ_GROUP_LIVE_MSG_ID`、`QQ_GROUP_LIVE_AUDIO_URL` |
| `MARKDOWN` | Markdown + 自定义 Keyboard 被动回复 | `QQ_GROUP_LIVE_MSG_ID` |
| `ARK` | Ark 模板 23 被动回复 | `QQ_GROUP_LIVE_MSG_ID` |

`msg_id` 和 `event_id` 必须来自目标测试群的近期真实事件。`IMAGE` 与 `AUDIO` live runner 仍使用外部 HTTPS URL，用于验证 URL 上传分支；模板生成图片默认走 QQ v2 分片上传，不依赖媒体域名。一次只运行一种模式；切换模式前重新确认平台频率限制。runner 不输出 group openid、消息/事件 ID、请求体或平台响应正文。

### JX3API

JX3API 默认启用 HTTP 查询指令，JX3API 游戏事件 WS 推送默认关闭。常用开关：

```text
JX3API_ENABLED=true
JX3API_HTTP_ENABLED=true
JX3API_WS_ENABLED=false
```

启用 JX3API HTTP 功能时至少需要：

```text
JX3API_API_TOKEN
JX3API_API_V2_TOKEN=<optional-lv2-token>
JX3API_TICKET
JX3API_DEFAULT_SERVER
```

实际变量名需要与部署环境映射到 `jx3api.api.*`。其中 `ticket` 仅用于需要推栏数据的接口；`JX3API_API_V2_TOKEN` 用于官方 `x-level=2` HTTP 接口，未配置时回退使用 `JX3API_API_TOKEN`。`JX3API_ENABLED=false` 或 `JX3API_HTTP_ENABLED=false` 时，已知 JX3 群指令会回复“该功能未开启。”，不会调用 JX3API。

需要接入 JX3API 游戏事件 WS 推送时，显式配置：

```text
JX3API_WS_ENABLED=true
JX3API_WS_URL=<wss-url>
JX3API_WS_TOKEN=<secret>
JX3API_WS_RECONNECT_MAX_TIMES=5000
JX3API_WS_RECONNECT_DELAY_SECONDS=30
```

这组配置只控制 JX3API 游戏事件推送，和 `BOT_QQ_MESSAGE_INGRESS_MODE=WS` 的 QQ 消息入口不是同一个 WebSocket。初次接入失败、断线、传输异常和 Ping 失败都会走统一重连调度；`JX3API_WS_RECONNECT_DELAY_SECONDS` 不允许低于 30，重复重连请求会合并。
JX3API WS 可以在关闭 HTTP 查询时单独启用：保留 `JX3API_ENABLED=true`、设置 `JX3API_HTTP_ENABLED=false`，并正常配置上述 WS 参数。`JX3API_WS_TOKEN` 是 JX3API 提供的专用 WS token，不是 QQ token，也不是 JX3API HTTP/LV.2 token。

正常运行时会打印以下生命周期日志，日志不会输出 WS token：

应用完成启动后，无论开关状态都会打印一条 JX3API WS 状态摘要。关闭时明确显示 `jx3api.enabled=false` 或 `jx3api.ws.enabled=false`；开启时只显示 endpoint 主机、token 是否已配置和重连间隔，不输出 token、URL 路径或查询参数。

- `JX3API WebSocket 连接任务已调度`：包含触发原因、等待秒数、当前/最大次数。
- `开始连接 JX3API WebSocket`：一次真实握手开始。
- `JX3API WebSocket 连接成功` 与 `连接已确认`：握手完成并重置重试计数。
- `JX3API WebSocket 连接已断开`：包含远端地址、关闭码、原因和下一次重试秒数。
- `JX3API WebSocket 连接失败` / `传输异常` / `Ping 失败`：打印完整异常堆栈，随后进入统一退避。
- `接收到 JX3API WS 消息`：记录 action、事件名和分组；正文解析同时支持 `data` 与 `detail`。正文缺失时只告警并忽略，不会因空 DTO 中断 WS 收包线程。
- `开始停止 JX3API WebSocket` 与 `已停止`：应用主动停机；该场景明确记录“不再重连”。

### QQ 群消息内容排障日志

群消息进入命令执行层后，会在当前 `invocationId` 下输出：

```text
收到群消息，content=>开服 乾坤一掷
未匹配到可处理的群消息命令，content=>未识别的内容
```

内容中的换行、制表符和控制字符会压缩为空格，`token`、`ticket`、`secret`、密码等字段会通过 `SensitiveDataUtil` 脱敏；单条内容最多记录 1000 个字符，超出部分明确标记为已截断。日志不额外打印群、成员或消息标识，使用 MDC `invocationId` 与 QQ payload、dispatch 和后续处理日志关联。
#### 在线 HTTP smoke test

普通 `mvn test` 只执行离线 contract，不访问 JX3API。需要用目标环境凭证验收官方 HTTP 接口时，额外提供一个真实、可查询的服务器、角色和用于骗子查询的 QQ 号，然后显式执行：

```powershell
$env:JX3API_LIVE_SMOKE = "true"
$env:JX3API_API_TOKEN = "<secret>"
$env:JX3API_TICKET = "<secret>"
$env:JX3API_SMOKE_SERVER = "<server>"
$env:JX3API_SMOKE_ROLE = "<role>"
$env:JX3API_SMOKE_UID = "<qq-number>"
$env:JX3API_SMOKE_DELAY_MS = "500"
mvn "-Dtest=Jx3ApiLiveSmokeIT" test
```

runner 从机器可读覆盖矩阵生成 79 个非语音 HTTP contract命令，复用真实 `REGEX -> Action -> Jx3RequestUtil -> DTO` 链路；阿里语音按独立验收项执行。输出只包含 `MethodEnum`、固定接口路径、`SUCCESS/EMPTY/FAILED` 和官方 code，不输出命令、请求参数或响应数据。`EMPTY` 表示接口成功但当前条件下无数据，不等价于 DTO 在线反序列化成功；更换有效查询条件后应再次验证。测试不会由普通 Maven 或 CI 自动执行。

### 指令发布模式

生产环境必须显式配置：

```text
BOT_COMMAND_RUNTIME_MODE=PRODUCTION
```

可选值为 `PRODUCTION`、`TEST`、`REVIEW`。`TEST` 会允许已开启群实验开关的群使用实验指令；`REVIEW` 仅用于部署审核分支和审核专用指令，不应作为正式环境配置。

### PostgreSQL

```text
DB_USERNAME
DB_PASSWORD
```

默认连接地址为 `jdbc:postgresql://localhost:5432/postgres?currentSchema=public`。生产环境应通过 Spring 配置覆盖完整 JDBC URL。

spring.jpa.hibernate.ddl-auto=update 可以增加字段，但不会可靠删除旧唯一约束。无论当前使用 update 还是 none，从旧版升级角色绑定功能时都必须执行下面的 user-info.sql；它会删除旧的三列唯一约束，并建立 group_open_id + member_openid + server + role_name 群级唯一约束：

```bash
psql "$DATABASE_URL" -f docs/database/group-info.sql
psql "$DATABASE_URL" -f docs/database/user-info.sql
psql "$DATABASE_URL" -f docs/database/group-command-setting.sql
psql "$DATABASE_URL" -f docs/database/group-command-cooldown.sql
psql "$DATABASE_URL" -f docs/database/jx3-api-cache.sql
psql "$DATABASE_URL" -f docs/database/command-invocation.sql
psql "$DATABASE_URL" -f docs/database/appearance-name-alias.sql
psql "$DATABASE_URL" -f docs/database/script-status-field.sql
psql "$DATABASE_URL" -f docs/database/group-push-subscription.sql
psql "$DATABASE_URL" -f docs/database/push-event-receipt.sql
```

user-info.sql 会为旧 user_info 表幂等增加 school 字段，并保留现有默认角色及多角色数据。若日志仍出现 uk_user_role_binding_account_role 冲突，说明目标数据库尚未执行该脚本；只替换 JAR 或重启应用不能修复数据库约束。

`group-info.sql` 会增加主动消息本地开关、QQ 平台授权和共享频控字段，并为 `open_group_id` 建立唯一索引。旧库执行脚本前先检查重复群记录；若查询有结果，应先按业务数据合并重复行：

```sql
SELECT open_group_id, COUNT(*)
FROM group_info
WHERE open_group_id IS NOT NULL
GROUP BY open_group_id
HAVING COUNT(*) > 1;
```

群主动消息默认关闭，必须同时满足两项条件：群主或管理员执行 `群设置 主动消息 开启`，并且 QQ 平台通过当前消息入口投递 `GROUP_MSG_RECEIVE`。`GROUP_MSG_REJECT`、`GROUP_DEL_ROBOT` 或重新加入群会把平台授权置为关闭，但不会改写管理员的本地开关。默认每群 60 秒最多一条主动消息，频控占位保存在数据库中，多应用实例共享；可通过以下环境变量调整：

```text
BOT_QQ_MESSAGE_INGRESS_MODE=WS
BOT_QQ_WEBSOCKET_INTENTS=33554432
BOT_QQ_WEBSOCKET_SHARD_ID=0
BOT_QQ_WEBSOCKET_SHARD_COUNT=1
BOT_QQ_WEBSOCKET_USE_GATEWAY_BOT=true
BOT_QQ_WEBSOCKET_RECONNECT_DELAY_SECONDS=5
BOT_QQ_WEBSOCKET_HANDSHAKE_CHECK_SECONDS=10
BOT_QQ_ACTIVE_MESSAGE_MIN_INTERVAL_SECONDS=60
BOT_QQ_IMAGE_UPLOAD_MODE=CHUNK
```

满足上述条件后，群主或管理员可使用 `群公告 内容` 发布主动消息。发送被平台或本地策略拒绝时，机器人会通过来源消息被动回复具体原因；公告 Action 不直接调用 QQ 接口。

群推送任务还需要单独订阅，数据库无记录时默认关闭：

```text
推送列表
+
+关闭推送 开服状态
关闭推送 开服状态
```

`推送列表`、`开启推送`、`关闭推送` 仅允许群主或群管理员执行，数据按 `group_open_id` 隔离。`推送列表` 返回表格图片，始终列出注册表中的全部项目：QQ WebSocket 的 4 类群状态事件、JX3API WebSocket 的全部实时事件以及自定义定时任务；数据库没有订阅行的项目显示为“未开启”。开启后，QQ WS 群状态事件只回推事件所属群，JX3API WS 事件只投递到已开启同名任务的群；关闭的群不会进入 QQ 发送。普通 QQ 群消息是指令入口，不注册为推送任务，避免消息回显循环。

所有主动推送仍同时受本群“主动消息”总开关、QQ 平台授权和频控约束。QQ/JX3API WS 实时事件与项目自定义定时任务共用统一投递线程池。`定时背包预警` 默认每天 09:00 按运行服务器的系统时区触发；只向执行过 `开启推送 定时背包预警` 的群发送，当前没有背包剩余空间小于 50 的角色时不发送。

群指令调用记录默认保留 30 天，并在每天 `03:15` 清理。可通过以下环境变量调整：

```text
BOT_COMMAND_AUDIT_RETENTION_DAYS=30
BOT_COMMAND_AUDIT_CLEANUP_CRON=0 15 3 * * *
BOT_COMMAND_STATISTICS_DEFAULT_DAYS=7
BOT_COMMAND_STATISTICS_MAX_DAYS=90
```

群指令冷却状态保存在 `group_command_cooldown`，因此多应用实例共享同一冷却窗口。Action 或 QQ 发送尚未结束时使用执行中租约防止重复处理；实例异常退出后默认 300 秒可恢复，允许通过以下环境变量在 30 至 3600 秒范围内调整：

```text
BOT_COMMAND_COOLDOWN_IN_FLIGHT_TIMEOUT_SECONDS=300
```

明确配置缓存策略的 JX3API 公共查询会写入 `jx3_api_cache`，多应用实例共享同一 TTL 缓存。token 不进入缓存键，失败响应、实时角色查询和超过 1 MB 的响应不会缓存；过期记录默认每天 `03:45` 清理：

```text
JX3API_CACHE_CLEANUP_CRON=0 45 3 * * *
```

群主或管理员可使用 `群统计` 或 `群统计 30` 查看当前群的聚合结果；统计不返回成员标识、消息原文或请求参数。

`command_invocation` 不保存原始聊天内容和请求参数。生产环境应按业务量制定数据保留和归档周期，避免调用记录无限增长。

#### PostgreSQL 双实例验收

上线多实例前，应在目标 PostgreSQL 中创建一个空的专用 schema。schema 名必须以 `botjava_smoke_` 开头，禁止使用 `public` 或生产业务 schema：

```sql
CREATE SCHEMA botjava_smoke_acceptance AUTHORIZATION botjava_smoke_user;
```

显式设置下列变量后运行双实例验收。JDBC URL 的 `currentSchema` 必须与 `POSTGRES_LIVE_SCHEMA` 完全一致；普通 `mvn test` 和 CI 不会连接该数据库：

```powershell
$env:POSTGRES_LIVE_SMOKE = "true"
$env:POSTGRES_LIVE_CONFIRM = "USE_DEDICATED_SMOKE_SCHEMA"
$env:POSTGRES_LIVE_JDBC_URL = "jdbc:postgresql://db.example.com:5432/botjava?sslmode=require&currentSchema=botjava_smoke_acceptance"
$env:POSTGRES_LIVE_USERNAME = "botjava_smoke_user"
$env:POSTGRES_LIVE_PASSWORD = "<password>"
$env:POSTGRES_LIVE_SCHEMA = "botjava_smoke_acceptance"
mvn "-Dtest=PostgresTwoInstanceLiveSmokeIT" test
```

runner 会启动两个相互独立的 Spring/JPA 上下文，并验证群绑定、个人绑定、指令冷却竞争及过期租约接管、主动消息频控竞争、JX3API 共享缓存和调用审计跨实例可见。执行前它要求所有业务表均为空；空库检查失败时不会执行任何清理，通过后才会在结束阶段删除本次 schema 中的业务行，并保留 schema 和 Hibernate 创建的表。该账号只能获得专用 smoke schema 权限，不得授予生产 schema 的写权限。

### 运行指标

Actuator 默认在本机 `127.0.0.1:8082` 提供健康检查和 Prometheus 指标，不经过 QQ webhook 的业务端口：

```text
GET http://127.0.0.1:8082/actuator/health
GET http://127.0.0.1:8082/actuator/prometheus
```

自定义监听地址和端口：

```text
MANAGEMENT_SERVER_ADDRESS=127.0.0.1
MANAGEMENT_SERVER_PORT=8082
```

QQ token 与 OpenAPI 请求默认 10 秒超时，可设置为 1 至 60 秒：

```text
TX_BOT_REQUEST_TIMEOUT_SECONDS=10
```

默认的 `/actuator/health` 不访问外部 QQ 服务。需要同时验证 QQ 鉴权和连通性时，可显式开启官方 `GET /users/@me` 探测：

```text
TX_BOT_HEALTH_CHECK_ENABLED=true
```

启用后，QQ 认证失败、限流、超时或网络故障会使整体健康状态变为 `DOWN`。健康详情保持隐藏，探测结果不包含机器人 ID、用户名、token 或上游响应；部署平台配置自动重启前，应先确认是否希望把 QQ 的短暂波动视为应用故障。

核心业务指标包括 `bot_command_duration_*`、`bot_jx3_request_duration_*`、`bot_jx3_cache_requests_total` 和 `bot_qq_request_duration_*`。标签只包含指令、固定接口路径、操作及结果类型，不包含群 openid、成员 openid、消息内容或角色名。

数据库共享缓存的 5 分钟命中率可以使用以下 PromQL；只有明确配置缓存策略的接口才计入 hit/miss：

```promql
sum(rate(bot_jx3_cache_requests_total{result="hit"}[5m]))
/
sum(rate(bot_jx3_cache_requests_total[5m]))
```

Prometheus 与应用不在同一主机时，可以把管理地址改为内网地址，并通过防火墙或反向代理鉴权限制来源；不要直接把 Actuator 管理端口暴露到公网。

目标环境可显式执行 Prometheus 指标 smoke test。该测试会读取 `OBSERVABILITY_PROMETHEUS_URL` 指向的文本端点，验证四类 `bot_*` 指标已被暴露，并确认响应文本中没有群 openid、成员 openid、消息 ID、角色名或消息内容等标签；输出只包含 host 和 HTTP 状态，不打印完整 URL 或响应正文：

```powershell
$env:OBSERVABILITY_LIVE_SMOKE = "true"
$env:OBSERVABILITY_PROMETHEUS_URL = "http://127.0.0.1:8082/actuator/prometheus"
mvn "-Dtest=ObservabilityLiveSmokeIT" test
```

### 图片上传模式与 MinIO

```text
MINIO_ENDPOINT
MINIO_PUBLIC_URL
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
MINIO_BUCKET
```

模板图片默认使用 QQ v2 本地分片上传，不需要 MinIO 或公网媒体域名。只有显式设置 `BOT_QQ_IMAGE_UPLOAD_MODE=MINIO` 时，图片模板才会先在 `target/generated-images/html` 生成 PNG，再上传 MinIO，并把 `MINIO_PUBLIC_URL` 拼出的公网 HTTPS URL 提交给 QQ。`MINIO_ENDPOINT` 是应用连接对象存储使用的地址，可以是内网 HTTP；`MINIO_PUBLIC_URL` 是 QQ 拉取媒体使用的公网 HTTPS 基础域名，两者不得混为一谈。

建议为 bucket 配置独立域名，例如 `media.example.com`，由 Nginx 或 Caddy 反向代理 MinIO。业务代码只依赖最终可公开访问的 URL，不依赖具体文件服务器产品。

选择 `MINIO` 模式或需要验收 URL 上传分支时，可显式执行媒体域名端到端验收。runner 会使用现有 Vue 模板生成真实 PNG，以唯一对象名上传，再通过 `MINIO_PUBLIC_URL` 匿名回读并校验 HTTP 200、`image/png`、5 MB 上限和 PNG 签名；不会覆盖或删除既有对象：

```powershell
$env:MINIO_LIVE_SMOKE = "true"
$env:MINIO_LIVE_CONFIRM = "UPLOAD_AND_FETCH_TEST_IMAGE"
$env:MINIO_ENDPOINT = "http://minio.internal:9000"
$env:MINIO_PUBLIC_URL = "https://media.example.com"
$env:MINIO_ACCESS_KEY = "<access-key>"
$env:MINIO_SECRET_KEY = "<secret-key>"
$env:MINIO_BUCKET = "qbot"
mvn "-Dtest=MinioMediaDomainLiveSmokeIT" test
```

成功后输出的 `QQ_GROUP_LIVE_IMAGE_URL` 可直接交给 QQ 群发送 runner 的 `IMAGE` 模式，完成 QQ 平台拉取验收。测试对象会保留在 bucket 中作为证据，后续由对象生命周期策略统一清理。

### 阿里语音

语音功能默认关闭。启用时配置：

```text
JX3_SOUND_ENABLED=true
JX3_SOUND_APPKEY
JX3_SOUND_ACCESS
JX3_SOUND_SECRET
JX3_SOUND_VOICE=Aitong
JX3_SOUND_FORMAT=mp3
JX3_SOUND_SAMPLE_RATE=16000
JX3_SOUND_VOLUME=50
JX3_SOUND_SPEECH_RATE=0
JX3_SOUND_PITCH_RATE=0
```

配置完成后可以显式执行阿里语音生成 smoke test。该测试会请求真实 JX3API 语音转换接口，并验证返回的是 HTTPS 音频地址；输出只包含音频 host，不打印完整音频 URL、appkey、access 或 secret：

```powershell
$env:JX3_SOUND_LIVE_SMOKE = "true"
$env:JX3_SOUND_ENABLED = "true"
$env:JX3API_API_TOKEN = "<secret>"
$env:JX3_SOUND_APPKEY = "<appkey>"
$env:JX3_SOUND_ACCESS = "<access>"
$env:JX3_SOUND_SECRET = "<secret>"
$env:JX3_SOUND_LIVE_TEXT = "botjava-jx3 sound live smoke"
mvn "-Dtest=SoundConverterLiveSmokeIT" test
```

此外，目标群必须开启实验功能。生成的音频 URL 会通过 QQ 群文件接口按语音类型上传，再作为 media 消息发送。真实平台验收时，将生成出的 HTTPS MP3 URL 填入 `QQ_GROUP_LIVE_AUDIO_URL`，并使用 `QQ_GROUP_LIVE_MODE=AUDIO` 执行 `QqGroupMessageLiveSmokeIT`。

## 4. 启动

项目根目录提供 `start.sh`，适用于将脚本、JAR 和外部 YAML 一起部署到常驻 Linux 主机。脚本优先读取同目录 `application.yml`，不存在时回退到 `config/application.yml`；配置文件缺失会直接终止，避免误用 JAR 内默认数据库配置。

```bash
chmod +x start.sh
./start.sh
```

也可以直接使用 POSIX sh：

```bash
sh start.sh
```

Windows 主机使用同目录的 `start.bat`。双击 BAT 或在 CMD 中执行后，Java 会直接运行在当前可见窗口；不创建后台进程、不写 PID 文件，也不重定向标准输出。日志会实时显示，Java 退出或被系统终止后窗口显示退出码并暂停：

```bat
start.bat
```

脚本优先读取同目录 `application.yml`，其次读取 `config\application.yml`。默认使用 `botjava-0.0.1.jar`、端口 `8081` 和 `-Xms128m -Xmx512m`，避免 JVM 堆无限增长挤占小内存主机。脚本通过 `chcp 65001` 将当前 CMD 切换为 UTF-8，并固定 JVM 标准输出、标准错误及 Spring Boot 控制台日志为 UTF-8；修改 `JAVA_OPTS` 不会覆盖这些编码参数。可在启动前覆盖参数：

```bat
set SERVER_PORT=8081
set JAVA_OPTS=-Xms128m -Xmx768m
set CONFIG_FILE=application.yml
start.bat
```

关闭应用时在该窗口按 `Ctrl+C`。前台运行只能让退出原因和日志可见，不能替代操作系统内存保护；如果仍被杀，应继续降低 `-Xmx`，并为 Chromium 图片渲染进程预留额外内存。

默认使用 `botjava-0.0.1.jar`、端口 `8081`，日志写入 `logs/botjava.log`，PID 写入 `botjava.pid`。可通过环境变量覆盖：

```bash
JAR_NAME=botjava-0.0.1.jar \
CONFIG_FILE=./application.yml \
SERVER_PORT=8081 \
JAVA_OPTS="-Xms256m -Xmx1g" \
sh start.sh
```

查看日志和停止进程：

```bash
tail -f logs/botjava.log
kill "$(cat botjava.pid)"
```

脚本会检查已有 PID，存活时不会重复启动；发现失效 PID 会先清理。生产环境长期运行仍建议使用 systemd、Docker 或其他进程管理器，并把日志目录挂载到持久化磁盘。
## 5. QQ 回调

- 应用监听端口以 Spring `server.port` 为准。
- 默认消息入口为 QQ v2 WebSocket；webhook 路径 `/bot/message` 仅在 `BOT_QQ_MESSAGE_INGRESS_MODE=HOOK` 或 `WEBHOOK` 时处理请求。
- webhook 模式公网入口必须使用 HTTPS，并原样转发请求体；`op=0` 事件推送验签依赖未经改写的 UTF-8 原始 body。
- webhook 模式公网入口必须转发 QQ 官方签名头 `X-Signature-Ed25519` 和 `X-Signature-Timestamp`。应用使用 `X-Signature-Timestamp + 原始 body` 做 Ed25519 验签，失败时返回 `401` 且不会进入任何 Action。
- `op=13` 回调地址验证不走事件推送验签；应用使用 bot secret 对 `event_ts + plain_token` 签名并返回 `plain_token/signature`。
- `GROUP_AT_MESSAGE_CREATE` 与 `GROUP_MESSAGE_CREATE` 使用同一群消息处理链路。

本地或预发布环境可以使用回放脚本检查 payload 结构。普通 `op=0` 事件必须携带一组有效签名头，否则会被入口验签拒绝：

```powershell
./tools/replay-payload.ps1 `
  .\docs\testing\payloads\group-message-create.json `
  http://localhost:8081/bot/message `
  -Timestamp "<X-Signature-Timestamp>" `
  -Signature "<X-Signature-Ed25519>"
```

回放样例位于 `docs/testing/payloads/group-message-create.json`。

## 6. 发布检查

1. `./tools/verify-utf8.ps1`、`git diff --check` 和 `mvn test` 全部通过。
2. Playwright 能生成非空 PNG。
3. 默认图片上传模式为 QQ v2 分片上传；如使用 `BOT_QQ_IMAGE_UPLOAD_MODE=MINIO` 或 URL 媒体 runner，MinIO 上传后的 URL 必须可从公网匿名读取。
4. 默认 WS 模式下确认应用已连接 QQ gateway 并收到群消息；如使用 webhook 模式，确认地址验证和事件推送验签成功，反向代理未改写 body 且已转发 `X-Signature-Ed25519`、`X-Signature-Timestamp`。
5. 群内文本和图片各完成一次真实发送。
6. 启用语音时，额外验证 MP3 URL 和 QQ 群语音发送。
7. 日志中不得出现 token、ticket、appkey、secret 或数据库密码。
8. 确认 `BOT_COMMAND_RUNTIME_MODE` 与部署环境一致，正式环境必须为 `PRODUCTION`。
9. 检查 `/actuator/health`，并确认 Prometheus 能采集三个 `bot_*_duration` 指标族。
10. 运行外部验收前，可先用 `./tools/check-external-acceptance-env.ps1 -Scope all` 做环境变量预检查；脚本只输出检查结果，不打印变量值。具备真实凭证和确认变量后，可用 `./tools/invoke-external-acceptance.ps1 -Scope <scope>` 统一执行对应 live smoke；不确定时先加 `-DryRun` 查看将执行的测试类。
11. 正式部署前，可用 `./tools/check-external-acceptance-env.ps1 -Scope production` 检查 QQ、JX3API、数据库和媒体域名的必需生产变量是否齐备；该检查同样不打印变量值。
12. 外部验收完成后，可先用 `./tools/new-external-acceptance-record.ps1` 生成脱敏记录草稿，再按 `docs/testing/external-acceptance-record-template.md` 整理结果，并追加到 `docs/PROJECT_DESIGN.md`。

CI 使用 `.github/workflows/ci.yml`，在 Ubuntu 22.04、Java 21 环境对 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件执行 UTF-8 检查，校验本次 push 或 PR 变更中的 Git 空白错误，安装 Chromium 后运行完整测试。
### MongoDB（可选的第二数据库）

MongoDB 与 PostgreSQL 并行使用，不会替换现有 JPA 数据源。未使用 Mongo 功能时保持默认关闭；需要双数据库模式时配置：

~~~text
BOT_MONGODB_ENABLED=true
BOT_MONGODB_URI=mongodb://<username>:<password>@<host>:27017/?authSource=admin
BOT_MONGODB_DATABASE=botjava
BOT_MONGODB_MAX_DOCUMENT_BYTES=1048576
BOT_MONGODB_ROLE_STATUS_COLLECTION=roles
BOT_MONGODB_ROLE_STATUS_SERVER_FIELD=服务器
BOT_MONGODB_ROLE_STATUS_ROLE_NAME_FIELD=角色名
BOT_MONGODB_ROLE_STATUS_MAX_MATCHES=20
~~~

MongoDB URI 必须通过环境变量或外部 Secret 提供，禁止提交真实账号和密码。详细数据边界、受控字段更新和跨库事务原则见 [MONGODB.md](MONGODB.md)。生产环境关闭 Hibernate 自动建表时，还需执行 `docs/database/script-status-field.sql`。

## 全局外部请求限速与日常推送

生产默认对所有 HTTP、MinIO SDK 操作、QQ 富媒体分片 PUT、远程图片下载以及 QQ/JX3API WebSocket 握手使用同一进程级限速器，启动间隔至少 500ms：

```bash
BOT_OUTBOUND_HTTP_RATE_LIMIT_ENABLED=true
BOT_OUTBOUND_HTTP_MAX_REQUESTS_PER_SECOND=2
BOT_PUSH_MONGO_DAILY_CRON="0 0 10 * * *"
BOT_PUSH_MONGO_BAG_WARNING_CRON="0 0 9 * * *"
BOT_PUSH_MONGO_DAILY_ZONE=Asia/Tokyo
```

每秒上限只允许配置为 1 或 2。多实例部署时该上限按实例计算；若需要全局两次/秒，应在出口网关增加共享限流或只让一个实例执行外部任务。

部署前执行：

- `docs/database/user-info.sql`
- `docs/database/group-command-permission-grant.sql`
- `docs/database/group-daily-push-field.sql`
- `docs/database/group-push-subscription.sql`

授权某个群成员配置日常推送字段：

```sql
INSERT INTO group_command_permission_grant(group_open_id, member_openid, permission_key)
VALUES ('群 openid', '成员 member_openid', 'DAILY_PUSH_FIELD_CONFIG')
ON CONFLICT (group_open_id, member_openid, permission_key)
DO UPDATE SET enabled = TRUE, update_time = CURRENT_TIMESTAMP;
```

`日常推送字段` 可公开查看；`日常推送字段添加 Mongo字段 显示名 [类型] [排序]` 和 `日常推送字段删除 Mongo字段` 需要上述数据库授权。类型支持 `TEXT / INTEGER / DECIMAL / BOOLEAN / DATETIME`。

QQ WebSocket 使用连接 generation 隔离回调并合并重复重连。旧 session 的关闭、传输错误、心跳或握手超时不能停止新连接；日志中的 `generation` 可用于判断一次完整连接生命周期。


## Webhook 防重放与请求限制

默认 WS 入站不受以下配置影响。切换到 `HOOK`/`WEBHOOK` 时，事件签名除 Ed25519 校验外还会检查签名时间戳的新鲜度，并在 JSON 解析前限制请求体大小：

```bash
BOT_QQ_WEBHOOK_MAX_SIGNATURE_AGE_SECONDS=300
BOT_QQ_WEBHOOK_MAX_BODY_BYTES=1048576
```

时间戳允许秒或毫秒格式，时间偏差默认不得超过 5 分钟。请求体默认最多 1 MiB，超限返回 HTTP 413。反向代理层仍建议配置相同或更小的请求体上限。

HTML 图片模板的数据使用 Base64 JSON 注入，Chromium 只允许加载 `bot.local` classpath 资源，其他 HTTP/HTTPS 请求会被阻断。消息上传结束后生成的临时 PNG 会立即删除。
## JX3API WebSocket 状态监控

应用对外暴露以下 Actuator 接口：

- `/actuator/health`：应用健康状态。JX3API WS 显式启用但尚未连接或已经断开时，`jx3ApiWebSocket` 健康项为 `DOWN`；配置关闭时保持 `UP`，并标记为未启用。
- `/actuator/prometheus`：Prometheus 指标。`bot_jx3_websocket_connected` 为 `1` 表示已连接，为 `0` 表示未连接或未启用。

群主或管理员可发送 `状态检查` 或 `/状态检查`，在群内查看上述两个接口是否可用以及当前 JX3API WS 状态。指令只读取应用内状态，不会自请求公网，也不会返回 token、WS URL、数据库地址等配置内容。

生产监控应直接采集 Actuator 接口，不应依赖 QQ 指令轮询。建议对 `bot_jx3_websocket_connected == 0` 持续超过一个重连周期（默认 30 秒）设置告警，并结合连接、断开和重连日志排查。
