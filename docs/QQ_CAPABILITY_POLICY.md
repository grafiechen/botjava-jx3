# QQ 能力权限、频率与发布政策

本文记录项目对 QQ 官方能力的使用边界。平台准入、审核结果和动态频率限制以腾讯机器人开放平台控制台及官方文档为准；本文只声明仓库已经实现并可验证的本地约束。

官方入口：

- [腾讯机器人开放平台文档](https://bot.q.qq.com/wiki/develop/api/)
- [腾讯机器人 OpenAPI v2 文档](https://bot.q.qq.com/wiki/develop/api-v2/)
- [群聊发送消息](https://bot.q.qq.com/wiki/develop/api-v2/autogen/api/v2_groups_group_openid_messages.post.html)
- [获取机器人群内状态](https://bot.q.qq.com/wiki/develop/api-v2/autogen/api/v2_groups_group_openid_bot_state.get.html)
- [富媒体消息](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/message/rich-media.html)

## 消息入口

| 事件 | 本地处理 | 权限边界 |
| --- | --- | --- |
| `GROUP_MESSAGE_CREATE` | 进入群指令统一执行链 | 指令根据 `CommandAccess` 判断公开或群管理权限 |
| `GROUP_AT_MESSAGE_CREATE` | 与普通群消息共用同一执行链 | 与普通群消息完全相同 |
| `INTERACTION_CREATE` | 进入类型化互动 handler | 只接受已注册业务前缀，不执行任意回调文本 |
| `GROUP_ADD_ROBOT` / `GROUP_DEL_ROBOT` | 同步群生命周期状态 | 不执行群指令 |
| `GROUP_MSG_RECEIVE` / `GROUP_MSG_REJECT` | 同步 QQ 主动消息授权 | 不改写管理员设置的本地开关 |

接收侧 DTO 需要无损保留官方新增字段，当前 `GroupAtMessageCreateDto` 已接收 `attachments`、`mentions`、`ark_data` 和 `msg_elements`。业务指令仍以 `content` 作为入口；后续图片、语音、引用消息和卡片消息业务可在此基础上扩展。

## 消息发送

| 发送方式 | 必需关联字段 | 项目约束 | 平台联调状态 |
| --- | --- | --- | --- |
| 消息被动回复 | `msg_id`，可选 `msg_seq` | `msg_seq` 限 1 至 5，可选择引用来源消息 | 离线结构测试完成，正式环境待验收 |
| 事件被动回复 | `event_id` | 禁止同时设置 `msg_id`、`msg_seq` 和消息引用 | 离线结构测试完成，正式环境待验收 |
| 群主动消息 | 不携带上述关联字段 | 必须同时开启本地开关并收到平台授权；默认每群 60 秒一条，数据库跨实例频控 | `群公告` 已接入，正式环境待验收 |
| 互动召回消息 | `is_wakeup=true` | 禁止同时设置 `msg_id` 或 `event_id`，必须走统一发送器 | 离线结构测试完成，正式环境待验收 |

项目的 60 秒限制是自身保护策略，不代表 QQ 平台的全部频率规则。平台返回限流时统一映射为类型化 QQ OpenAPI 错误，业务代码不得自行无限重试。

`GET /v2/groups/{group_openid}/bot_state` 属于平台侧群内机器人状态查询能力，官方文档标注为内邀开放。项目提供 `bot.qq.verify-platform-state-before-active-send` 开关；默认关闭，不影响现有发送链路。开启后主动消息发送前会读取 `allow_proactive_msg`，平台不允许或接口失败时回滚本地频控占位并拒绝主动发送。

OpenAPI 默认域名按 v2 文档使用 `https://api.bot.qq.com`；`TX_BOT_OPENAPI_URL` 和 `TX_BOT_ACCESS_TOKEN_URL` 仍保留覆盖能力，便于平台灰度或兼容旧环境。
bot.qq.message-ingress-mode 控制消息入口，默认 WS：应用通过 /gateway/bot 获取 QQ v2 WebSocket 地址并发送 Identify、Heartbeat、Resume；op=0 dispatch 复用现有 BotMessageService。配置为 HOOK 或 WEBHOOK 时，应用关闭 QQ WS，改用 /bot/message webhook，并继续执行 Ed25519 原始 body 验签。


## 群管理 OpenAPI

| 官方能力 | 本地封装 | 权限边界 |
| --- | --- | --- |
| 获取群基础信息 | `GET /v2/groups/{group_openid}/info` | 只作为只读能力，不直接授予业务操作权限 |
| 获取机器人群内状态 | `GET /v2/groups/{group_openid}/bot_state` | 可选用于主动消息发送前校验平台授权 |
| 入群申请列表拉取 | `GET /v2/groups/{group_openid}/join_request_list` | 官方要求机器人具备群管理员身份；业务指令接入时必须再做本地群管理权限判断 |
| 入群申请审批 | `POST /v2/groups/{group_openid}/approval_join_request/{member_openid}` | 审批、拒绝和拉黑属于高风险操作，业务指令默认不得开放给普通群成员 |
| 查询群禁言状态 | `GET /v2/groups/{group_openid}/restrict_chat_setting` | 只读查询仍应按业务需要限制展示范围 |
| 设置群成员禁言 | `POST /v2/groups/{group_openid}/restrict_chat_setting` | 修改成员禁言必须限制为群主、群管理员或明确授权的本地管理流程 |
| 入群自动审批策略 | `/v2/groups/join_approval_strategy` 及策略详情、执行、白名单接口 | 当前只提供底层客户端；创建、修改、删除、执行和白名单变更接业务前必须补权限、审计和人工确认策略 |

上述群管理接口已经按 QQ v2 官方导航补齐类型化 DTO 和客户端方法，但尚未默认暴露为聊天指令。接入业务指令时应继续复用 `GroupCommandExecutionService` 的权限、冷却、审计和 `invocationId` 日志链路，避免绕过现有保护。
## 返回类型

| `BotResponse` 类型 | `msg_type` | 当前状态 |
| --- | ---: | --- |
| `TEXT` | 0 | 群聊已接入 |
| `MARKDOWN` | 2 | 菜单与 Keyboard 已形成业务闭环，其他模板按平台准入扩展 |
| `ARK` | 3 | 类型与发送校验已具备，正式平台能力待验收 |
| `EMBED` | 4 | 群聊发送器明确拒绝，仅保留未来其他场景模型 |
| `IMAGE` / `IMAGE_URL` / `AUDIO_URL` / `MEDIA` | 7 | 图片链路已接入；语音默认关闭并等待正式凭证联调 |

群富媒体支持两条上传链路：`IMAGE_URL` 与 `AUDIO_URL` 表示业务已经提供 QQ 可拉取的 HTTPS URL，继续使用 `POST /v2/groups/{group_openid}/files` 的 URL 上传；模板图片 `IMAGE` 默认使用 QQ v2 本地分片上传，按 `/upload_prepare -> presigned_url PUT -> /upload_part_finish -> /files(upload_id)` 换取 `file_info`，再由发送消息接口引用。配置 `bot.qq.media.image-upload-mode=MINIO` 时，模板图片会回退到旧的 MinIO 公网 URL 上传链路。当前本地分片流程为串行上传，尚未实现并发上传、断点续传或按官方 `upload_config` 自动重试。

真实群发送验收使用 `QqGroupMessageLiveSmokeIT`，必须同时提供 `QQ_GROUP_LIVE_SMOKE=true`、确认短语 `SEND_TO_TEST_GROUP`、测试群 openid 和单一模式。runner 每次最多发送一条最终群消息，不由普通测试或 CI 执行；`ACTIVE` 模式只验证 OpenAPI 主动消息传输，不替代应用数据库授权与跨实例频控验收。

## 指令权限和发布阶段

- `PUBLIC`：群成员均可调用。
- `GROUP_ADMIN`：只允许 payload 中 `author.member_role` 为 `owner` 或 `admin`；角色缺失时拒绝。
- `PRODUCTION`：正式能力，在正式运行模式开放。
- `EXPERIMENTAL`：只在 `TEST` 模式且目标群开启实验功能时开放。
- `REVIEW`：只在 `REVIEW` 模式开放；feature 分支不保存审核写死返回。
- 群管理员可以关闭非系统单项指令；系统指令不接受单项关闭。

`绑定区服` 属于 `GROUP_ADMIN` 本地数据库指令，按 `group_openid` 对整群生效。角色与常用角色按当前群消息的 `member_openid` 保存，区服和角色名必填、门派可选。QQ 官方为同一用户在不同群分配不同 `member_openid`，因此个人绑定不会跨群或 C2C 自动共享。`author.member_role=owner` 只表示群主，不表示机器人应用拥有者。

`群公告 内容` 属于 `GROUP_ADMIN` 正式指令。Action 只返回 `DeliveryMode.ACTIVE` 和公告文本，统一执行模板负责调用主动消息发送器；本地开关未开启、平台未授权或频控未结束时，模板改用来源消息进行被动提示，不把拒绝结果作为 webhook 异常抛出。


## 私聊主号配置管理

`C2C_MESSAGE_CREATE` 已作为独立私聊管理入口接入，不复用群指令执行链。只有 `bot.qq.admin.master-openids` 中配置的 QQ 用户 OpenID 可以执行管理命令；未配置主号时所有配置管理命令都会被拒绝。当前支持：

- `配置帮助`
- `配置列表`
- `配置查看 key`
- `配置设置 key value`
- `配置删除 key`
- `脚本字段列表`
- `脚本字段设置 Mongo字段 显示名 分组 类型 READ|WRITE 排序`
- `脚本字段删除 Mongo字段`
- `配置日志` / `审核日志`

配置列表只返回配置 key，不回显 value；配置查看 key 只对主号开放，且 key 包含 token、secret、password、credential、authorization、accesskey、privatekey、appkey 或 ticket 时固定返回 ******，避免通过 QQ 私聊泄露敏感配置。

运行时配置保存在 `bot_runtime_config`，仅由显式读取该表的业务能力生效，不热改环境变量或 `application.yml`。配置与审核类操作统一写入 `bot_admin_audit_log`；审计日志记录类别、动作、目标 key、操作者 OpenID、来源和成功/失败，不记录配置值、原始私聊内容或凭证。
## 冷却与审计

- 群消息的同群同指令共享数据库冷却，不同群或不同指令互不影响。
- 本地指令默认 5 秒，外部调用默认 30 秒；可按 `REGEX` 枚举名单独覆盖。
- 权限、发布策略和参数校验失败不消耗冷却。
- 调用审计不保存消息原文、请求参数或上游响应，只保存固定指令名、状态、耗时和脱敏失败摘要。


## 官方 v2 API 客户端覆盖

底层客户端已覆盖当前 QQ v2 autogen 服务端 API：

- 机器人与网关：`/users/@me`、`/users/@me/guilds`、`/gateway`、`/gateway/bot`、`/v2/generate_url_link`。
- C2C：发送单聊消息、流式发送、撤回、URL 富媒体上传、分片预上传、分片完成。
- 群聊：发送群消息、撤回、URL 富媒体上传、分片预上传、分片完成、群基础信息、机器人群内状态、入群申请、审批、禁言、入群自动审批策略。
- Interaction：`PUT /interactions/{interaction_id}`。
- 频道/子频道：频道详情、机器人加入频道列表、子频道列表、创建、详情、修改和删除。

上述能力默认只代表“代码可以按官方路径调用”。审批、禁言、频道修改、自动审批策略、主动消息等高风险操作不得绕过权限、审计、冷却和人工验收直接开放给普通聊天指令。
## 发布检查

1. 确认 QQ 控制台已为目标环境开通所需事件和消息能力。
2. 使用 `PRODUCTION`、`TEST` 或 `REVIEW` 明确选择发布模式。
3. 在正式环境分别验收消息回复、事件回复、媒体上传及需要的平台模板能力。
4. 观察类型化错误、`invocationId` 日志和 Micrometer 指标，不通过日志打印 token、openid 或消息正文。
5. 平台规则变化时同步更新本文件、DTO、发送校验和离线 fixture。
## 群主动推送订阅

- 主动推送除了群级“主动消息”总开关和 QQ 平台授权，还必须命中当前群已开启的具体推送任务；缺少任一条件都不发送。QQ WS 群状态事件只能回推事件所属群，JX3API WS 事件只能投递到开启对应任务的群。
- `开启推送 任务名`、`关闭推送 任务名`、`推送列表` 仅允许群主或群管理员使用，变更写入 `bot_admin_audit_log`，分类为 `GROUP_PUSH`。列表表格必须同时展示已开启与未开启项目，并明确区分 QQ WS、JX3API WS 和定时任务来源。
- WS 实时事件必须先完成持久化指纹去重，再进入异步群投递；相同事件不会因重连、重复帧或多实例重复发送。
- 自定义定时任务与 WS 事件共用订阅和发送策略，但不使用 WS 帧指纹去重；定时任务自身应提供稳定调度与业务幂等键。