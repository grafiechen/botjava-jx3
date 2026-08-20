# botjava-jx3 变更历史

### 2026-08-12 群指令权限接口预留

调整内容：

- 新增 `GroupCommandPermissionConfiguration`，作为后续数据库权限配置的扩展点，用于声明哪些群指令属于权限接口，以及指定群内哪些成员可操作。
- 新增默认空实现 `NoopGroupCommandPermissionConfiguration`，暂不接管任何指令，保证当前线上行为仍由 `REGEX.CommandAccess` 的公开/群管理规则控制。
- `GroupCommandExecutionService` 在静态 `CommandAccess` 之前查询权限配置；被动态权限接管的指令按 `groupOpenId + memberOpenId + REGEX` 授权结果放行或拒绝，未接管指令保持原逻辑。
- 本次只预留服务接口和执行链路，不新增权限表、不新增配置指令，也不默认配置任何权限接口。

验证范围：

- `mvn "-Dtest=GroupCommandExecutionServiceTest,ProjectDocumentationContractTest" test` 通过，共 22 项测试。

### 2026-08-12 JX3API 模块配置化开关

调整内容：

- 移除启动类上的 `@EnableJX3ApiHttp` 依赖，JX3API 能力改为通过配置文件控制。
- 新增 `jx3api.enabled` 总开关，默认 `true`；关闭后不加载 JX3API HTTP 指令 Action、请求工具和 JX3API WS 推送配置。
- 新增 `jx3api.http.enabled`，默认 `true`，用于单独关闭 JX3API HTTP 查询指令链路。
- 新增 `jx3api.ws.enabled`，默认 `false`，用于显式开启 JX3API 游戏事件 WebSocket 推送；该配置与 QQ v2 WebSocket 消息入口相互独立。
- 当用户触发已知 JX3 指令但 JX3API HTTP 指令链路未启用时，群聊返回 `该功能未开启。`。

验证范围：

- `mvn "-Dtest=BotjavaApplicationTests,ConfigurationValidationTest,GroupCommandExecutionServiceTest,ProjectDocumentationContractTest" test` 通过，共 36 项测试。
### 2026-08-12 QQ token 使用前刷新边界

调整内容：

- `QqOpenApiClient` 明确保存 QQ token 的实际过期时间；HTTP OpenAPI 请求和 QQ v2 WebSocket `Identify`/`Resume` 每次取 `Authorization` 前都会判断剩余有效期，剩余 `60` 秒及以内会重新获取 token。
- QQ WS 继续复用统一 QQ OpenAPI 鉴权客户端，不单独维护第二份 token 状态，避免 HTTP 与 WS 刷新窗口不一致。

验证范围：

- `mvn "-Dtest=QqOpenApiClientTest,QqWebSocketGatewayServiceTest" test` 通过，共 9 项测试。
### 2026-08-11 QQ v2 WebSocket 默认消息入口

### 2026-08-11 QQ v2 全量客户端与主号私聊配置

调整内容：

- 对照 QQ v2 autogen 服务端 API 列表补齐底层客户端：C2C 消息、流式消息、单聊富媒体、URL Link、Interaction ACK、频道/子频道基础管理，并继续保留群管理和群消息客户端。
- `TxMessageInfo` 补充 C2C `input_notify`，支持官方输入中状态消息结构。
- 新增 `C2C_MESSAGE_CREATE` 私聊入口，只有 `bot.qq.admin.master-openids` 配置的 QQ 主号可以执行运行时配置管理命令。
- 私聊配置列表只展示 key；显式查看配置时对 token、secret、password、credential、authorization、accesskey、privatekey、appkey、ticket 等敏感 key 自动打码，配置操作和越权尝试继续写入 `bot_admin_audit_log`。
- 新增 `bot_runtime_config` 和 `bot_admin_audit_log`，配置值入库，配置/审核类操作审计入库；审计日志不保存配置值或原始私聊内容。
- QQ 分片上传外部 PUT 日志保留 method/host/partIndex/耗时/状态等排障字段，但不再明文输出临时 `uploadId`，错误响应体继续走敏感信息脱敏。
- 新增 `docs/database/bot-runtime-config.sql`，部署文档补充主号 OpenID 配置和 SQL 执行说明。

验证范围：

- `mvn -DskipTests compile` 通过。
- `mvn "-Dtest=QqUserMessageClientTest,QqGuildClientTest,QqUtilityClientTest,QqGroupManagementClientTest,C2cAdminConfigServiceTest,QqOpenApiClientTest" test` 通过，共 20 项测试。
### 2026-08-11 QQ v2 群管理 OpenAPI 对齐

调整内容：

- 对照 QQ v2 官方左侧“群聊管理”导航，补齐入群申请列表拉取、入群申请审批、查询群禁言状态、设置群成员禁言和入群自动审批策略相关底层客户端方法。
- `QqOpenApiClient` 新增 `PATCH` 传输方法，用于官方“修改入群自动审批策略”接口。
- 新增群管理 DTO，覆盖 `join_request_id`、`verify_info`、禁言全局规则、成员禁言状态、自动审批策略、策略关联群、白名单和策略执行结果等官方字段。
- 当前变更只实现可复用 OpenAPI 客户端能力，不默认开放聊天指令；后续接业务时必须继续走现有权限、冷却、审计和日志脱敏链路。
- `QQ_CAPABILITY_POLICY.md` 增加群管理接口边界，明确审批、禁言、自动审批策略等高风险操作的权限要求。

验证范围：

- `mvn -DskipTests compile` 通过。
- `mvn "-Dtest=QqGroupManagementClientTest,QqGroupMessageClientTest,QqOpenApiClientTest,ProjectDocumentationContractTest,QqCapabilityPolicyDocumentTest" test` 通过，共 28 项测试。
调整内容：

- 新增 `bot.qq.message-ingress-mode`，默认 `WS` 使用 QQ v2 WebSocket 获取消息；配置为 `HOOK` 或 `WEBHOOK` 时关闭 WS，启用 `/bot/message` webhook。
- 新增 `bot.qq.websocket.*` 配置，覆盖 intents、shard、`/gateway/bot` 选择、重连延迟和握手检查时间；默认 intents 为 `33554432`。
- 新增 `QqGatewayClient` 和 `QqGatewayDto`，封装 `/gateway` 与 `/gateway/bot`。
- 新增 `QqWebSocketGatewayService`，处理 Hello、Identify、Heartbeat、Resume、Reconnect、Invalid Session 和 Dispatch 转发；业务事件继续复用 `BotMessageService`。
- `BotMessageController` 在 WS 模式下不处理 webhook 请求，避免同一机器人同时通过 WS 和 webhook 重复消费事件。
- 架构、部署、QQ 能力边界和文档契约同步记录 WS 默认入口与 webhook 回退模式。

验证范围：

- `mvn "-Dtest=ConfigurationValidationTest,BotMessageControllerTest,QqOpenApiClientTest,QqGatewayClientTest,QqWebSocketGatewayServiceTest,ProjectDocumentationContractTest" test` 通过，共 34 项测试。
- `tools/verify-utf8.ps1` 通过，共覆盖 595 个文本文件；`git diff --check` 通过，仅保留 Windows 工作区 CRLF 提示。
### 2026-08-11 QQ v2 本地分片媒体上传

调整内容：

- `QqGroupMessageClient` 补齐本地文件分片上传闭环：计算文件 `md5`、`sha1`、`md5_10m`，调用 `/upload_prepare`，按返回的预签名 URL 串行 PUT 分片，逐片调用 `/upload_part_finish`，最后通过 `/files` 提交 `upload_id` 获取 `file_info`。
- `GroupMessageSender` 的模板图片发送支持 `bot.qq.media.image-upload-mode`，默认 `CHUNK` 走 QQ v2 分片上传；显式配置 `MINIO` 时继续兼容旧的 MinIO 公网 URL 上传链路。
- `application.yml` 增加 `BOT_QQ_IMAGE_UPLOAD_MODE` 环境变量映射，默认值为 `CHUNK`。
- 架构、部署和 QQ 能力文档补充默认分片上传、MinIO 兼容模式、URL 上传适用范围以及当前暂未实现并发/断点重试的边界。

验证范围：

- `mvn "-Dtest=QqGroupMessageClientTest,GroupMessageSenderTest,ProjectDocumentationContractTest" test` 通过，共 33 项测试。
- `tools/verify-utf8.ps1` 通过，共覆盖 588 个文本文件；`git diff --check` 通过，仅保留 Windows 工作区 CRLF 提示。
### 2026-07-28 QQ OpenAPI v2 群能力更新

调整内容：

- QQ OpenAPI 默认域名同步到 v2 文档的 `https://api.bot.qq.com`，同时保留 `TX_BOT_OPENAPI_URL` 和 `TX_BOT_ACCESS_TOKEN_URL` 覆盖能力。
- `QqGroupMessageClient` 新增群基础信息、机器人群内状态、群消息撤回、富媒体分片预上传和分片完成入口；群消息发送结果改为 `QqGroupMessageSendResultDto`，保留官方返回的消息 `id`、`timestamp` 和 `ext_info.ref_idx`。
- 群消息发送模型补充 `is_wakeup`；`GroupMessageSender` 统一校验互动召回消息不能与 `msg_id` 或 `event_id` 混用。
- 接收侧群消息 DTO 补充 `attachments`、`mentions`、`ark_data` 和 `msg_elements`，后续可扩展图片、语音、引用消息和卡片消息业务。
- 新增 `bot.qq.verify-platform-state-before-active-send` 开关。默认关闭；开启后主动消息前查询 `GET /v2/groups/{group_openid}/bot_state`，仅当 `allow_proactive_msg=true` 时继续发送。
- `TxFileUploadResultDto` 补充 `raw_url`，兼容官方富媒体分片上传完成后的返回结构。
- `QQ_CAPABILITY_POLICY.md` 同步记录新官文能力边界、默认域名、内邀状态查询限制和分片上传未完整封装事项。

验证范围：

- `mvn "-Dtest=QqGroupMessageClientTest,GroupMessageSenderTest,QqMessageDtoTest,ConfigurationValidationTest,QqOpenApiClientTest,QqGroupLiveSmokePlanTest,ProjectDocumentationContractTest" test` 通过，共 57 项测试。

### 2026-07-23 QQ webhook 事件推送验签

调整内容：

- `BotMessageController` 改为接收原始 `byte[]` 请求体，并读取 `X-Signature-Ed25519`、`X-Signature-Timestamp`。
- 除 `op=13` 回调地址验证外，所有 QQ 事件推送在进入 `BotMessageService` 前必须使用 `X-Signature-Timestamp + 原始 body` 完成 Ed25519 验签；失败返回 `401`，不再进入 Action、数据库或 QQ OpenAPI 链路。
- `CallbackValidAction` 改为复用统一 `QqCallbackSignatureUtil`，`op=13` 继续按官方要求对 `event_ts + plain_token` 签名。
- 验签逻辑补齐签名 hex 解码、64 字节长度和 Ed25519 签名最高位约束，降低畸形签名进入验证器的概率。
- `tools/replay-payload.ps1` 支持 `-Timestamp` 与 `-Signature` 参数，用于本地或预发布环境透传 QQ 签名头；无签名 `op=0` 回放会被入口拒绝。
- 部署指南和架构文档补充反向代理必须原样转发 body 和两个 QQ 签名头，明确本地回放脚本的签名要求。

验证范围：

- `mvn "-Dtest=QqCallbackSignatureUtilTest,BotMessageControllerTest,BotMessageServiceTest" test` 通过，共 7 项测试。

### 2026-07-16 Docker 镜像与 Java 21 运行基线

调整内容：

- 项目编译版本统一为 Java 21，GitHub Actions 现有 CI 同步使用 Temurin Java 21。
- 新增 Dockerfile，使用 Ubuntu/Jammy 系 Java 21 多阶段镜像构建 Spring Boot 可执行 JAR，并在运行镜像中预装 Playwright Chromium、系统依赖和中文字体，支持 HTML/Vue 截图。
- 新增 `.dockerignore`，排除 `.git`、`target`、本地日志、IDE 文件和文档等不参与镜像构建的内容。
- HTML/Vue 截图工具启动 Chromium 时增加 `--no-sandbox` 和 `--disable-dev-shm-usage`，适配默认 Docker 运行环境。
- 部署指南和架构文档统一把运行基线改为 Java 21，并补充 Docker 构建与运行命令。

验证范围：

- `mvn -DskipTests package` 已在 Java 21 下通过；`docker build -t jx3bot:local .` 已成功生成镜像 `sha256:cf6c1ed122fc1258e3dfe63c2a5ca43ddd925b60d78bcf886452c9dd9cebd71a`；容器内 `java -version` 确认为 Temurin 21.0.11。`ProjectDocumentationContractTest` 8 项通过；严格 UTF-8 检查通过，共覆盖 558 个文本文件。

### 2026-07-16 本地运行产物忽略规则

调整内容：

- `DpsComputeAction` 移除服务地址、请求路径和模型名的代码内硬编码，改为读取 `jx3api.api.dps-service-url`、`dps-service-path` 和 `dps-model`；默认值保持现有第三方 DPS 服务与“旗舰”模型。
- `application.yml` 暴露 `JX3API_DPS_SERVICE_URL`、`JX3API_DPS_SERVICE_PATH` 和 `JX3API_DPS_MODEL`，DPS token 继续使用 `JX3API_DPS_TOKEN`。
- 架构文档将 DPS 实验指令配置纳入关键配置清单，并把过时的“未来集中注册”描述改为当前约束。
- 源码契约测试防止主代码重新引入 `SpringContextUtil` 静态取 Bean、旧 `BotRequestUtl` 上传链路和审核写死标记，并约束 QQ 群消息传输字段只允许在 `GroupMessageSender` 内拼装。
- 外部验收预检查脚本的 `production` 范围补齐 JX3API ticket、默认服务器和数据库账号密码检查，并在部署发布检查中增加生产变量预检查命令。
- `ApiProperties` 启动校验补齐 `default-server`、`ticket` 和机器人名称，和部署指南中声明的 JX3API 必需配置保持一致，避免缺省区服或 ticket 在业务 Action 执行时才失败。
- `AuthorDto` 明确封装 QQ 群成员角色判断，使用 payload 中 `author.member_role` 的 `owner`/`admin` 识别群主和管理员；`绑定区服` 继续通过 `GROUP_ADMIN` 权限在执行模板前置拦截，普通成员与缺失角色默认拒绝。
- `.gitignore` 增加 `logs/`，避免本地测试和运行生成的日志文件进入仓库。
- `.gitignore` 增加 `.codex/`，避免本地 Codex 沙箱配置误提交。
- 新增 `docs/testing/external-acceptance-record-template.md`，用于真实 JX3API、QQ、MinIO、阿里语音、PostgreSQL、可观测性和 GitHub Actions 验收后的脱敏记录。
- 部署发布检查和架构外部验收门槛补充该模板路径，明确验收结果应追加到变更历史。
- 项目文档契约测试增加外部验收模板断链保护，确保架构文档和部署发布检查持续引用模板，并保留敏感信息禁写提醒。
- 架构文档明确 WebSocket 订阅配置表属于未来 WS 专项，不纳入当前 HTTP 群指令、QQ 消息发送、模板截图和持久化完成口径；文档契约测试同步保护该边界。
- 新增 `tools/check-external-acceptance-env.ps1`，可按 JX3API、QQ、MinIO、PostgreSQL、阿里语音或生产部署范围做外部验收环境变量预检查；脚本只报告检查是否通过，不输出变量值。
- 新增 `tools/invoke-external-acceptance.ps1`，在通过预检查后统一调用已有 live smoke 测试类：JX3API、QQ OpenAPI、QQ 群消息、MinIO 公网媒体域名、PostgreSQL 双实例和阿里语音生成；`production` 当前只执行预检查并明确说明不调用外部服务。
- QQ 群消息 live smoke 增加 `AUDIO` 模式，使用 `QQ_GROUP_LIVE_AUDIO_URL` 走 `BotResponse.audioUrl` 和 QQ `file_type=3` 上传发送链路，用于阿里语音生成 MP3 后的平台验收。
- 新增 `SoundConverterLiveSmokeIT` 和 `SoundConverterLiveSmokePlan`，显式开启后使用真实 JX3API token 与阿里语音凭证生成 HTTPS 音频 URL；测试输出只保留音频 host，不打印完整 URL 或凭证。
- 新增 Prometheus scrape 契约测试，验证 `bot.command.duration`、`bot.jx3.request.duration`、`bot.jx3.cache.requests` 和 `bot.qq.request.duration` 会暴露为 Prometheus 文本，并继续禁止 `group_openid`、`member_openid`、消息 ID、角色名和消息内容进入指标标签。
- 新增 `ObservabilityLiveSmokeIT` 和 `ObservabilityLiveSmokePlan`，显式开启后读取目标环境 `OBSERVABILITY_PROMETHEUS_URL`，验证四类 `bot_*` 指标已暴露且响应文本不包含敏感/高基数字段；输出只保留 host 和 HTTP 状态。
- 外部验收预检查脚本与统一 runner 增加 `observability` scope，`-DryRun` 会展示 `ObservabilityLiveSmokeIT`，实际执行前要求 `OBSERVABILITY_LIVE_SMOKE=true` 和目标 Prometheus URL。
- 部署发布检查补充外部验收前置预检查命令，减少正式凭证环境下的试错。
- 项目文档契约测试增加外部验收预检查脚本断链和脱敏输出保护，确保部署文档持续引用脚本，脚本继续声明不打印变量值。
- 新增 `tools/new-external-acceptance-record.ps1`，用于生成外部验收记录草稿，自动填充日期、分支和 commit；脚本不读取环境变量值，输出只包含脱敏占位内容，方便真实验收后追加到本变更历史。

验证范围：

- 本地完整 Maven 回归测试共 451 个通过，其中 15 项为 Playwright 模板渲染测试；沙箱内执行会因 Node/Playwright 访问用户目录受限而失败，已通过沙箱外同命令确认通过。
- 严格 UTF-8 检查共覆盖 551 个文本文件。
- 外部验收记录生成脚本已完成输出文件与控制台两种模式验证，项目文档契约测试同步保护脚本引用和脱敏边界。
- 外部验收统一 runner 已完成 `-DryRun` 覆盖验证，确认各 scope 会映射到对应 live smoke 测试类且不打印环境变量值。
- QQ 群消息 live smoke plan 聚焦测试覆盖 `AUDIO` 模式的来源消息 ID、HTTPS 音频 URL 校验和 plan 字段映射。
- 阿里语音 live smoke plan 聚焦测试覆盖强确认、JX3API token、语音凭证、音频参数边界和 200 字文本上限。
- Prometheus scrape 聚焦测试覆盖四类 `bot_*` 指标名称、固定标签和值域边界，以及敏感/高基数字段不得进入指标文本。
- 可观测性 live smoke plan 聚焦测试覆盖强确认、HTTP/HTTPS URL 解析和非法 scheme 拒绝；runner `observability -DryRun` 已验证不会访问目标端点。
- DPS 配置化聚焦测试额外覆盖配置默认值、非法配置拒绝、DPS 请求模型/token/路径拼装和实验指令冷却/发布策略，共 22 个通过。
- 群角色权限聚焦测试覆盖 `member_role=owner/admin/member/null`、绑定区服权限拦截和注册表权限元数据，共 31 个通过。

### 2026-07-16 PostgreSQL 双实例验收与事务边界

调整内容：

- 为群指令冷却的条件占位/完成更新和 JX3API 缓存覆盖更新补齐 repository 事务边界，避免生产服务在没有测试外层事务时执行 `@Modifying` 失败。
- 数据层测试关闭 `DataJpaTest` 默认外层事务，直接验证 repository 自身事务契约。
- 新增强确认的 `PostgresTwoInstanceLiveSmokeIT`，只接受空的 `botjava_smoke_*` 专用 schema，并强制 JDBC `currentSchema` 与声明值一致。
- runner 使用两个独立 Spring/JPA 上下文验证群绑定、个人绑定、冷却并发单一放行及过期租约接管、主动消息频控并发单一放行、共享缓存和调用审计。
- 修复空库检查失败后仍可能清理既有 smoke 数据的问题；只有空库检查成功后才开放结束清理，并增加未授权清理不得访问 Spring 上下文的安全测试。
- 部署指南补充专用 schema、最小权限、环境变量、执行命令和清理边界；普通 Maven 与 CI 不会访问真实 PostgreSQL。
- HTML 图片渲染对 Playwright 明确的 page/browser/connection closed 瞬断最多重试一次，模板脚本和资源错误仍立即失败；解决批量模板回归中偶发的 Chromium 提前退出。

验证范围：

- repository 事务与 live plan 聚焦测试共 4 个通过；无环境变量时在线 runner 编译成功并安全跳过。
- 完整 Maven 测试共 433 个通过，其中 14 项为 Playwright 模板渲染测试，另有 1 项浏览器瞬断分类测试；严格 UTF-8 检查共覆盖 545 个文本文件。
- 通过 GitHub 公共 Actions API 检查远端仓库，2026-07-16 的 `total_count=0`，首次远端 CI 运行仍待代码推送后执行。
- 目标 PostgreSQL 凭证当前不可用，真实双实例结果仍按外部验收门槛记录为待执行。

### 2026-07-16 README 索引职责与 UTF-8 质量门槛统一

调整内容：

- README 收回为项目简介、文档索引和基础 AI 编程约束，不再重复维护技术栈、配置项、启动命令及外部参考链接；详细内容继续以设计文档和部署指南为准。
- 新增 `tools/verify-utf8.ps1`，在 Windows 本地使用严格 UTF-8 解码统一检查 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。
- GitHub Actions 的 UTF-8 范围补齐 SQL、XML 和 Properties，并增加相对 push/PR 基线的 `git diff --check` 空白错误校验；历史空白债务不阻塞当前分支，但新变更不得继续引入问题。

验证范围：

- 项目文档契约聚焦测试共 11 个通过；完整 Maven 测试共 433 个通过，其中 14 项为 Playwright 模板渲染测试。
- 本地严格 UTF-8 脚本执行成功，共检查 545 个文本文件；`git diff --check` 无空白错误。
- 审计剩余实验指令：`语音`已有配置隔离与完整发送链路，`DPS`已有实际外部调用；`水墨圈圈`和`吃瓜`缺少语义明确的数据源或专用模板，继续保持非生产实验占位，并在设计文档未实现清单中明确记录，未使用相近接口伪装完成。
- 设计文档增加外部环境验收门槛，分别定义 JX3API 在线 smoke test、QQ webhook/发送、自有媒体域名、阿里语音、PostgreSQL 双实例、可观测性和 GitHub Actions 所需证据；离线测试不再被表述为正式平台验收。
- 新增项目文档契约测试，动态校验 README 只承担入口职责，并确保设计文档中的官方 contract 数、注册指令数、图片指令数及四类完成度口径与当前代码一致。
- 新增显式启用的 `Jx3ApiLiveSmokeIT`，根据覆盖矩阵构造 60 个非语音官方接口命令并复用真实 Action/DTO 链路；普通测试和 CI 不联网，阿里语音继续独立验收。
- JX3API INFO 日志不再打印请求参数，仅保留固定接口路径；在线 smoke 输出同样不包含命令、角色、QQ 号、请求参数或响应正文。
- 群生命周期、指令执行、消息模板和 QQ 客户端日志移除群 openid 与消息 id，仅保留 invocation id、固定指令/事件名和结果类型；新增源码契约禁止日志模板重新引入群、成员或消息标识。
- 新增显式启用且无发送副作用的 `QqOpenApiLiveSmokeIT`，通过真实 token 获取和官方 `/users/@me` 类型化资源验证 QQ 鉴权；输出不包含机器人 ID、用户名或凭证，真实群发送仍需指定测试群人工验收。
- 新增 `QqGroupMessageLiveSmokeIT`，通过测试群 openid、强确认短语和单一模式分别验收文本回复、引用、事件回复、主动消息、图片、Markdown + Keyboard 或 Ark；每次最多发送一条最终群消息，默认测试和 CI 不运行。
- MinIO 连接 endpoint 与公网媒体域名拆分为 `MINIO_ENDPOINT` 和 `MINIO_PUBLIC_URL`；修复对象扩展名前缺少点号的问题，上传使用不可覆盖的时间戳与 UUID 对象名。
- 新增 `MinioMediaDomainLiveSmokeIT`，显式生成 Vue PNG、上传唯一测试对象，再经公网 HTTPS 匿名回读并校验状态、MIME、大小和 PNG 签名；输出可直接供 QQ `IMAGE` 模式继续验收。

### 2026-07-16 八卦文本字段收敛与 Action 注册清理

调整内容：

- `TiebaRandomData` 改用当前官方 `tags` 字段，并通过 `@JsonAlias("class")` 兼容旧响应。
- `TiebaRandomAction` 继续按官方请求固定 `limit=1` 并保持纯文本返回；返回内容明确裁剪为分类、区服、角色、标题和日期，不输出内部 ID 与外部帖子 URL。
- 删除未被任何 `REGEX` 引用、且与 `ActiveCurrentAction` 重复的 `ActiveCalendarAction`；活动日历继续由唯一注册处理器 `ActiveCurrentAction` 接管。
- Action 源码契约增加反向校验：每个具体 `@Jx3Action` 都必须至少被一条 `REGEX` 引用，防止新增无法由指令到达的 Spring Bean。

验证范围：

- 八卦帖子当前字段、旧字段兼容、请求参数、精简文案和内部字段隔离测试通过；注册表正反向源码契约通过。
- 聚焦 Maven 测试共 89 个通过；完整 Maven 测试共 416 个通过，其中 14 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 526 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-16 科举、区服与扶摇 Vue 图片返回

调整内容：

- `ExamAnswerAction`、`ServerMasterAction` 和 `ActiveNextEventAction` 从通用精简文本切换为独立图片返回，分别使用 `科举答题.html`、`搜索区服.html` 和 `扶摇预测.html`。
- 搜索区服 DTO 修正为当前官方 `center/alias/slave/event/voice` 字段，并通过 `@JsonAlias` 兼容旧 `column/abbreviation/subordinate`；稳定视图过滤 ID、事件状态和动态语音对象。
- 科举答题只展示问题和答案，过滤题库 ID、正确率、内部索引和拼音；扶摇预测只展示大区、服务器和北京时间，在官方未定义数字状态语义前不生成猜测性状态文案。
- 三个模板均为自包含 Vue 文本插值页面，不加载远程脚本或上游资源；图片响应清单从 55 条提高到 58 条。

验证范围：

- 当前官方 JSON、旧搜索区服字段兼容、稳定视图、字段隔离、图片响应、请求参数和注册表源码契约定向回归共 159 项通过。
- 三张模板完成独立 Playwright 渲染和目视检查，问答列表、区服标签数组和扶摇时间表均无空白、裁切、溢出或重叠。
- 完整 Maven 测试共 413 个通过，其中 14 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 526 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-16 贴吧、金币、副本解密与统战 Vue 图片返回

调整内容：

- `TiebaItemRecordsAction`、`TradeDemonAction`、`MechCalculatorAction` 和 `DuowanStatisticsAction` 切换为独立图片返回，分别使用 `贴吧物价.html`、`金币价格.html`、`副本解密.html` 和 `统战歪歪.html`。
- 贴吧物价只展示区服、物品、帖子内容、回复数、楼层和可读时间，过滤帖子编号、token 和外部 URL；金币价格只接收当前官方贴吧、万宝楼和 DD373 价格字段，历史平台字段不进入稳定视图。
- 副本解密以当前官方 `curr/next/time/cdtn` 结构为主，并兼容历史 `now_*/next_*/interval_time` 字段，两个版本统一转换为当前节点、下一节点、时间与条件视图。
- 统战歪歪按服务器组织频道名称、阵营、在线人数和容量，过滤频道 ID、阵营 ID、服务器 ID 及 Logo URL。
- 四个模板均为自包含 Vue 插值页面，不加载远程脚本或上游内容；图片响应清单从 51 条提高到 55 条。

验证范围：

- 四个 Action 的当前官方字段反序列化、历史副本解密兼容、稳定视图、内部字段隔离、图片响应和注册表源码契约定向回归共 156 项通过。
- 四张模板完成独立 Playwright 渲染和目视检查，表格、价格平台、节点流转和频道分组均无空白、裁切、溢出或重叠。
- 完整 Maven 测试共 405 个通过，其中 13 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 522 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 新闻、公告、骗子与物品搜索 Vue 图片返回

调整内容：

- 修复新闻资讯和维护公告 DTO 字段漂移，以当前官方 `catid/type` 为主，并兼容旧 `token/class` 字段。
- 新闻和公告共享 `新闻资讯.html`，由各自 Action 传入不同标题；稳定视图只保留分类、标题和可读日期，不展示内部编号、分类编号或外部详情 URL。
- 骗子查询将区服、来源和记录列表扁平化为可读视图，帖子编号及链接不进入模板；搜索物品最多展示 8 条，过滤 `wblalias` 和原始图片 URL。
- 物品图片复用受控远程图片加载器，只有校验通过并转换为 data URI 后才交给 Vue，失败时使用模板占位。
- 新增自包含 Vue 模板 `新闻资讯.html`、`骗子查询.html` 和 `搜索物品.html`；新闻资讯与维护公告共享模板，图片响应清单从 47 条提高到 51 条。

验证范围：

- 四个 Action 的当前字段反序列化、请求参数、稳定视图、敏感字段隔离、图片响应和官方 contract 聚焦回归共 160 项通过。
- 新闻资讯、维护公告、骗子查询和搜索物品均完成实际 Playwright 渲染；目视检查表格、双列记录、长说明和内嵌物品图均无空白、裁切、溢出或重叠。
- 完整 Maven 测试共 396 个通过，其中 12 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 517 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 角色名片受控远程图片与 Vue 返回

调整内容：

- 新增 `RemoteImageProperties` 和 `RemoteImageDataUriLoader`，远程图片必须使用 HTTPS、命中精确域名白名单，并拒绝重定向、内网解析地址、超限响应、非图片 MIME、文件签名不符和像素超限内容。
- 下载成功的 PNG/JPEG/GIF 编码为 data URI 后才允许进入 Vue；下载失败只显示占位，不把原始 URL 交给浏览器，也不影响角色名片元数据。
- `Jx3RequestUtil` 暴露统一远程图片解析入口，保留原四参数构造器供离线 contract 测试使用，生产 Spring 构造器注入安全加载器。
- 角色名片、所有名片、随机名片和缓存名片统一输出 `RoleCardView`，过滤 `showHash/global`、原始图片 URL 和随机接口状态码；单次最多展示 12 张，相同 URL 只下载一次。
- 新增自包含 Vue 模板 `角色名片.html`，支持单卡和双列多卡布局；图片展示覆盖从 43 条提高到 47 条。
- 修正 `OfficialIndependentTextActionTest` 的旧纯文本假设，按每个 Action 明确声明的 `TEXT` 或 `IMAGE` 类型验证返回契约。

验证范围：

- 远程图片配置与安全策略、四条名片指令、字段隔离、参数和图片响应定向回归共 152 项通过。
- `角色名片.html` 使用实际内嵌 PNG 完成 Playwright 渲染；目视检查双卡比例、长角色名、启用状态和缓存时间均无溢出或重叠。
- 完整 Maven 测试首次发现并修正 2 项旧纯文本测试假设，修正后共 387 个测试通过，其中 11 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 511 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 活动、行侠与鲜花查询独立 Vue 图片返回

调整内容：

- `ActiveCurrentAction`、`ActiveListCalendarAction`、`ActiveCelebritiesAction` 和 `HomeFlowerAction` 切换为独立图片返回，并分别输出稳定模板视图。
- 活动日历不再直接读取固定的 `team[0..2]`，所有列表统一过滤空值；活动月历只展示可读日期和活动内容，不向模板传递冗余年月日字段。
- 行侠事件 DTO 增加当前官方示例 `map/stage` 到 `mapName/event` 的兼容别名，内部 `icon` 编号不进入模板。
- 家园鲜花把动态服务器 JSON key 转换为稳定分组数组，鲜花的 `line` 数组以线路标签逐项展示。
- 新增自包含 Vue 模板 `活动日历.html`、`活动月历.html`、`行侠事件.html` 和 `家园鲜花.html`，图片展示覆盖从 39 条提高到 43 条。
- 明确角色名片图片化的前置条件：先建立第三方图片受控下载、类型与大小校验、超时和 data URI 内嵌边界，不让正式 Vue 模板直接依赖上游 URL。

验证范围：

- 四个 Action 的参数、稳定视图、字段别名、动态 key 转换、官方 HTTP contract 和图片响应定向回归共 124 项通过。
- 四张模板独立 Playwright 渲染通过；目视检查日历分组、月历双列、行侠长说明和鲜花线路数组均无空白、溢出或重叠。
- 完整 Maven 测试共 374 个通过，其中 10 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 504 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 技改、小药与家园查询独立 Vue 图片返回

调整内容：

- `SkillReworkAction`、`SchoolFoodsAction`、`HomeFurnitureAction` 和 `HomeTravelAction` 分别声明图片类型、模板名称及稳定模板数据，不再把官方 DTO 直接交给通用文本处理。
- 技改记录只展示标题与时间，剔除内部 `id` 和外部详情 URL；小药推荐展示门派、心法、品质、分类、名称和增益，剔除内部 `id`。
- 家园装饰和器物图谱只展示名称、来源、限制、品质及各类可读属性，内部分类码、颜色码、建筑码和第三方图片 URL 不进入 Vue 数据。
- 新增自包含 Vue 模板 `技改记录.html`、`小药推荐.html`、`家园装饰.html` 和 `器物图谱.html`，上游内容均使用普通文本插值。
- 图片展示覆盖从 35 条提高到 39 条；四个 Action 继续复用统一 token、请求缓存、错误映射、图片生成与 QQ 发送链路。

验证范围：

- 四个 Action 的请求参数、稳定视图、内部字段隔离、官方 HTTP contract、会员/免费指令和图片响应定向回归共 138 项通过。
- 四张模板独立 Playwright 渲染通过，实际生成 1280×720 PNG；目视检查长标题、七列表格和家园 11 个展示字段均无空白、溢出或重叠。
- 完整 Maven 测试共 365 个通过，其中 9 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 499 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 师徒与阵营查询独立 Vue 图片返回

调整内容：

- 补齐师徒系统官方根字段 `type/time`，并保持招募项角色、等级、阵营、帮会、体型、门派和说明的类型化接收。
- 修复阵营事件字段漂移：以当前官方 `fenxian_name/friend_name/seize_time` 为主字段，并兼容旧服务器名和时间字段。
- `MemberTeacherAction`、`ServerSandAction`、`ServerEventAction` 和 `ServerAntiviceAction` 分别声明图片类型、模板名称和稳定模板数据，不再使用通用精简文本处理。
- 师徒视图排除 `roleId/bodyId/forceId` 和未确认展示语义的查询类型；沙盘视图排除 `reset` 及帮会、领地、首领、阵营 ID；两类事件视图均排除记录 `id`。
- 新增自包含 Vue 模板 `师徒系统.html`、`阵营沙盘.html`、`阵营事件.html` 和 `诛恶事件.html`，所有上游内容使用普通文本插值。
- 图片展示覆盖从 31 条提高到 35 条；空结果继续由 `Jx3BaseAction` 统一处理。

验证范围：

- 当前官方师徒与阵营事件字段、北京时间转换、四个稳定视图、内部字段隔离及图片响应测试通过。
- 四张模板独立 Playwright 渲染通过，实际生成 1280×720 PNG；目视检查长角色名、帮会、招募说明、六列表格、区服和时间均无空白、溢出或重叠。
- 完整 Maven 测试共 358 个通过，其中 8 项为 Playwright 模板测试。
- 严格 UTF-8 解码检查通过，共检查 494 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 未做奇遇与近期奇遇独立 Vue 图片返回

调整内容：

- 补充未做奇遇官方 `type` 字段；历史 `last` 结构继续兼容反序列化，但不进入图片视图。
- `LuckUnfinishedAction` 输出角色、区服及 `{name, type, level}` 列表，切换为独立的 `未做奇遇.html` 图片返回。
- 补充近期事件官方 `id/source` DTO 字段；`LuckRecentAction` 只输出大区、服务器、角色、奇遇和北京时间。
- 近期奇遇内部 `id/source/status` 不进入模板，在官方状态码语义未确认前不生成猜测性中文状态。
- 新增自包含 Vue 模板 `未做奇遇.html` 与 `近期奇遇.html`，两个 Action 继续分别接管 `{server, name}` 和 `{server}` 请求。
- 图片展示覆盖从 29 条提高到 31 条；空结果继续由 `Jx3BaseAction` 统一处理。

验证范围：

- 当前和历史 DTO 结构、北京时间转换、请求参数、两个稳定视图及内部字段隔离测试通过。
- 图片响应、官方 HTTP contract、会员指令和 Action 源码契约定向回归共 124 项通过。
- 两张模板独立 Playwright 测试和整批模板回归均通过，实际生成 1280×720 的 `未做奇遇.png` 与 `近期奇遇.png`；目视检查类型、等级、角色、区服和时间无空白、溢出或重叠。
- 完整 Maven 测试共 351 个通过。
- 严格 UTF-8 解码检查通过，共检查 489 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 心法阵眼与技能详情独立 Vue 图片返回

调整内容：

- 修复 `SchoolMatrixData` 的官方字段漂移：效果列表以当前 `data` 为主，并通过 `@JsonAlias("descs")` 兼容旧响应。
- `SchoolMatrixAction` 输出 `{name, skillName, effects}` 稳定视图并切换为 `心法阵眼.html` 图片返回。
- `SchoolSkillsAction` 独立解析官方套路分组列表，输出套路名称及技能说明、调息、释放、距离、消耗和武器等可读字段。
- 新增自包含 Vue 模板 `心法阵眼.html` 与 `技能详情.html`；技能模板完整展示返回分组和技能，不加载上游第三方图标 URL。
- 两个 Action 继续独立接管 `{name, ticket}` 请求参数，并复用统一 token、缓存、错误处理和最外层 QQ 发送链路。
- 图片展示覆盖从 27 条提高到 29 条；空结果继续由 `Jx3BaseAction` 统一处理。

验证范围：

- 阵眼官方 `data` 与旧 `descs`、技能官方分组结构、两个 Action 请求参数、稳定视图及图标隔离测试通过。
- 图片响应、官方 HTTP contract、会员指令和 Action 源码契约定向回归共 122 项通过。
- 两张模板独立 Playwright 测试和整批模板回归均通过，实际生成 1280×720 的 `心法阵眼.png` 与 `技能详情.png`；目视检查长描述、效果分重、套路标题、技能元数据无空白、溢出或重叠。
- 完整 Maven 测试共 344 个通过。
- 严格 UTF-8 解码检查通过，共检查 485 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 角色百战官方字段治理与 Vue 图片返回

调整内容：

- 将 `RoleMonsterData` 的精力、耐力和技能总数改为明确的数字类型，以官方 snake_case 字段为主，并继续兼容旧 camelCase 字段。
- 清理 `MonsterSkill` 的旧客户端式属性名，官方技能名称、首领、消耗、颜色、等级和弃用状态成为主模型；旧字段通过别名或兼容字段接收。
- `RoleMonsterAction` 从精简文本切换为图片返回，继续保持 `{server, name}` 请求参数以及统一 token、缓存和错误处理链路。
- Action 输出稳定的角色概览和技能列表视图；角色 `role_id/global_id`、技能输入输出 ID 和旧类型字段不进入模板数据。
- 新增自包含 Vue 模板 `角色百战.html`，完整展示本次返回的技能，不截断列表，并标记已弃用技能。
- 图片展示覆盖从 26 条提高到 27 条；空结果继续由 `Jx3BaseAction` 统一处理。

验证范围：

- 官方字段和旧客户端字段反序列化、北京时间转换、请求参数、稳定视图和内部 ID 隔离测试通过。
- 图片响应、官方 HTTP contract、会员指令和 Action 源码契约定向回归共 118 项通过。
- 角色百战模板独立 Playwright 测试和整批模板回归均通过，实际生成 1280×720 的 `角色百战.png`；目视检查角色区服、精耐、总数、技能来源、等级、消耗和弃用状态无空白、溢出或重叠。
- 完整 Maven 测试共 337 个通过。
- 严格 UTF-8 解码检查通过，共检查 481 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 百战首领官方嵌套结构修复与 Vue 图片返回

调整内容：

- 修复 `ActiveMonsterData` 与官方样例不一致的问题：补充 `week/boss`，以官方 `list` 作为首领列表字段，并用 `@JsonAlias("data")` 兼容旧响应。
- 补充首领 `index` 映射；`ActiveMonsterAction` 从占位处理切换为独立的 `{server}` 请求和图片返回，继续复用统一 token、缓存和错误处理链路。
- Action 将嵌套 DTO 裁剪为 `{server, week, boss, start, end, data: [{index, name, skills, extraName, effects, description}]}`，附加机制内部 `id` 和时间字段不进入模板。
- 新增自包含 Vue 模板 `百战首领.html`，展示周次、本周目标、北京时间范围、首领技能和附加机制。
- 图片展示覆盖从 25 条提高到 26 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 官方 `list` 与旧 `data` 两种根列表字段、首领序号、技能、附加机制和请求参数测试通过。
- 图片响应、官方 HTTP contract、注册表与 Action 源码契约定向回归共 117 项通过。
- 百战模板独立 Playwright 测试和整批模板回归均通过，实际生成 1280×720 的 `百战首领.png`；目视检查标题、周次、时间、技能标签和机制说明无空白、溢出或重叠。
- 完整 Maven 测试共 333 个通过。
- 严格 UTF-8 解码检查通过，共检查 479 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 阵营拍卖官方字段兼容与 Vue 图片返回

调整内容：

- 修复 `AcutionRecordsData` 与官方样例不一致的问题：新增 `map_name/item_name/item_amount` 映射，并用 `@JsonAlias` 继续兼容旧 `name/amount`。
- `AuctionRecordsAction` 从精简文本切换为图片返回，继续保持 `{server, 可选 name, limit=20}` 请求参数和统一 JX3API 调用链路。
- Action 将官方列表裁剪为 `{zone, server, mapName, roleName, campName, itemName, itemAmount, time}`，内部 `id` 不进入模板数据。
- 新增自包含 Vue 模板 `阵营拍卖.html`，展示物品、副本地图、角色、阵营、区服、北京时间和数量。
- 图片展示覆盖从 24 条提高到 25 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 当前官方字段和旧字段别名反序列化测试均通过；官方 61 个 HTTP contract 完整回归通过。
- 请求参数、视图裁剪、图片响应、官方会员指令和 Action 源码契约测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的 `阵营拍卖.png`；目视检查四条记录、长副本名、角色阵营、时间和数量无空白、溢出或重叠。
- 完整 Maven 测试共 329 个通过。
- 严格 UTF-8 解码检查通过，共检查 477 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 的卢记录 Vue 图片返回与 JX3 北京时间

调整内容：

- `HorseRecordsAction` 从精简文本切换为图片返回，继续保持 `{server}` 请求参数以及现有 token、缓存和统一错误处理链路。
- Action 将官方列表裁剪为可读的地点、刷新、捕获、拍卖、金额和时间区间视图，不向模板传递 `id`、历史兼容的 `name` 或 `level`。
- 新增自包含 Vue 模板 `的卢记录.html`，展示区服地点、刷新时间、捕获角色与阵营、拍卖角色与阵营及成交金额。
- `TimeUtils.timeFormatting` 从服务器系统时区改为固定 `Asia/Shanghai`，避免应用部署在日本或 UTC 环境时 JX3 秒级时间戳偏移。
- 图片展示覆盖从 23 条提高到 24 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 固定官方样例时间戳 `1733490900` 在测试中明确得到北京时间 `2024-12-06 21:15:00`。
- 请求参数、视图裁剪、图片响应、官方会员指令和 Action 源码契约测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的 `的卢记录.png`；目视检查三条记录、长时间区间、角色阵营和成交金额无空白、溢出或重叠。
- 完整 Maven 测试共 326 个通过。
- 严格 UTF-8 解码检查通过，共检查 475 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 马场刷新动态分组 Vue 图片返回

调整内容：

- `HorseEventAction` 从精简文本切换为图片返回，继续保持 `{server}` 请求参数以及现有 token、缓存和统一错误处理链路。
- 将官方 `Map<地图, List<预测文本>>` 动态键结构在 Java 中转换为稳定的 `{mapName, predictions}[]` 图片视图列表。
- 模板数据固定为 `{zone, server, note, data}`，Vue 页面只读取可读区服、地图、预测文本和说明，不依赖动态 JSON 属性名。
- 新增自包含模板 `马场刷新.html`，支持同一地图多条预测和换行说明，不依赖外部图片、脚本或 URL。
- 图片展示覆盖从 22 条提高到 23 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 请求参数、动态分组转换、图片响应、注册表和 Action 源码契约测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的 `马场刷新.png`；目视检查四个地图、多预测标签和两段说明无空白、溢出或重叠。
- 完整 Maven 测试共 323 个通过。
- 严格 UTF-8 解码检查通过，共检查 472 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 本日与本周赤兔共享 Vue 图片模板

调整内容：

- `ChituRecordsAction` 与 `ChituWeekRecordsAction` 从精简文本切换为图片返回；两个 Action 继续独立接管官方无参数请求和各自的数据转换。
- 两类 Action 共同使用 `赤兔记录.html`，通过模板数据中的 `mode=本日/本周` 明确查询语义，最外层响应保持 `IMAGE + 赤兔记录`。
- 官方 DTO 在 Java 中裁剪为 `{server, mapName, horse, date}` 图片视图列表，只保留服务器、地图、马匹和可读日期。
- 上游 `id` 和含义尚未确认的 `send` 不进入模板数据，页面也不依赖外部图片、脚本或 URL。
- 图片展示覆盖从 20 条提高到 22 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 两个 Action 的无参数请求、独立视图转换、图片响应模式、模板名称和注册表契约测试通过。
- Playwright/Vue 同时生成 1280×720 的 `本日赤兔.png` 与 `本周赤兔.png`；目视检查标题、区服、地图、马匹和日期无空白、溢出或重叠。
- 完整 Maven 测试共 322 个通过。
- 严格 UTF-8 解码检查通过，共检查 470 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过，仅有 Windows 行尾转换提示。

### 2026-07-15 关隘首领 Vue 图片返回

调整内容：

- `MineCartAction` 从精简文本切换为图片返回，继续保持官方 `/data/mine/cart` 无业务参数请求及现有 token、缓存和错误处理链路。
- 将官方 `List<{server, data[]}>` 两层结构在 Java 中扁平化为 `{zone, server, leader, campName, castle, statusText}` 图片视图列表。
- 模板数据固定为 `{server: "全服", data}`；接口 `id` 和数字 `status` 不进入模板，只保留官方可读状态文本。
- 新增自包含 Vue 模板 `关隘首领.html`，展示关隘、首领、阵营、区服和状态，不依赖外部图片、脚本或 URL。
- 图片展示覆盖从 19 条提高到 20 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 官方参数契约、嵌套数据扁平化、图片响应和 Action 源码契约测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的关隘首领 PNG；目视检查四条记录、长首领名和状态标签无空白、溢出或重叠。
- 完整 Maven 测试共 320 个通过。
- 严格 UTF-8 解码检查通过，共检查 468 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 官方查询独立 Action 与空返回治理

调整内容：

- 将随机名片、缓存名片、阵营事件、诛恶事件、关隘首领、今日本周赤兔、马场刷新、贴吧物价、搜索物品、副本解密、统战歪歪、八卦帖子和舔狗日记共 14 条查询切换为独立 Action。
- 每个具体 Action 分别接管请求参数构造和返回处理，继续复用 `Jx3BaseAction` 的 token、缓存、错误映射与最外层发送模板。
- 删除已无指令引用的 `OfficialExtendedAction` 共享分发类，避免后续在单一 `switch` 中继续堆叠业务分支。
- `REGEX` 只负责声明指令元数据和具体处理器，不再让已经存在的业务子类保持 `return null` 占位。
- 修复旧 `ActiveCalendarAction` 的数字参数判断和空返回；删除 17 个没有注册、没有源码引用且只保留空实现的历史占位 Action。
- 新增 Action 源码契约测试：既逐项反查 `REGEX.getBaseAction()`，也扫描全部具体 `Jx3BaseAction` 源文件，要求显式实现 `dealAfterJx3ApiRequest` 并禁止该方法返回 `null`。
- 当前 71 个已注册 Action 和全部 72 个具体 `Jx3BaseAction` 源文件均通过契约检查，不存在处理器缺失或空返回实现。

验证范围：

- 14 个独立 Action 的参数、返回处理和 `REGEX` 处理器归属测试通过；61 个官方 HTTP DTO 契约和统一注册表回归通过。
- 完整 Maven 测试共 319 个通过。
- 严格 UTF-8 解码检查通过，共检查 466 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 帮战记录独立 Action 与可读时间视图

调整内容：

- 将 `BattleRecords` 从共享 `OfficialExtendedAction` 拆分到独立 `BattleRecordsAction`，保持 `{server}` 请求参数和现有 JX3API token、缓存及错误处理链路。
- 子类将官方帮战 DTO 转换为 `{zoneName, serverName, declaringTongName, acceptingTongName, startTime, endTime, duration}` 图片视图模型。
- 开始、结束时间统一在 Java 侧转换为可读时间，秒数时长转换为小时、分钟和秒，模板不承担时间语义计算。
- 新增自包含 Vue 模板 `帮战记录.html`，展示对战帮会、区服、起止时间和持续时长，不展示内部 ID、外部图片或来源 URL。
- 图片展示覆盖从 18 条提高到 19 条；空结果继续由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 独立 Action 参数、时间视图、正则注册、共享 Action 回归及图片响应契约测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的帮战记录 PNG；目视检查四条记录无空白、溢出或重叠。
- 完整 Maven 测试共 317 个通过。
- 严格 UTF-8 解码检查通过，共检查 476 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 黑市物价独立 Action 与通用价格模板

调整内容：

- 将 `TradeRecords` 从共享 `OfficialExtendedAction` 拆分到独立 `TradeRecordsAction`，继续保持 `{server, name}` 请求参数并返回图片类型。
- `TradeRecordsAction` 与既有 `TradeRecordAction` 复用 `物品价格.html`，通过 `{mode, server, name, data}` 区分“黑市物价”和“物品价格”。
- 重构价格模板为自包含 Vue 页面，同时兼容官方黑市 DTO 的 `list/sale` 与既有物品价格 DTO 的 `data/sales`。
- 模板保留分类、子类、等级、官方售价、描述、趋势图和近期记录；不展示 `id/token/source/status`，也不加载接口提供的外部商品图片。
- 上游描述改为普通文本插值，不再使用 `v-html`；趋势图最多展示最近 8 条有效记录，纵轴与价格统一按整数格式化。
- 补齐此前遗漏的 `TradeRecordAction` 图片响应契约测试；图片展示覆盖从 17 条提高到 18 条。

验证范围：

- 黑市物价请求参数、独立 Action、正则注册和两类价格图片响应契约测试通过。
- Playwright/Vue 同时渲染 `物品价格.png` 与 `黑市物价.png`；目视检查两套字段、折线图、标签及近期记录表格无空白、溢出或重叠。
- 完整 Maven 测试共 316 个通过。
- 严格 UTF-8 解码检查通过，共检查 473 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 角色奇遇 Vue 图片返回

调整内容：

- `LuckAdventureAction` 从精简文本切换为标准图片模板返回，继续使用官方 `/data/event/records` 类型化列表 DTO 和 `{server, name}` 请求参数。
- 重构既有 `角色奇遇.html`，移除 Bootstrap、背景图、日历图标和奇遇图片等静态资源依赖，并删除已经过期的 `world_serendipity/pet_serendipity` 字段绑定。
- 模板数据契约为 `{server, name, data}`，与 `LuckAdventureData` 的 `zone/server/name/event/time` 可读字段直接对应。
- 未对官方样例中含义不够明确的 `level/status` 擅自添加业务文案，也不展示任何内部标识或外部图片 URL。
- 图片展示覆盖从 16 条提高到 17 条；空结果仍由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 请求参数、图片响应与模板数据契约测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的角色奇遇 PNG；目视检查角色、区服、奇遇名称和时间列无空白、溢出或重叠。
- 完整 Maven 测试共 314 个通过。
- 严格 UTF-8 解码检查通过，共检查 471 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 试炼排行独立 Action 与 Vue 图片返回

调整内容：

- 将 `RankTrials` 从共享 `OfficialExtendedAction` 拆分到独立 `RankTrialsAction`，由具体子类声明图片响应、模板名称和模板数据。
- JX3API 请求继续使用 `{server, name}`；`试炼 花间游` 使用默认区服，`试炼 乾坤一掷 花间游` 使用显式区服。
- 重构既有 `试炼排行.html` 为自包含 Vue 模板，移除 Bootstrap、背景图、定位图标和门派图片等静态资源依赖。
- 模板数据契约为 `{server, name, time, data}`，展示角色、最高层数、装备分数、试炼总分和可读更新时间，不展示接口 `id`。
- 图片展示覆盖从 15 条提高到 16 条；空结果仍由 `Jx3BaseAction` 统一返回精简文本提示。

验证范围：

- 独立 Action 参数测试覆盖默认区服和显式区服；正则注册、图片响应及共享 Action 回归测试通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的试炼排行 PNG；目视检查大数字、表格列和筛选条件无空白、溢出或重叠。
- 完整 Maven 测试共 312 个通过。
- 严格 UTF-8 解码检查通过，共检查 470 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 资历排行独立 Action 与 Vue 图片返回

调整内容：

- 将 `SchoolSeniority` 从共享 `OfficialExtendedAction` 拆分到独立 `SchoolSeniorityAction`，由具体子类明确声明图片响应、模板名称和模板数据。
- JX3API 请求参数继续保持 `{server, school, ticket}`，没有改变现有 token、ticket、HTTP 调用和错误处理链路。
- 新增 `资历排行.html` 自包含 Vue 模板，模板数据契约为 `{server, school, data}`，展示名次、角色、门派、区服和格式化资历。
- 模板只读取 `zoneName/serverName/roleName/forceName/seniority` 等可读字段，不展示角色 ID、门派 ID、图标或头像 URL。
- 图片展示覆盖从 14 条提高到 15 条；空列表仍由 `Jx3BaseAction` 统一降级为精简文本提示。

验证范围：

- 独立 Action、请求参数、正则注册及图片响应聚焦测试共 41 个通过。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的资历排行 PNG；目视检查表格、筛选条件和数字格式无空白、溢出或重叠。
- 完整 Maven 测试共 310 个通过。
- 严格 UTF-8 解码检查通过，共检查 468 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 掉落统计 Vue 图片返回

调整内容：

- `ValuablesStatisticalAction` 从通用精简文本切换为图片模板返回，继续使用官方 `/data/reward/statistics` 类型化列表 DTO。
- 修正 Action 参数语义：物品名改由 `CommandArguments.value()` 获取，不再借用角色名兼容方法；HTTP 请求仍保持 `{server, name, limit=20}`。
- 新增 `掉落统计.html` 自包含 Vue 模板，以双列卡片展示物品、获得角色、地图和可读获得时间。
- 模板数据契约为 `{server, name, data}`，不展示接口 ID、标签等内部字段，不依赖外部图片或脚本。
- 图片展示覆盖从 13 条提高到 14 条。

验证范围：

- 图片 Action 契约测试共 13 组通过；聚焦测试共 33 个通过，并锁定请求参数结构未变化。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的掉落统计 PNG；目视检查双列卡片无空白、溢出或重叠。
- 完整 Maven 测试共 309 个通过。
- 严格 UTF-8 解码检查通过，共检查 466 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 本服榜单 Vue 图片返回

调整内容：

- `RankStatisticalAction` 从通用精简文本切换为图片模板返回，继续使用官方 `/data/rank/statistics` 类型化根 DTO 和榜单条目 DTO。
- 新增 `本服榜单.html` 自包含 Vue 模板，展示榜单类别、区服、名次、帮会、领地、帮主、当前/上限人数和总分。
- 模板数据契约为 `{server, name, time, data}`；Action 将秒级接口时间转换为本地可读时间，榜单类别独立于角色参数解析。
- 模板不展示接口 `id`，不依赖外部图片或脚本，并对缺失字段和空榜单提供明确占位。
- 图片展示覆盖从 12 条提高到 13 条。

验证范围：

- 图片 Action 契约测试共 12 组通过，明确校验榜单区服、类别和可读时间。
- Playwright/Vue 主模板测试通过，并实际生成 1280×720 的本服榜单 PNG；目视检查表格无重复信息、空白、溢出或重叠。
- 完整 Maven 测试共 308 个通过。
- 严格 UTF-8 解码检查通过，共检查 465 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 名剑统计 Vue 图片返回

调整内容：

- `MatchSchoolsAction` 从通用精简文本切换为图片模板返回，继续使用官方 `/data/arena/schools` 类型化列表 DTO。
- 新增 `名剑统计.html` 自包含 Vue 模板，以双柱图对比各门派的上期值 `last` 与本期值 `this`，并显示增减差值。
- 模板数据契约为 `{mode, maxValue, data}`；Action 从同批 DTO 计算绝对最大值，模板据此归一化柱长，不假定接口数值是百分比还是计数。
- 支持 `22/33/55` 模式中文展示、正负差值配色、持平状态和空列表提示，不依赖外部图片或脚本。
- 图片展示覆盖从 11 条提高到 12 条。

验证范围：

- 图片 Action 契约测试共 11 组通过，明确校验 `mode` 与 `maxValue`。
- Playwright/Vue 主模板测试通过，并实际生成 1280×800 的名剑统计 PNG；目视检查柱长、文字和两列布局无空白、溢出或重叠。
- 完整 Maven 测试共 307 个通过。
- 严格 UTF-8 解码检查通过，共检查 464 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 名剑排行 Vue 图片返回

调整内容：

- `MatchAwesomeAction` 从通用精简文本切换为图片模板返回，继续使用官方 `/data/arena/awesome` 类型化列表 DTO。
- 新增 `名剑排行.html` 自包含 Vue 模板，展示比赛模式、名次、角色、门派、区服、评分、胜率和名次上升值。
- 模板数据契约为 `{mode, data}`；`mode` 将 `22/33/55` 显示为 `2 对 2/3 对 3/5 对 5`，列表为空时显示暂无排行数据。
- 模板不读取头像 URL，不展示内部标识；字符串 `0` 的名次变化按持平显示。
- 图片展示覆盖从 10 条提高到 11 条。

验证范围：

- 图片 Action 契约测试共 10 组通过，包含新增名剑排行返回。
- Playwright/Vue 模板测试 3 个通过，并实际生成 1280×720 的名剑排行 PNG；目视检查无空白、溢出或重叠。
- 完整 Maven 测试共 306 个通过。
- 严格 UTF-8 解码检查通过，共检查 463 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 角色信息 Vue 图片返回

调整内容：

- `RoleDetailedAction` 从精简文本切换为标准图片模板返回，继续使用官方 `/data/role/detail` 类型化 DTO。
- 新增 `角色信息.html` 自包含 Vue 模板，展示角色、区服、门派、体型、阵营、帮会和身份等中文可读字段。
- 模板不展示 roleId、globalId、forceId 等标识，不读取头像 URL，不依赖外部网络资源。
- `ImageActionResponseTest` 增加角色信息模板数据契约，Playwright 主模板测试增加实际 PNG 渲染。
- 修正架构文档中过期的图片 Action 示例；模板名不再写 `static/*.html`，返回类型使用 `BotResponse`，Vue 由截图工具自动挂载。
- 图片展示覆盖从 9 条提高到 10 条。

验证范围：

- 图片 Action 契约测试共 9 组通过，包含新增角色信息返回。
- Playwright/Vue 模板测试 3 个通过，并实际生成 1280×720 的角色信息 PNG；目视检查无空白、溢出或重叠。
- 完整 Maven 测试共 303 个通过。

### 2026-07-15 群公告主动消息业务闭环

调整内容：

- `BotResponse` 增加 `DeliveryMode`，默认 `REPLY`，业务子类可显式选择 `ACTIVE`；发送方式继续由最外层模板解释，Action 不直接调用 QQ 客户端。
- 新增群管理员正式指令 `群公告 内容`，公告正文限制 1 至 500 个字符，不调用外部 JX3API。
- `GroupCommandExecutionService` 根据返回标识选择被动回复或群主动消息，并统一完成冷却与调用审计。
- `GroupMessageSender.sendActive` 返回类型化发送结果；本地开关未开、平台未授权或处于频控时不再直接抛出异常，执行模板改用来源消息提示管理员。
- 主动发送真正失败时仍回滚数据库 UUID 占位并抛出异常，避免失败请求占用群级 60 秒窗口。
- 架构文档、QQ 能力政策和部署指南同步记录首个主动消息业务触发及正式环境待验收状态。

验证范围：

- 聚焦测试共 34 个通过，覆盖公告 Action、注册与权限、主动分发、拒绝反馈、频控反馈、失败回滚和政策文档。
- 完整 Maven 测试共 302 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 461 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-15 QQ 能力政策与 MDC 日志关联

调整内容：

- 新增 `docs/QQ_CAPABILITY_POLICY.md`，集中说明 QQ 消息入口、被动/事件/主动发送方式、返回类型、群管理权限、项目冷却和发布审核阶段。
- 明确区分项目自身 5/30/60 秒保护策略与 QQ 平台动态限制，未经过正式环境验证的能力继续标记为待验收。
- `logging.pattern.level` 输出 SLF4J MDC 中的 `invocationId`，现有同步执行链日志可直接被后续集中式日志平台关联。
- README、部署指南和架构文档增加政策文档入口，并更新 MDC 与 QQ 说明文档的实施状态。
- 增加回归测试，防止日志 pattern 或政策文档中的关键权限、频率及发布边界被误删。

验证范围：

- 完整 Maven 测试共 297 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 完整测试日志已实际输出 `[invocationId:invocation-1]`，验证 MDC 字段进入 Spring Boot 日志 pattern。
- 严格 UTF-8 解码检查通过，共检查 459 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件；`git diff --check` 通过。

### 2026-07-13 群区服与个人角色绑定权限确认

调整内容：

- `绑定区服` 是整群配置，只按当前消息的 `group_openid` 直接写入 `group_info`，不调用 JX3API，也不写入个人账号表。
- 群消息通过 `author.member_role` 判断权限；仅 `owner` 和 `admin` 可以绑定群区服，普通成员或缺失角色信息一律拒绝。
- `绑定角色`、`添加角色`、`切换角色`、`我的角色`、`解绑角色`、`绑定门派` 和 `解绑门派` 是个人配置，只按发送者 `member_openid` 写入或查询 `user_info`、`user_role_binding`，同一账号跨群共用。
- 上述绑定操作均为本地数据库指令，默认使用 5 秒群指令冷却；需要调用 JX3API 的查询指令仍默认 30 秒。

验证范围：

- 回归测试锁定群主与管理员放行、普通成员和角色缺失拒绝，以及群区服和个人角色绑定均不被误判为外部调用。
- 聚焦测试共 21 个通过；全量业务与数据层测试 292 个通过，3 个 Playwright/Vue 截图测试单独复跑通过。
- 严格 UTF-8 解码检查通过，共检查 457 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 QQ 身份资源与可选健康检查

调整内容：

- 依据腾讯官方 `botgo` 的 `UserAPI.Me` 和资源常量实现 `GET /users/@me`，新增类型化 `QqBotUserDto` 与 `QqBotIdentityClient`。
- 身份响应必须具备 id、username 且 `bot=true`，缺失字段使用现有 QQ `INVALID_RESPONSE` 类型化错误处理。
- 新增 `QqOpenApiHealthIndicator`，由 `TX_BOT_HEALTH_CHECK_ENABLED=true` 显式开启；默认不注册，不增加外部探测流量。
- 健康状态只包含认证布尔值或错误分类与 HTTP 状态，不记录机器人标识、用户名、token、异常消息或上游响应。
- QQ token 与 OpenAPI 请求增加统一可配置超时，`TX_BOT_REQUEST_TIMEOUT_SECONDS` 允许 1 至 60 秒、默认 10 秒；非 QQ 的历史 `RequestUtil.doPost` 保留独立 30 秒默认值。
- 官方依据：`https://github.com/tencent-connect/botgo/blob/master/openapi/v1/me.go`、`openapi/v1/resource.go` 和 `dto/user.go`。

验证范围：

- 聚焦测试覆盖身份路径与 DTO、必需字段校验、QQ 指标、UP/DOWN 脱敏结果、默认关闭与显式启用条件、超时边界及 DPS 历史调用兼容。
- 本次完整测试范围共 295 个；业务与数据层 292 个通过，Playwright/Vue 截图 3 个单独复跑通过。
- 严格 UTF-8 解码检查通过，共检查 457 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 QQ OpenAPI 通用传输边界

调整内容：

- 新增 `QqOpenApiClient`，统一 QQ access token 缓存、提前刷新、401 失效后单次重试、类型化错误映射和 GET/POST/PUT/DELETE 请求。
- `QqGroupMessageClient` 只负责 `/v2/groups/{group_openid}/messages` 与 `/files` 资源路径、媒体请求体和业务指标，不再反向引用工具类 URL 常量。
- 删除旧 `BotRequestUtl`，QQ OpenAPI 只保留一个鉴权与 HTTP 传输入口。
- 图片、语音上传都通过通用客户端发送，群资源客户端显式设置 `file_type`、URL 和 `srv_send_msg=false`。
- `RequestUtil` 增加通用 HTTP 方法、查询参数与可选 JSON 请求体支持；GET/DELETE 不发送请求体，POST/PUT 明确要求请求体。
- JX3API 指令覆盖测试增加反向断言，确保 61 个官方 HTTP contract 全部存在群指令，且每个官方群指令都具备 contract；修正架构文档中过期的“仍有未注册接口”表述。

验证范围：

- 聚焦测试覆盖 token 刷新、401 重试、限流不重试、四种 HTTP 方法路由、查询参数、请求体、请求前校验、群消息、图片与语音上传、发送模板、Spring 构造注入和 JX3API 双向覆盖。
- 完整 Maven 测试共 288 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 452 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 群生命周期与主动消息双重授权

调整内容：

- 新增 `GROUP_ADD_ROBOT`、`GROUP_DEL_ROBOT`、`GROUP_MSG_RECEIVE`、`GROUP_MSG_REJECT` 的类型化 payload、Action 路由和可插拔 handler 注册表。
- 根据 QQ 官方事件结构解析 `timestamp`、`group_openid` 和 `op_member_openid`；数字和字符串时间戳均兼容为字符串读取。
- 将群管理员设置的 `active_messages_enabled` 与 QQ 平台状态 `active_messages_platform_allowed` 分开保存，平台事件不再覆盖本地开关。
- 主动消息数据库原子占位同时要求本地开关和平台授权为真；拒绝、退群和重新入群均按安全默认值关闭平台授权。
- 再次确认绑定作用域：群默认区服只按 `group_openid` 写入 `group_info`，角色、常用角色和门派只按 `member_openid` 写入账号表。

验证范围：

- 27 个聚焦测试通过，覆盖四类官方事件 fixture、事件路由、授权状态同步、双重授权原子条件、群区服隔离和账号角色共享。
- 完整 Maven 测试共 286 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 452 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 Action 群配置读取边界

调整内容：

- `GroupConfigurationService` 增加按 `group_openid` 查询群默认区服的只读方法，并统一清理数据库空值和首尾空格。
- `Jx3BaseAction` 不再依赖 `GroupInfoMapper`，只通过群配置领域服务读取当前群默认区服；未绑定时继续使用 JX3API 全局默认区服。
- 79 个 JX3 Action 文件的构造依赖统一迁移为 `GroupConfigurationService`；`BindServerAction` 和 `GroupSettingsAction` 消除 Mapper 与领域服务的重复注入。
- Action 目录内已不存在 `GroupInfoMapper`，群配置读写边界均由领域服务承接。
- 指令注册表测试继续按构造器类型自动提供依赖，覆盖所有已注册处理器的启动完整性。

验证范围：

- 聚焦测试共 92 个通过，覆盖群默认区服优先、全局默认区服兜底、服务读取清洗、全部处理器注册和 61 个官方 HTTP contract。
- 完整 Maven 测试共 279 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 439 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 群配置与个人绑定数据库边界

调整内容：

- 新增 `GroupConfigurationService`，统一按 `group_openid` 写入整群默认区服、查询开关、实验开关和主动消息开关。
- `BindServerAction` 与 `GroupSettingsAction` 不再直接保存 `GroupInfo`，只负责参数与返回文本拼装。
- 绑定区服使用条件更新和唯一键竞争恢复，保证同一群只能首次绑定；不同群分别保存自己的默认区服。
- `GroupInfoMapper` 的原子更新方法增加独立短事务，服务在没有外层事务时也可执行，并允许并发插入失败后安全重试。
- 个人默认角色、常用角色和门派继续统一由 `UserCommandPreferenceService` 按 `member_openid` 保存，不写入 `group_openid`；同一 QQ 账号跨群共用个人绑定。
- 指令注册表测试按 Action 构造器参数自动提供依赖，新增领域服务不再破坏处理器完整性校验。

验证范围：

- 单元测试覆盖群区服首次绑定、重复绑定、并发首次写入恢复、群开关更新、Action 委派和个人绑定账号隔离。
- H2 数据层测试在无外层事务条件下覆盖群配置服务，并验证不同群区服隔离及同一账号角色共享。
- 完整 Maven 测试共 277 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 439 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 JX3API 缓存命中率指标

调整内容：

- `BotMetrics` 增加 `bot.jx3.cache.requests` Counter，Prometheus 输出为 `bot_jx3_cache_requests_total`。
- 仅对明确配置缓存策略的固定 JX3API 路径记录 `result=hit|miss`，非缓存接口不进入命中率分母。
- 不使用服务器、角色、群 openid、成员 openid 或请求参数作为标签，避免指标高基数和用户信息泄漏。
- `Jx3RequestUtil` 在读取共享缓存后统一记录命中或未命中，原有 cache/remote 耗时指标保持不变。
- 部署文档增加 5 分钟缓存命中率 PromQL。

验证范围：

- 聚焦测试共 9 个通过，覆盖 hit/miss Counter、固定路径标签、缓存命中调用链、敏感维度缺失以及现有缓存策略回归。
- 完整 Maven 测试共 272 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 436 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 群指令统一追踪编号

调整内容：

- 新增 `RequestTraceContext`，以群指令 `invocation_id` 建立可嵌套的 ThreadLocal 与 MDC 同步作用域。
- `GroupCommandExecutionService` 在权限、冷却、业务 Action、QQ 发送和审计全链路外建立作用域，并在成功、提前返回和异常路径自动清理。
- `Jx3BaseAction` 优先复用 `invocation_id` 作为 JX3API 请求编号，因此用户错误提示中的请求编号可以直接关联 `command_invocation` 记录。
- QQ 群消息日志增加内部 `invocationId`；腾讯响应中的官方追踪编号明确命名为 `qqTraceId`，避免两个编号混淆。
- 群指令失败、冷却和空返回日志补充 `invocationId`，异常摘要继续执行敏感信息脱敏。

验证范围：

- 聚焦测试共 16 个通过，覆盖嵌套作用域恢复、MDC 同步、正常与异常清理、执行模板内编号可见、JX3API 错误提示编号复用和 QQ 请求期间编号可见。
- 完整 Maven 测试共 272 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 436 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 JX3API 数据库共享缓存

调整内容：

- 将 `Jx3ApiResponseCache` 从单机 `ConcurrentHashMap` 迁移到 PostgreSQL 表 `jx3_api_cache`，多个应用实例共享同一短时查询结果。
- 保持原有八类公共查询和 30 秒至 5 分钟 TTL；token 不进入 SHA-256 缓存键，失败响应与实时角色接口不缓存。
- 缓存响应使用 JSON 存储，单条限制 1 MB；数据库不可用、序列化失败或缓存 JSON 损坏时按未命中处理，不阻断 JX3API 远程查询。
- 增加每日过期数据清理和生产迁移脚本 `docs/database/jx3-api-cache.sql`。
- 将架构文档中的通用模板章节由未来建议更新为当前实现状态。

验证范围：

- 聚焦测试共 77 个通过，覆盖序列化往返、token 键隔离、TTL 边界、损坏数据淘汰、1 MB 上限、数据库故障降级、非缓存接口、失败响应、定时清理、跨服务实例共享、覆盖更新和 61 个官方 HTTP contract。
- 完整 Maven 测试共 271 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 434 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 群指令统一执行模板

调整内容：

- 新增 `GroupCommandExecutionService`，集中编排指令解析、访问权限、发布与群策略、个人默认参数、冷却、业务 Action、QQ 发送和调用审计。
- `GroupAtMessageAction` 缩减为 payload 反序列化与执行服务委派，只保留一个业务依赖。
- 具体 `Jx3BaseAction` 继续只负责数据请求与 `BotResponse` 拼装，不直接调用 QQ 接口；外层模板根据返回类型交给 `GroupMessageSender` 统一发送。
- 保持原有时序：权限和参数错误不占冷却，Action 与 QQ 发送结束后开始冷却，空返回记为 `NO_RESPONSE`，发送或业务异常记为 `FAILED`。

验证范围：

- 聚焦测试共 17 个通过，覆盖 payload 委派、执行异常兜底、成功发送、管理员权限、角色缺参、冷却静默、空返回、发送失败、冷却完成和跨实例数据库状态。
- 完整 Maven 测试共 266 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 430 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 群指令冷却跨实例持久化

调整内容：

- 将 `GroupCommandCooldownService` 的 JVM 内存状态迁移到 PostgreSQL 表 `group_command_cooldown`，按群 openid 与 `REGEX` 指令名唯一隔离。
- 使用数据库条件更新抢占执行权；相同群、相同指令在多个应用实例之间只放行一个请求，不同群或不同指令互不影响。
- 每次执行使用独立 UUID，只有当前持有者能在 Action 和 QQ 发送结束后开始冷却，旧请求无法覆盖新占位。
- 增加执行中租约，应用实例异常退出时默认 300 秒后自动允许接管，可通过 `BOT_COMMAND_COOLDOWN_IN_FLIGHT_TIMEOUT_SECONDS` 调整。
- 新增生产迁移脚本 `docs/database/group-command-cooldown.sql`，部署顺序同步记录在 `docs/DEPLOYMENT.md`。

验证范围：

- 聚焦测试共 20 个通过，覆盖内部/外部默认值、逐指令覆盖、关闭冷却、群与指令隔离、并发单一放行、跨服务实例共享、完成后计时、旧 UUID 保护和租约超时恢复。
- 完整 Maven 测试共 264 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 428 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件。

### 2026-07-13 Markdown 帮助菜单与按钮回调

调整内容：

- `菜单` 返回 Markdown 与两行四按钮的自定义 Keyboard，`帮助` 和 `查询帮助` 保持纯文本兼容。
- 新增 `HelpMenuService`，菜单文本和分类内容均从 `REGEX` 及当前群功能策略动态生成。
- 新增 `HelpInteractionHandler`，只接管 `data.type=11` 且以 `jx3:help:` 开头的按钮回调。
- 回调只选择帮助分类，不把任意 `button_data` 当作群指令执行，避免绕过权限、冷却与审计链路。
- `GroupMessageSender` 增加自定义按钮字段校验；回调数据必填并限制为 128 个字符。

验证范围：

- 聚焦测试共 32 个通过，覆盖文本兼容、Markdown/Keyboard 拼装、四类按钮、回调选择、失效按钮、重复 handler、事件回复和非法回调拒绝。
- 完整 Maven 测试共 262 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- QQ 正式环境按钮展示与点击回调仍需使用已准入机器人完成联调。

### 2026-07-13 QQ 互动事件通用路由

调整内容：

- `PayloadTEnum` 增加 `INTERACTION_CREATE`，由独立 `InteractionCreateAction` 接管。
- 新增互动事件 Java Bean；稳定字段强类型映射，随互动类型变化的 `data.resolved` 使用 `JsonNode` 保持兼容。
- 新增 `QqInteractionHandler` 和注册表，具体按钮、菜单或表单由独立 handler 判断并返回 `BotResponse`。
- 群互动业务返回统一通过 `GroupMessageSender.sendEventReply` 使用互动 id 作为 `event_id` 发送。
- 未匹配互动安全忽略；同一互动匹配多个 handler 时明确拒绝，避免重复执行业务。
- 增加 UTF-8 payload 样例以及 DTO、路由、重复匹配和事件回复测试。

验证范围：

- 完整 Maven 测试共 255 个通过，其中包含 3 个 Playwright/Vue 截图测试。
- 严格 UTF-8 解码检查通过，共检查 420 个 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell 和 XML 文件。

### 2026-07-13 群区服与个人角色作用域确认

调整内容：

- `绑定区服` 继续按 `group_openid` 写入 `group_info`，一个群保存一份默认区服，对群内所有成员生效。
- `绑定角色`、`添加角色`、`切换角色`、`我的角色` 和 `解绑角色` 继续按 `member_openid` 写入或读取个人表，不保存 `group_openid`。
- 同一个 QQ 账号在不同群中共用个人角色和默认角色；不同群的默认区服互不影响。
- 增加 H2 数据层回归测试，固定上述群级与账号级数据边界。

验证范围：

- H2 数据层测试验证两个群分别读取各自区服，同时个人角色仅按同一 `member_openid` 查询。

### 2026-07-13 Micrometer 与 Prometheus 运行指标

目标：

- 将群指令、JX3API 和 QQ OpenAPI 的结果与耗时接入机器可采集指标。
- 指标不能携带群、成员、角色或消息内容等高基数及隐私数据。

调整内容：

- 新增 `BotMetrics`，统一定义 `bot.command.duration`、`bot.jx3.request.duration` 和 `bot.qq.request.duration`。
- `CommandInvocationRecorder` 在审计落库前记录指令指标，数据库故障不会造成指标丢失。
- `Jx3RequestUtil` 区分缓存与远端来源，并记录成功、API 错误、空响应和异常。
- `QqGroupMessageClient` 记录消息发送、图片上传和语音上传结果。
- 引入 Actuator 和 Prometheus registry；管理端口默认只监听 `127.0.0.1:8082`，仅暴露 health、info 和 prometheus。

验证范围：

- 聚焦测试覆盖指标标签、隐私标签缺失、指令接线、JX3 缓存命中和 QQ 成功/无效响应结果。
- 指标相关聚焦测试共 76 个通过；Maven 全量测试共 248 个通过，其中包含 3 个 Playwright HTML 转图片测试。

### 2026-07-13 群主动消息共享频控

目标：

- 主动消息频控在多应用实例部署时仍按群统一生效。
- QQ 发送失败只能回滚本次发送占位，不能清除其他实例后来取得的窗口。

调整内容：

- `GroupInfo` 新增主动消息占位时间和 UUID，占位状态持久化到 PostgreSQL。
- `GroupInfoMapper` 使用条件 `UPDATE` 原子抢占发送窗口，不在 QQ 网络请求期间持有数据库事务或行锁。
- `GroupActiveMessagePolicy` 移除进程内 `ConcurrentHashMap`，失败时按群 openid 和 UUID 条件回滚。
- `group_info.open_group_id` 增加唯一约束，保证群配置和群频控始终一群一行。
- `group-info.sql` 幂等增加共享频控字段和唯一索引；测试新增 H2 数据层验证。

验证范围：

- 数据层测试覆盖首次占位、重复占位拒绝、错误 UUID 拒绝回滚和正确 UUID 释放窗口。
- 共享频控相关聚焦测试共 15 个通过；Maven 全量测试共 246 个通过，其中包含 3 个 Playwright HTML 转图片测试。

### 2026-07-13 群主动消息授权与频控

目标：

- 主动消息必须经过群级明确授权，不能因为业务代码调用 `sendActive` 就直接发送。
- 主动消息使用独立频控，不占用用户被动指令的冷却窗口。

调整内容：

- `GroupInfo` 新增 `activeMessagesEnabled`，历史群默认未授权；`群设置 主动消息 开启/关闭` 由群主或管理员维护。
- 新增 `GroupActiveMessagePolicy`，默认按群限制 60 秒一条主动消息。
- `GroupMessageSender.sendActive` 在最外层统一申请发送许可，未授权或频控期不会调用 QQ 客户端。
- QQ 发送失败时回滚本次频控占位，允许业务修复后重试；发送成功后保留时间窗。
- 新增 `docs/database/group-info.sql`，幂等补齐查询、实验和主动消息开关列。

验证范围：

- 聚焦测试覆盖默认拒绝、显式授权、独立时间窗、倒计时、失败回滚、发送器拦截和群设置持久化，共 25 个测试通过。
- Maven 全量测试共 245 个通过，其中包含 3 个 Playwright HTML 转图片测试。

### 2026-07-13 QQ OpenAPI 错误统一处理

目标：

- 保留 QQ 官方 HTTP 状态、错误 code 和 trace id，替代丢失上下文的普通 `RuntimeException`。
- 防止官方错误响应、token 或其他敏感内容进入 stderr、应用日志和用户提示。

调整内容：

- 新增 `RemoteHttpException`，`RequestUtil` 不再向 stderr 打印完整 HTTP 响应体。
- 新增 `QqOpenApiException` 与 `QqOpenApiErrorMapper`，统一分类认证、权限、限流、参数、目标不存在、服务端、网络和协议响应异常。
- 仅提取官方响应中的 `code`、`trace_id`，忽略上游 `message`，类型化异常提供固定安全提示。
- 缓存 token 遇到 401 时失效、重新获取并重试一次；429 和 5xx 不自动重发群消息，避免重复回复。
- 图片或语音上传成功响应缺少 `file_info` 时，统一归类为 `INVALID_RESPONSE`。

验证范围：

- 聚焦测试覆盖错误分类、数字 code、trace id、无效 JSON、敏感 message 隔离、401 刷新重试、429 不重发和媒体响应校验，共 26 个测试通过；完整 `mvn test` 共 241 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 个人默认门派

目标：

- 允许每个 QQ 账号独立保存默认门派，并用于支持门派参数的查询。
- 让角色绑定和其他个人偏好拥有独立生命周期，解绑角色不能误删门派设置。

调整内容：

- `UserInfo` / `user_info` 新增 `school` 字段，生产 SQL 使用 `ADD COLUMN IF NOT EXISTS` 兼容旧表。
- 新增 `绑定门派 门派名`、`解绑门派`，并在 `我的绑定` 中同时展示默认门派和常用角色。
- `CommandArguments.withDefaults` 增加门派默认合并，显式 `school` 参数保持最高优先级。
- `UserCommandPreferenceService` 在删除最后一个或全部角色时只清空角色字段；存在门派时保留个人偏好记录。
- 当前默认门派可直接补齐 `资历` 查询的可选门派参数。

验证范围：

- 聚焦测试覆盖门派绑定/解绑、显式参数优先、默认参数合并、保留门派的角色清理、Action 文本和注册表完整性，共 44 个测试通过；完整 `mvn test` 共 236 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 调用记录保留与群统计

目标：

- 为群指令调用记录增加明确保留周期，避免审计表无限增长。
- 提供不暴露用户内容的群级调用量、成功率和耗时统计。

调整内容：

- 新增 `CommandAuditProperties`，默认保留 30 天、统计 7 天，单次统计最多 90 天。
- 新增每日定时清理服务，按 `create_time` 删除过期记录，并补充独立时间索引。
- `CommandInvocationMapper` 增加按群、时间和状态聚合的类型化投影。
- 新增群管理员指令 `群统计` / `群统计 天数`，最多展示 10 条高频指令的调用量、成功率和平均耗时。
- 聚合结果只使用现有脱敏调用记录，不包含成员标识、消息原文或请求参数。

验证范围：

- 聚焦测试覆盖东京时区清理边界、默认与最大统计窗口、成功率、展示名转换、空结果和 Action 文本，共 15 个测试通过；完整 `mvn test` 共 233 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 群单项指令开关

目标：

- 在群查询总开关之外，允许群主或管理员单独关闭某一条业务指令。
- 帮助菜单与实际执行使用同一策略，避免菜单展示与可执行状态不一致。

调整内容：

- 新增 `GroupCommandSetting`、Mapper 和服务，以 `group_open_id + REGEX 枚举名` 保存稳定覆盖值。
- 新增 `群指令` 管理入口；`群指令 开服状态 关闭` 按中文展示名设置，`群指令` 查看本群已关闭项。
- 系统指令不能通过单项设置关闭，保证帮助、群设置和个人基础能力始终可访问。
- `GroupCommandPolicy` 一次读取群配置和单项覆盖，统一过滤执行请求与帮助菜单。
- 新增生产迁移脚本 `docs/database/group-command-setting.sql`。

验证范围：

- 聚焦测试覆盖配置持久化、旧枚举忽略、总开关、单项拒绝、系统指令保护、菜单过滤和管理 Action，共 26 个测试通过；完整 `mvn test` 共 228 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 个人多角色绑定

目标：

- 群默认区服仍保存在 `group_info`，对整个群生效。
- 角色绑定按 `member_openid` 隔离，允许每个 QQ 账号保存多个常用角色并切换默认角色。

调整内容：

- 新增 `user_role_binding` 与 `UserRoleBinding`，以账号、区服和角色名组合去重。
- `user_info` 继续作为当前默认角色指针，保持现有查询默认参数兼容。
- `绑定角色` 添加角色并设为默认；新增 `添加角色`、`切换角色`、`我的角色` 和带参的 `解绑角色`。
- 删除当前默认角色后，自动选择最早保存的剩余角色；无参 `解绑角色` 仍清空当前账号的全部角色。
- 生产 SQL 幂等将旧 `user_info` 中的完整角色迁移到角色集合，不影响原默认角色。

验证范围：

- 聚焦测试覆盖新增、切换、列表、单个解绑、全部解绑、默认角色接替和旧数据兼容；完整 `mvn test` 共 222 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 官方 HTTP contract 类型化 DTO 补齐

目标：

- 消除 `MethodEnum` 中官方接口使用裸 `Map.class` 承接返回值的情况。
- 继续使用官方文档 contract JSON 作为字段和层级的验证依据。

调整内容：

- 新增 `OfficialQueryData`，补齐家园鲜花、技改、小药、扶摇、名剑战绩、榜单、资历、关隘、赤兔、试炼、黑市物价、物品搜索、帮战、副本解密和统战数据模型。
- 将 17 处 `Map.class` 映射替换为类型化 DTO；本服榜单和全服榜单共享稳定榜单模型。
- 家园鲜花的服务器名称是动态 JSON key，使用 `@JsonAnySetter` 保存为类型化鲜花列表。
- 名剑 `performance` 在有数据和无数据时可能分别为对象与空数组，使用 `JsonNode` 保留官方多形态结构，历史战绩保持类型化。
- 官方 contract 参数化测试增加硬约束，禁止官方 `MethodEnum` 再使用 `Map.class`。

验证范围：

- 61 个官方 HTTP 反序列化 contract 与 4 个 check 文档元测试，共 71 个测试通过。
- 完整 `mvn test` 共 217 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 指令正式、测试与审核环境隔离

目标：

- 复用现有指令发布元数据和群实验开关，隔离正式、测试和审核能力。
- 帮助菜单只展示当前群在当前运行模式真正能够执行的指令。

调整内容：

- 新增 `CommandReleaseProperties`，通过 `bot.command.runtime-mode` 支持 `PRODUCTION`、`TEST`、`REVIEW`。
- `CommandAvailability` 增加 `REVIEW`；feature 分支当前不包含审核专用指令，仅提供通用机制。
- `GroupCommandPolicy` 先校验全局运行模式，再校验群查询和实验开关。
- 正式模式只开放系统与正式指令；测试模式允许已开启实验功能的群使用实验指令；审核模式只额外开放审核指令。
- `HelpAction` 使用策略层一次读取当前群可用指令，菜单与执行规则保持一致。
- `application-dev.yml` 默认使用 `TEST`，其他环境默认 `PRODUCTION`，可通过 `BOT_COMMAND_RUNTIME_MODE` 显式覆盖。

验证范围：

- 覆盖三种模式互斥、生产隐藏实验指令、测试群实验开关、系统指令豁免和菜单过滤。
- 聚焦测试共 20 个通过。
- 完整 `mvn test` 共 217 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 QQ 群消息发送上下文统一

目标：

- 由最外层发送模板统一区分消息被动回复、事件被动回复和群主动消息。
- 具体 Action 不直接拼装 `event_id`、`msg_id`、`msg_seq` 或来源消息引用。

调整内容：

- `GroupMessageSender.send(...)` 保持现有群指令兼容，自动使用来源消息 ID，并显式设置默认 `msg_seq=1`。
- 新增 `sendEventReply(...)` 与 `sendActive(...)`，分别只设置 `event_id` 或不携带任何被动回复字段。
- `BotResponse` 支持声明 1 至 5 的回复序号，以及是否引用来源消息；消息 ID 仍由发送器填充。
- 完善 `MessageReferenceDto`，按 QQ 字段名输出 `message_id` 和 `ignore_get_message_error`。
- 主动消息、事件回复与消息回复参数执行互斥校验，非法组合不会调用 QQ 接口。
- 过渡 `RAW_MESSAGE` 中手工设置的传输字段会被清除并按发送上下文重建，避免旧 Action 绕过外层策略。
- `QqGroupMessageClient` 不再记录完整 QQ 返回对象，只记录群标识和返回类型。

验证范围：

- 覆盖默认/自定义 `msg_seq`、引用消息 JSON、主动消息、事件回复、非法参数组合和旧消息结构传输字段覆盖。
- 相关协议与发送器聚焦测试共 19 个通过；完整 `mvn test` 共 214 个测试通过，包含 3 个 Playwright/Vue 截图测试。

### 2026-07-13 群指令冷却审计与日志脱敏

目标：

- 群消息指令支持按“群 + 指令”设置响应间隔，内部功能默认 5 秒，外部调用默认 30 秒。
- 对已匹配指令的权限、策略、冷却、执行和发送结果建立可追踪记录，同时避免保存聊天原文和敏感参数。

调整内容：

- `REGEX` 统一声明是否涉及外部调用，`GroupCommandCooldownService` 按指令定义选择默认值并支持配置覆盖。
- 新增 `CommandInvocation`、状态枚举、Mapper 和 best-effort 记录服务，记录调用编号、群、成员、指令、分类、外部调用标识、状态、响应类型和耗时。
- 调用状态覆盖成功、权限不足、功能关闭、参数错误、冷却、无返回和失败；失败只保存最长 500 字符的脱敏摘要。
- 调用记录不保存原始 `content`、正则参数或外部请求参数，审计写库失败不会阻断消息回复。
- 调整入口、JX3API 和 QQ token 相关日志，不再输出完整 payload、上游返回对象或含敏感值的异常明文。
- 新增生产建表脚本 `docs/database/command-invocation.sql` 以及调用记录和文本脱敏单元测试。

数据作用域确认：

- `绑定 区服` 继续按 `group_openid` 写入 `group_info`，对整个群生效。
- `绑定角色 区服 角色名` 继续按 `member_openid` 写入 `user_info`，只对发送指令的账号生效。

验证结果：

- 群入口、调用记录、事件注册和敏感文本聚焦测试共 14 个，全部通过。
- 沙箱外完整 `mvn test` 共 208 个测试通过，包含 3 个 Playwright/Vue 截图测试。
- UTF-8 严格解码共检查 373 个项目文件，未发现非法编码。

### 2026-07-13 个人角色绑定与默认参数合并

目标：

- 群默认区服与个人角色配置按不同作用域持久化。
- 绑定个人角色后，角色查询可以省略重复的区服和角色参数。

调整内容：

- 新增 `UserInfo`、`UserInfoMapper` 和 `UserCommandPreferenceService`，以唯一 `member_openid` 保存个人默认区服和角色名。
- 新增 `绑定角色 服务器 角色名`、`我的绑定`、`查看绑定` 和 `解绑角色`；个人指令不会修改 `group_info`。
- 保留 `绑定 服务器` 的群管理员语义，继续按 `group_openid` 写入整群默认区服。
- Action 执行前统一按“本次显式参数、个人默认、群默认、全局默认”合并区服和角色。
- 角色、装备、战绩、名片、烟花、副本等角色查询允许在已绑定后省略参数；未提供参数且没有绑定时返回明确提示。
- 新增生产环境 PostgreSQL 建表脚本 `docs/database/user-info.sql`。

验证结果：

- 用户偏好、指令注册、群入口和事件注册目标测试共 22 个，通过且无失败。
- 图片 Action 与 Playwright 回归共 11 个测试通过，并人工抽查奇遇汇总、比赛记录、角色装备和团队招募截图。
- 完整 `mvn test` 共 202 个测试通过，无失败和跳过。

### 2026-07-12 高信息密度查询图片化与本地资源修复

目标：

- 已有 Vue 模板真正进入 Action 返回链路，而不只是保留为静态文件。
- Playwright 截图必须加载本地 CSS、图片和字体，不能只验证 PNG 非空。

调整内容：

- 角色装备、奇遇统计、奇遇汇总、名剑战绩、烟花记录和奇穴详情改为标准图片模板返回。
- 团队招募与副本进度按 DTO 展开 `server/time/data/num` 和 `server/name/globalId/data`。
- 新增 8 条 Action 图片响应测试，校验 `IMAGE`、模板名和模板数据。
- Playwright 资源基地址改为 `http://bot.local/static/`，通过只读路由从 classpath 返回 CSS、图片、脚本和字体。
- 截图前强制检查未加载图片和样式表，存在断图时直接失败。
- 修正 `奇遇汇总.html` 仍使用旧 `serendipity/name/time` 字段的问题，改为当前 DTO 的 `event/count/data`。

验证状态：

- 8 条 Action 图片响应测试通过。
- 初次截图抽查发现 CSS 和图片未加载，证明旧非空 PNG 检查不足；随后已完成资源路由和模板字段修复。
- 修复后的 Playwright 回归通过，3 个截图测试无失败；奇遇汇总、比赛记录、角色装备和团队招募产物已完成视觉抽查。

### 2026-07-12 部署指南与持续集成

目标：

- 将 Playwright、MinIO、自有媒体域名、数据库和 QQ webhook 的部署前提写成可执行文档。
- 每次 push 和 pull request 自动执行编码检查与完整测试。

调整内容：

- 新增 `docs/DEPLOYMENT.md`，记录 Java 21、PostgreSQL、MinIO、Chromium、环境变量、启动和发布检查。
- 新增 `.github/workflows/ci.yml`，使用 Ubuntu 22.04、Temurin Java 21 和 Maven 缓存。
- CI 严格检查 Java、Markdown、HTML、JSON、YAML 和 PowerShell 文件的 UTF-8 编码。
- CI 通过 Playwright CLI 安装 Chromium 与系统依赖后执行 `mvn test`。
- MinIO 和 JX3API 配置补齐环境变量映射，README 修正 Java 版本与配置前缀并增加部署文档索引。

验证结果：

- Playwright Maven CLI `--help` 执行成功，证明 CI 安装命令入口可解析。
- `mvn test` 执行成功，共 183 个测试通过。
- GitHub Actions 尚需提交到远端仓库后取得首次运行结果。

### 2026-07-12 阿里语音与 QQ 群语音链路

目标：

- 完成最后一个官方 HTTP contract 的群指令接入。
- 第三方阿里凭证独立配置，默认关闭，不在日志中泄露。
- JX3API 返回音频 URL 后，由最外层统一上传 QQ 并发送群语音。

调整内容：

- 新增实验指令 `语音 <文本>`，最多接收 200 个字符，沿用 30 秒外部调用冷却。
- 新增 `SoundProperties`，通过 `jx3api.sound` 配置开关、appkey、access、secret、发音人和音频参数。
- 配置关闭时直接返回“语音功能尚未配置”，不会调用 JX3API。
- `BotResponse` 增加 `AUDIO_URL`，`GroupMessageSender` 统一转换为 QQ media 消息。
- `QqGroupMessageClient` 增加语音上传，按 QQ 官方协议使用 `file_type=3`。
- `SensitiveDataUtil` 增加 appkey 脱敏。
- `application-dev.yml` 中 QQ 凭证改为环境变量引用，配置扫描不再发现明文 token、secret 或 password。
- HTTP 群指令覆盖矩阵达到 61/61；两个旧接口继续单独标记为 `LEGACY`。

验证结果：

- 语音配置、关闭短路、JX3API 参数、音频 URL、QQ 上传类型、media 拼装和凭证脱敏测试通过。
- `mvn test` 执行成功，共 183 个测试通过。
- 真实阿里凭证和 QQ 正式平台联调尚未执行。

### 2026-07-11 最后一批普通官方查询接口接入群指令

目标：

- 将除阿里语音外的剩余官方 HTTP 查询全部纳入群指令系统。
- 复用参数策略，但保持每条指令的元数据、endpoint 和覆盖状态独立。

调整内容：

- 新增随机名片、缓存名片、资历排行、阵营事件、诛恶事件、关隘首领、本日赤兔、本周赤兔、马场刷新、试炼排行、贴吧物价、黑市物价、搜索物品、帮战记录、副本解密、统战歪歪、八卦帖子和舔狗日记 18 条指令。
- 新增 `OfficialExtendedAction`，集中处理参数结构简单且统一返回精简文本的官方扩展查询。
- 共享 Action 使用 `switch(REGEX)` 严格选择参数模板，未知指令立即失败，不静默套用默认参数。
- 扩展 `body`、`force`、`school` 和 `tags` 命名参数。
- 新增 18 条参数化测试，逐条验证共享 Action 的官方 endpoint、参数和响应。
- 官方 HTTP 群指令覆盖达到 60/61；剩余阿里语音需要独立第三方凭证和 QQ 媒体返回设计。

验证结果：

- 当时 Maven 离线模式因沙箱本地仓库缺少 Spring Boot parent POM 无法启动，随后联网恢复后完成验证。
- 后续 `mvn test` 执行成功，当时共 177 个测试通过；最新结果见上方变更记录。

### 2026-07-11 第三批官方查询接口接入群指令

目标：

- 继续消除已有 Action 中的 `null` 占位，并为缺失接口补齐处理器。
- 扩展奇遇、师徒、榜单、掉落和名片相关群查询。

调整内容：

- 新增技改记录、小药推荐、扶摇预测和近期奇遇 4 个 Action。
- 完善奇遇汇总、师徒系统、本服榜单、掉落统计和角色名片 Action。
- 新增技改、小药、扶摇、近期奇遇、奇遇汇总、师徒、榜单、掉落、名片和所有名片 10 条群指令。
- `RoleShowCardAction` 根据当前 `REGEX` 分别调用角色名片和所有名片官方接口。
- 命名参数扩展 `type`、`keyword` 和 `name1`，复用统一服务器与整数校验。
- 机器可读覆盖矩阵同步为 42 个官方群 HTTP 接口和 2 个旧接口。

验证结果：

- 第三批新增指令已纳入会员指令参数化测试，官方 endpoint、参数和文本响应均验证通过。
- 第三批针对性测试共 27 项通过，主代码和测试代码完成编译，覆盖矩阵验证通过。
- 后续 `mvn test` 执行成功，当时共 177 个测试通过；最新结果见上方变更记录。

### 2026-07-11 第二批官方会员接口接入群指令

目标：

- 继续把已有 HTTP contract 和 Action 类的会员接口开放为群聊指令。
- 对 QQ 号、竞技模式和 ticket 参数执行统一且可测试的处理。

调整内容：

- 新增阵营拍卖、的卢记录、骗子查询、未做奇遇、名剑排行、名剑统计、角色信息、角色百战、心法阵眼和技能详情 10 条群指令。
- 对应 Action 从 `null` 占位改为真实参数拼装和精简文本返回。
- `CommandArguments` 增加长整数与限定整数集合校验，支持 QQ 号以及 22、33、55 名剑模式。
- 需要推栏凭证的名剑、阵眼和技能接口统一加入 `ticket` 参数。
- 机器可读覆盖矩阵同步为 32 个官方群 HTTP 接口和 2 个旧接口。

验证结果：

- 10 条新增指令均有参数化测试，覆盖默认服务器、QQ 长整数、名剑模式、ticket、官方路径和非空响应。
- `mvn test` 执行成功，共 149 个测试通过。

### 2026-07-11 第一批官方免费接口接入群指令

目标：

- 将已有 HTTP contract 但尚不可从群聊调用的免费接口接入统一模板链路。
- 每个新指令具备明确正则、参数规则、帮助元数据和精简文本返回。

调整内容：

- 新增活动月历、行侠事件、科举答题、家园鲜花、家园装饰、器物图谱、新闻资讯和搜索区服 8 条群指令。
- 补齐对应 Action 的请求参数与 `buildReadableText` 返回，删除这些 Action 原有的 `null` 占位实现。
- 扩展命名参数提取，支持 `num`、`limit`、`subject`、`name` 和 `map`。
- 月历天数、新闻条数通过 `CommandArguments` 执行整数范围校验。
- 覆盖矩阵同步为 22 个官方群查询接口，另有角色装备和副本进度 2 个旧接口。
- 经语义复核，官方 `role.detail` 不等价于装备属性，因此没有把角色装备错误迁移到该接口。

验证结果：

- 8 条新增指令均有参数化测试，验证命令输入、官方路径、请求参数和非空文本响应。
- `mvn test` 执行成功，共 139 个测试通过。

### 2026-07-11 通用指令参数与 HTTP 群指令覆盖矩阵

目标：

- 正则参数只解析一次，并由注册表传递到具体 Action。
- 服务器、角色名、关键词和数字参数使用一致的兼容规则。
- 61 个官方 HTTP contract 与当前群指令映射具备机器可读、可测试的覆盖关系。

调整内容：

- 新增只读 `CommandArguments`，统一兼容 `server/server1/value/value1/roleName/loop` 命名组。
- `ResolvedJx3Command` 改为携带 `CommandArguments`，`GroupAtMessageAction` 将同一实例传入 Action 上下文。
- 已注册查询 Action 不再重复调用 `REGEX.handleEncounter`。
- 数字参数增加统一整数和范围校验，参数错误不进入外部调用。
- `BindServerAction` 改为类级事务，确保新的四参数模板入口仍受事务保护。
- 新增 `docs/testing/jx3api-command-coverage.json`，记录 `REGEX -> MethodEnum` 和官方/旧接口状态。
- 新增覆盖矩阵测试，自动与当前 `REGEX` 和 61 个官方 HTTP contract 双向核对。
- 明确当前有 14 个官方 HTTP 接口已注册为群查询；角色装备和副本进度仍使用旧接口并标记为 `LEGACY`。

验证结果：

- `mvn test` 执行成功，共 131 个测试通过，包含参数对象、注册表传递、覆盖矩阵和全部既有回归测试。

### 2026-07-11 JX3API 统一错误返回与请求追踪

目标：

- 外部查询失败不再由各个 Action 重复判断和拼接文案。
- 空数据、限流、认证失效、超时和未知错误具备稳定且不泄露敏感信息的用户提示。
- 日志和用户提示通过请求编号关联，并记录单次外部调用耗时。

调整内容：

- 新增 `Jx3ApiFailureMapper`，统一映射接口状态、错误消息和调用异常。
- `Jx3BaseAction` 在模板流程中统一短路空数据和非成功响应，成功结果才交给子类拼装。
- 每次 JX3API 调用生成 8 位请求编号，开始与结束日志记录指令、接口路径和耗时。
- 用户错误提示携带请求编号，上游原始错误消息不直接发送到群聊。
- 修正 JX3API 空响应日志的错误文案。

验证结果：

- `mvn test` 执行成功，共 127 个测试通过。
- 新增限流、认证信息脱敏、超时、通用异常和空数据模板短路测试。

### 2026-07-11 群消息指令独立冷却

目标：

- 群消息中的相同指令必须保持可配置的最小响应间隔。
- 每条指令可以单独覆盖冷却时间。
- 内部指令默认 5 秒，外部调用默认 30 秒。

调整内容：

- `REGEX` 增加 `defaultCooldownSeconds` 元数据；JX3API 指令默认 30 秒，内部指令默认 5 秒。
- DPS 虽然不绑定 `MethodEnum`，但因调用外部服务显式设置为 30 秒。
- 新增 `CommandCooldownProperties`，通过 `bot.command.cooldown-seconds.<REGEX名称>` 独立覆盖。
- 配置 `0` 可关闭单条指令冷却；负数、超过 86400 秒或未知指令名会启动失败。
- 新增 `GroupCommandCooldownService`，按群 openid 和指令隔离冷却状态。
- 使用 `ConcurrentHashMap.compute` 原子获取执行权，并发相同请求只放行一个。
- 冷却采用两阶段状态：Action 执行期间保持占用，QQ 发送结束后才开始倒计时。
- 权限和群功能策略先于冷却判断，不合法请求不消耗冷却。
- 冷却期间不发送提示消息，确保真实响应间隔符合配置。
- 增加内部/外部默认值、DPS、覆盖、关闭、群隔离、指令隔离、慢请求和并发竞争测试。

验证状态：

- 新增代码完成引用、构造器、括号结构和严格 UTF-8 静态检查。
- `mvn test` 执行成功，共 123 个测试通过，冷却规则、并发控制和配置校验均已覆盖。

### 2026-07-11 QQ Markdown、Keyboard、Ark 与 Embed 类型化

目标：

- 将原来的空消息 DTO 补成与 QQ 官方协议一致的 Java Bean。
- 群消息发送前校验必填结构和按钮数量。
- 明确 Embed 不支持群聊，避免发送一个平台必然拒绝的请求。

调整内容：

- `MarkdownDto` 支持 `content`、`custom_template_id` 和 `params`。
- `KeyboardDto` 支持模板 ID、自定义 rows/buttons、render_data、action 和 permission。
- `ArkDto` 支持 `template_id`、普通 kv 和数组变量的 `obj/obj_kv`。
- 新增 `EmbedDto`，包含 title、prompt、description、thumbnail 和 fields。
- `BotResponse` 与 `TxMessageInfo` 的 Embed 字段改为类型化 `EmbedDto`。
- `GroupMessageSender` 增加文本、图片、媒体、Markdown、Keyboard 和 Ark 结构校验。
- 自定义 Keyboard 限制为最多 5 行、每行最多 5 个按钮。
- Embed 在群聊发送路径明确抛出不支持异常。
- 新增官方 snake_case 字段序列化和各消息类型发送测试。

官方能力边界：

- 群聊自定义 Markdown 与按钮已开放；按钮必须依附 Markdown。
- Ark 主动消息可用，被动消息需要符合平台准入条件。
- Embed 当前不支持群聊，仅支持文字子频道和频道私信。

验证状态：

- 文件引用、构造调用、括号结构和严格 UTF-8 静态检查已执行。
- 后续完整 `mvn test` 验证成功，最新共 123 个测试通过。

### 2026-07-11 群查询开关与实验功能灰度

目标：

- 群主或管理员可以按群控制正式查询和实验功能。
- 审核、测试中的能力与正式查询隔离，默认不向普通群开放。
- 关闭查询后仍能使用系统指令恢复设置。

调整内容：

- `GroupInfo` 增加 `commandsEnabled` 和 `experimentalEnabled` 持久化字段。
- `REGEX` 增加 `CommandAvailability`，区分 `SYSTEM`、`PRODUCTION`、`EXPERIMENTAL`。
- 新增管理员指令 `群设置 查询 开启/关闭` 和 `群设置 实验 开启/关闭`。
- 新增 `GroupSettingsAction`，创建或更新当前群配置。
- 新增 `GroupCommandPolicy`，在业务 Action 执行前统一判断群开关和发布阶段。
- 帮助、绑定和群设置属于系统指令，总开关关闭时仍可执行。
- 水墨圈圈、DPS、吃瓜归入实验功能，历史群和新群默认关闭。
- 历史群配置字段为空时，正式查询默认开启，保证升级兼容。

验证结果：

- `mvn test` 执行成功，当时共 109 个测试通过。
- 覆盖默认策略、关闭查询、实验开关、系统指令绕过、配置创建和更新。

### 2026-07-11 JX3API 短时缓存与 payload 回放工具

目标：

- 减少热门公开查询对 JX3API 的重复请求，同时保持角色实时数据新鲜。
- 提供可复用的本地群消息 payload 和 webhook 回放入口。

调整内容：

- 新增 `Jx3ApiResponseCache`，只缓存配置了策略且返回码为 200 的 JX3API 响应。
- 开服和招募缓存 30 秒；金价、物价、沙盘缓存 2 分钟；日常、公告、百战缓存 5 分钟。
- 角色装备、奇遇、战绩、烟花、DPS 和写操作不缓存。
- 缓存键忽略 token，并使用 SHA-256 参数摘要，避免凭证明文进入缓存键。
- 缓存按区服、物品等业务参数隔离，到期后立即失效，失败响应不写缓存。
- 新增 `docs/testing/payloads/group-message-create.json`，Java payload 测试读取同一份样例。
- 新增 `tools/replay-payload.ps1`，严格按 UTF-8 校验并 POST payload 到本地 webhook。

验证结果：

- 当时共 104 个测试通过；最新结果见上方变更记录。
- 缓存命中、参数隔离、token 轮换、TTL 到期、失败响应和实时接口不缓存均有测试覆盖。
- PowerShell 回放脚本语法解析通过，payload JSON 解析通过。

### 2026-07-11 群指令访问权限基础

目标：

- 写配置类指令不能由普通群成员执行。
- 权限规则与指令定义放在一起，群消息入口统一拦截。

调整内容：

- `REGEX` 增加 `CommandAccess` 元数据，支持公开和群管理员两种基础级别。
- `绑定区服` 标记为群管理员指令，仅 `owner` 和 `admin` 角色允许执行。
- 未授权请求直接由 `GroupAtMessageAction` 返回权限提示，不进入业务 Action，也不会写数据库。
- 普通查询指令继续保持公开访问。
- 新增权限元数据和普通成员拦截测试。

验证结果：

- `mvn test` 执行成功。
- 当时共 101 个测试通过；最新结果见上方变更记录。

### 2026-07-11 指令元数据、别名与自动帮助菜单

目标：

- 指令帮助不再单独手工维护。
- 为统一指令注册表补充用户可读的分类、名称、说明和示例。
- `/指令`、直接指令和别名走同一个处理器。

调整内容：

- `REGEX` 每个枚举项增加 `CommandGroup`、展示名称、功能描述和调用示例。
- 新增 `HelpAction`，支持 `帮助`、`菜单`、`查询帮助`，不调用外部接口。
- 帮助文本直接遍历注册定义生成，并按基础、免费查询、会员查询和其他分类展示。
- `开服状态` 增加为 `开服` 的显式别名，参数解析结果保持一致。
- 新增帮助 Action、别名解析、分组菜单和不暴露正则表达式的测试。

验证结果：

- `mvn test` 执行成功。
- 当时共 99 个测试通过；最新结果见上方变更记录。

### 2026-07-11 事件注册、配置校验与敏感日志治理

目标：

- QQ dispatch 事件路由不依赖静态 Spring 上下文。
- 补齐 webhook 到群消息 Action 的离线路由验证。
- 配置缺失在启动阶段给出明确错误，日志不得输出凭证和 ticket。

调整内容：

- 新增 `PayloadActionRegistry`，启动时校验 `PayloadTEnum` 中每个事件都有处理器。
- `BotMessageService` 改用构造器注入的事件注册表，删除零引用的 `SpringContextUtil`。
- `GROUP_AT_MESSAGE_CREATE` 与 `GROUP_MESSAGE_CREATE` 继续共享 `GroupAtMessageAction`。
- 增加 `BotMessageServiceTest`、`PayloadActionRegistryTest` 和 `GroupAtMessageActionTest`，覆盖 dispatch 到 JX3 Action 再到发送器的离线路径。
- 新增 `SensitiveDataUtil`，递归过滤 token、secret、ticket、password、authorization 等敏感字段。
- JX3API 请求日志使用脱敏副本，并且不再修改 Action 传入的参数 Map。
- QQ token 刷新增加线程同步、60 秒过期缓冲、返回值校验和失败即中止。
- QQ token 获取异常日志不再打印包含 `clientSecret` 的请求参数。
- `TxBotProperty` 与启用后的 `ApiProperties` 增加必需配置启动校验。
- 跟踪的数据库密码改为 `DB_PASSWORD` 环境变量，DPS 服务改用专用 `dpsToken`。

验证结果：

- `mvn test` 执行成功。
- 当时共 96 个测试通过；最新结果见上方变更记录。

### 2026-07-11 统一指令注册与 QQ OpenAPI 客户端分层

目标：

- 群消息入口不再通过静态 Spring 上下文查找 JX3 Action。
- 在应用启动时验证每条已注册指令都存在对应处理器。
- QQ 群消息发送和图片上传地址统一由客户端维护，业务发送器不直接拼接 URL。

调整内容：

- 新增 `Jx3CommandRegistry`，统一完成指令规范化、`REGEX` 匹配、参数提取和 Action 定位。
- 新增 `ResolvedJx3Command`，承载规范化指令、定义、只读参数和处理器。
- `REGEX.baseAction` 改为 `Class<? extends Jx3BaseAction>`，消除原始 `Class` 类型。
- `GroupAtMessageAction` 改为构造器注入指令注册表，不再调用 `SpringContextUtil.getBean`。
- 注册表兼容 Spring AOP 代理，并对重复 Action 和缺失处理器给出明确启动异常。
- 新增 `QqGroupMessageClient`，统一封装群消息发送、图片上传、`file_info` 校验和 QQ 请求日志。
- `GroupMessageSender` 只负责将 `BotResponse` 转换为 `TxMessageInfo`，之后委托 QQ 客户端发送。
- 新增注册表和 QQ 客户端单元测试，并保留机器可读 check 文档的 `caseId` 追踪。

验证结果：

- `mvn test` 执行成功。
- 当时共 87 个测试通过；最新结果见上方变更记录。

### 2026-07-11 已注册 JX3 指令闭环与并发上下文修正

目标：

- 按项目架构落实 `REGEX -> Jx3BaseAction -> JX3API -> BotResponse -> GroupMessageSender -> QQ` 主链路。
- 避免 Spring 单例 Action 在并发群消息下共享并覆盖当前请求上下文。
- 已匹配的指令必须得到明确业务返回，不能以 `null` 静默结束。

调整内容：

- `Jx3BaseAction` 使用请求线程上下文保存消息、指令和正则，并在 `finally` 中清理。
- 增加空请求参数、空接口结果、非 200 结果、子类空返回和请求异常的统一文本兜底。
- 增加精简文本构建能力，过滤 ID、URL、token、头像等不可读字段，保留中文描述与可读时间，并限制消息长度。
- 补齐当前 `REGEX` 注册的 20 个 Action 的参数解析或明确返回，其中 JX3API 指令调用真实 HTTP 接口。
- 区服参数统一按“指令参数、群默认配置、全局默认配置”的顺序解析。
- `水墨圈圈`、`吃瓜` 仍属于内部待适配能力，当前返回明确状态文本，不伪造 JX3API 查询结果。
- 新增 `Jx3BaseActionTest`，覆盖单例 Action 并发上下文隔离和精简文本字段过滤。
- 更新架构文档，区分 HTTP 接口覆盖、群指令覆盖和展示覆盖三个完成度口径。

验证状态：

- 严格 UTF-8 解码检查通过，共检查项目中的 310 个 Java、Markdown、HTML 和 JSON 文件（包含未跟踪的新文件）。
- 后续已恢复 Maven Central 访问并完成验证；最新结果见上方变更记录。

### 2026-07-11 文档职责拆分

目标：

- 将长期项目设计与逐次变更记录分开维护。
- `PROJECT_DESIGN.md` 后续作为变更历史使用。
- 新增独立设计文档，作为 AI 编程和人工开发时的当前架构依据。

涉及文件：

- `README.md`
- `docs/PROJECT_DESIGN.md`
- `docs/PROJECT_ARCHITECTURE.md`

调整内容：

- 新增 `docs/PROJECT_ARCHITECTURE.md`，承接项目定位、Feature 清单、模块职责、图片消息设计、数据配置和开发约定。
- `docs/PROJECT_DESIGN.md` 改为变更历史，只追加历史记录，不再承载长期设计正文。
- `README.md` 更新项目文档索引，分别指向设计文档和变更历史。

### 2026-07-10 feature-jx3api 通用调用链路调整

目标：

- `feature-jx3api` 分支恢复和完善真实 JX3API HTTP 调用链路。
- 审核用写死返回保留在 `valid` 分支，不进入本分支的群消息主链路。
- 当前只调整 JX3API HTTP 通用链路，WS 相关能力暂不处理。

涉及文件：

- `src/main/java/com/grafie/botjava/action/GroupAtMessageAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/util/REGEX.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/base/Jx3BaseAction.java`
- `README.md`

调整内容：

- 群消息处理改为直接按 `REGEX -> Jx3BaseAction -> Jx3RequestUtil -> MethodEnum -> TxMessageInfo` 调用真实 JX3API HTTP 链路。
- `REGEX` 增加外部指令规范化，支持 `/指令` 和 `指令` 两种输入形式。
- 示例：`/开服`、`开服`、`/开服 乾坤一掷`、`开服 乾坤一掷` 会进入同一套匹配规则。
- `Jx3BaseAction` 增加 JX3API 调用失败和非成功返回的通用文本兜底。
- `README.md` 保持项目介绍和索引定位，不记录详细变更历史。

### 2026-07-10 HTML 模板生成图片工具完善

目标：

- 为需要以图片形式发送到 QQ 的接口返回值提供通用 HTML 截图能力。
- 模板优先使用 Vue 写法，Java 只负责注入数据和截图。

涉及文件：

- `src/main/java/com/grafie/botjava/util/HtmlToImageUtl.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/base/Jx3BaseAction.java`

调整内容：

- 新增 `HtmlToImageUtl.renderTemplateToImage(String htmlName, Object data)`。
- 新增 `HtmlToImageUtl.renderTemplateJsonToImage(String htmlName, String jsonData)`。
- 不指定输出路径时，图片默认生成到 `target/generated-images/html`。
- `htmlName` 支持传不带 `.html` 后缀的模板名，例如 `角色详情` 会匹配 `src/main/resources/static/角色详情.html`。
- 模板数据统一注入到 `window.__BOT_DATA__`。
- 工具会自动补充资源 `<base>`，保证模板中的 `css/**`、`js/**`、`img/**` 等相对路径可以被 Playwright 正确加载。
- 工具会在页面中自动内联 Vue 运行时并挂载 `#main`，后续模板可以使用 Vue 的 `{{ }}`、`v-for`、`v-if` 等语法。
- `Jx3BaseAction` 的图片消息链路改为直接调用通用模板截图入口。
- 后续图片发送需要接入自有域名文件服务器：先将 PNG 上传到本地文件服务，再用自有域名拼出公网 URL，最后作为 QQ 群富媒体上传参数。
- 文件服务实现暂不绑定具体产品，后续可以在 MinIO、本地静态文件服务、对象存储网关之间选择，但业务层应只依赖统一上传接口。

Vue 模板约定：

```html
<div id="main">
    <h1>{{ name }}</h1>
    <div v-for="item in list">
        {{ item.title }}
    </div>
</div>
```

JSON 数据与 HTML 字段对应规则：

- Java 侧传入的 `Object data` 或 JSON 字符串会被完整注入到浏览器的 `window.__BOT_DATA__`。
- 工具会自动创建 Vue 实例，并将 `window.__BOT_DATA__` 作为 `#main` 的根级 `data`。
- JSON 根字段可以在 HTML 中直接使用，例如 JSON 里有 `{"server":"乾坤一掷"}`，HTML 中写 `{{ server }}`。
- JSON 数组字段使用 `v-for`，例如 `{"data":[{"name":"测试"}]}` 对应 `<tr v-for="one in data"><td>{{ one.name }}</td></tr>`。
- JSON 嵌套对象使用点号或方括号访问，例如 `data.group.server`、`data.performance['3v3'].grade`。
- 条件展示使用 `v-if` / `v-else`，例如 `<div v-if="data.group.robot_status">开</div><div v-else>关</div>`。
- 动态 HTML 属性不能写旧式 `src="{{ icon }}"`，必须使用 Vue 绑定，例如 `<img :src="icon">`。
- 需要拼接路径时使用表达式，例如 `<img :src="'sect/' + school + '.png'">`。
- 动态 class 使用 `:class`，例如 `<div :class="['boss', boss.finished ? 'completed' : 'not-completed']">`。
- 动态 style 使用 `:style`，例如 `<strong :style="{ color: one.color }">{{ one.name }}</strong>`。
- 后续新增或美化模板时，不再使用 `{% for %}`、`{% if %}`、`{% endif %}` 这类旧模板语法。

Java 调用示例：

```java
String imagePath = HtmlToImageUtl.renderTemplateToImage("角色详情", data);
String imagePathFromJson = HtmlToImageUtl.renderTemplateJsonToImage("角色详情", jsonData);
```

### 2026-07-10 现有 HTML 模板 Vue 化

目标：

- 现有 `src/main/resources/static/*.html` 后续仍作为图片消息模板的美化基础。
- 将旧 Jinja 风格控制语法迁移为 Vue 写法，保证可以被 `HtmlToImageUtl` 直接渲染截图。

涉及文件：

- `src/main/resources/static/*.html`
- `src/test/java/com/grafie/botjava/util/HtmlToImageUtlTest.java`
- `src/test/resources/static/测试模板.html`

调整内容：

- 将 `{% for %}` / `{% endfor %}` 迁移为 `v-for`。
- 将 `{% if %}` / `{% else %}` / `{% endif %}` 迁移为 `v-if` / `v-else`。
- 将图片、图标、动态 class、动态 style 的旧属性插值迁移为 Vue 属性绑定。
- 修正 `奇穴信息.html` 的嵌套循环和 `rowspan` 写法。
- 增加代表模板截图测试，覆盖 `团队招募.html`、`角色装备.html`、`副本进度.html`、`奇穴信息.html`。

### 2026-07-10 物品价格模板折线图

目标：

- 使用现有 `物品价格.html` 作为价格趋势图片模板。
- 明确 Java JSON 数据和 Vue 模板字段的对应关系。
- 保证截图时不依赖外部网络资源。

调整内容：

- `物品价格.html` 改为 Vue 模板，支持直接传入物品价格对象或 `{ "data": 物品价格对象 }`。
- 价格趋势图使用模板内 SVG 生成，字段来源为价格记录的 `date` / `datetime` 和 `value`。
- 保留分区价格记录展示，按电信区、双线区、无界区、公示期、在售期、指定服务器展示。
- 截图测试新增 `物品价格.html` 覆盖。

### 2026-07-10 业务返回与 QQ 发送解耦

目标：

- 具体 Action 子类只负责 JX3API 参数解析和业务返回数据整理。
- 最外层根据返回类型统一拼装 QQ 消息并调用 QQ 接口。
- 为后续 Markdown、Ark、按钮、富媒体等 QQ 官方消息类型扩展预留入口。

涉及文件：

- `src/main/java/com/grafie/botjava/entity/dto/common/BotResponse.java`
- `src/main/java/com/grafie/botjava/service/GroupMessageSender.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/base/Jx3BaseAction.java`
- `src/main/java/com/grafie/botjava/action/GroupAtMessageAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/*.java`

调整内容：

- 新增 `BotResponse`，用于承载业务返回类型和业务数据。
- `Jx3BaseAction.doRequest` 和 `dealAfterJx3ApiRequest` 改为返回 `BotResponse`。
- `GroupMessageSender` 负责将 `BotResponse` 转换为 `TxMessageInfo`，并统一调用 QQ 群消息发送接口。
- 图片模板返回使用 `BotResponse.image(templateName, templateData)`。
- 外部图片 URL 返回使用 `BotResponse.imageUrl(imageUrl)`，由 `GroupMessageSender` 统一调用 QQ 文件上传接口。
- 文本返回使用 `BotResponse.text(content)`。
- 保留 `BotResponse.message(rawMessage)` 作为历史手写消息结构的过渡入口。

### 2026-07-10 JX3API HTTP 文档路径与返回结构同步

目标：

- 根据 `https://www.jx3api.com/#/doc` 左侧菜单树同步 HTTP 接口路径。
- 参考官方返回 JSON 示例，先修正当前高频接口的 DTO 字段兼容。
- 暂不处理 socket/ws 事件接口。

涉及文件：

- `docs/JX3API_HTTP_API_INVENTORY.md`
- `src/main/java/com/grafie/botjava/jx3/http/MethodEnum.java`
- `src/main/java/com/grafie/botjava/jx3/http/util/Jx3RequestUtil.java`
- `src/main/java/com/grafie/botjava/jx3/http/data/server/ServerCheckData.java`
- `src/main/java/com/grafie/botjava/jx3/http/data/trade/record/TradeRecordData.java`
- `src/main/java/com/grafie/botjava/jx3/http/data/trade/record/SaleData.java`
- `src/main/java/com/grafie/botjava/jx3/http/data/role/RoleDetailedData.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/ServerCheckAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/ActiveCurrentAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordAction.java`

调整内容：

- 批量读取官方 HTTP 文档，生成接口清单。
- 将 `MethodEnum` 中旧路径同步为当前官方路径，例如 `status.check`、`role.detail`、`recruit.search`、`trade.item.records`。
- `Jx3RequestUtil` 支持空参数请求，并关闭未知字段反序列化失败。
- `ServerCheckData.status` 从数字改为字符串，匹配官方返回的 `爆满`、`维护` 等文本。
- `TradeRecordData` 兼容官方 `data.list` 和旧字段 `data.data`。
- `SaleData` 兼容官方 `sale` 和旧字段 `sales`。
- `RoleDetailedData` 兼容官方 `globalId` 和旧字段 `globalRoleId`。
- `TradeRecordAction` 接入官方物品价格接口，并返回 `物品价格.html` 图片模板。

### 2026-07-11 JX3API HTTP 全量反序列化测试覆盖

目标：

- 所有官方 HTTP 文档接口都必须有可执行的反序列化 contract。
- 后续接口映射、DTO 字段、返回类型调整时，必须同步更新 check 文档和测试。
- 测试不访问真实 JX3API，全部使用官方文档中的返回 JSON 示例。

涉及文件：

- `docs/testing/jx3api-http-check.json`
- `src/test/java/com/grafie/botjava/jx3/http/Jx3ApiHttpContractTest.java`
- `src/test/java/com/grafie/botjava/testing/Jx3ApiHttpCheckDocumentTest.java`
- `src/main/java/com/grafie/botjava/jx3/http/MethodEnum.java`

调整内容：

- `docs/testing/jx3api-http-check.json` 增加 `httpContracts`，覆盖 61 个官方 HTTP 接口。
- 每个 HTTP contract 包含 `caseId`、接口名、文档路径、`MethodEnum`、官方路径、`dataShape` 和官方返回 JSON。
- `Jx3ApiHttpContractTest` 增加参数化测试，逐个校验官方路径与 `MethodEnum` 一致，并验证返回 JSON 能被当前映射反序列化。
- 反序列化 contract 对 object 类型和 list 元素增加有效字段断言，避免 DTO 字段名失配时只得到空对象或空元素。
- `Jx3ApiHttpCheckDocumentTest` 增加元测试，校验 check 文档结构、声明测试文件、普通 case 覆盖、全量 HTTP contract 数量与接口清单一致。
- `MethodEnum` 补齐缺失的官方 HTTP 接口枚举，未类型化 DTO 的接口暂时使用 `Map.class` 作为安全承接类型。
- `RoleShowCardData`、`RoleShowRandomData`、`RoleMonsterData` 和 `MonsterSkill` 兼容官方名片、随机名片、缓存名片、角色百战返回中的新字段名，同时保留旧字段别名。
- 默认单元测试不连接真实开发数据库，也不调用 QQ token 接口；真实外部联调后续应放到单独的 integration profile 或手工验收流程。
- `BotTxTests` 保留为离线 payload 反序列化测试，覆盖 `GROUP_MESSAGE_CREATE` 结构和 `/指令 参数` 格式。

验证命令：

```bash
mvn test
```

验证结果：

- 全项目默认测试：78 个测试通过。
- `Jx3ApiHttpContractTest`：67 个测试通过，其中包含 61 个官方 HTTP 反序列化 contract。
- `GroupMessageSenderTest`：2 个测试通过。
- `Jx3ApiHttpCheckDocumentTest`：4 个测试通过。
- `HtmlToImageUtlTest`：3 个测试通过，覆盖 Vue HTML 模板截图链路。

### 2026-08-12 JX3API 官方 OpenAPI 路径、LV.2 token 与 WS 事件清单同步

目标：

- 根据 `https://www.jx3api.com/openapi` 同步当前 HTTP 路径和 `x-level`。
- 区分 JX3API 普通 HTTP token、LV.2 HTTP token 和 JX3API WS token。
- 根据 `https://www.jx3api.com/openapi.socket.json` 记录 WS 推送事件，并为未类型化事件提供安全兜底。

涉及文件：

- `docs/JX3API_HTTP_API_INVENTORY.md`
- `docs/JX3API_WS_API_INVENTORY.md`
- `src/main/java/com/grafie/botjava/jx3/config/ApiProperties.java`
- `src/main/java/com/grafie/botjava/jx3/config/JX3ApiWsAutoConfiguration.java`
- `src/main/java/com/grafie/botjava/jx3/http/MethodEnum.java`
- `src/main/java/com/grafie/botjava/jx3/http/util/Jx3RequestUtil.java`
- `src/main/java/com/grafie/botjava/jx3/ws/action/WsActionHandler.java`
- `src/main/java/com/grafie/botjava/jx3/ws/data/Jx3WsEventEnum.java`
- `src/main/java/com/grafie/botjava/jx3/ws/data/GenericWsData.java`

调整内容：

- 官方 HTTP 文档当前解析到 78 个接口；`MethodEnum` 中已接入能力的旧 `/data/...` 路径同步为当前路径。
- `MethodEnum.getApiLevel()` 按官方 `x-level` 标注现有接口等级，`Jx3RequestUtil` 根据等级选择 token。
- `jx3api.api.api-v2-token` 专用于 LV.2 HTTP 接口，未配置时回退 `jx3api.api.api-token`。
- `jx3api.ws.ws-token` 是 JX3API 游戏事件 WebSocket 的独立 token，启用 WS 时和 `ws-url` 一起强制校验。
- 官方 WS 文档当前解析到 39 个事件；未类型化事件不再返回 null，而是转换为 `GenericWsData` 后继续进入推送服务。
- JX3API WS 默认数据类扫描包修正为 `com.grafie.botjava.jx3.ws.data`。

待补事项：

- 官方 HTTP 新增接口如 `trade.manufacture`、`trade.wanbaolou`、`school.search`、`skill.calculate`、`event.strategy`、`chat.records`、`card.preset` 和新增骚话类接口，需要后续逐个补 DTO、Action、REGEX、模板/文本返回和 contract。
- 官方 WS 新增事件如 `1014`、`1015`、`1017`、`1018`、`1111` 至 `1122`、`1201` 当前仅有事件清单和兜底数据，尚未接入群订阅、权限和推送文案。
### 2026-08-12 群指令冷却剩余时间回复

目标：

- 群指令命中冷却时，不再静默丢弃请求，而是回复剩余 CD 秒数。

涉及文件：

- `src/main/java/com/grafie/botjava/service/GroupCommandExecutionService.java`
- `src/test/java/com/grafie/botjava/service/GroupCommandExecutionServiceTest.java`

调整内容：

- `GroupCommandExecutionService` 在 `GroupCommandCooldownService.Decision.deny` 时返回 `指令冷却中，请在 X 秒后重试。`。
- 冷却命中仍记录为 `CommandInvocationStatus.COOLDOWN`，响应类型记录为 `TEXT`。
- 冷却分支不执行 Action，也不占用新的冷却预约。
### 2026-08-12 JX3API 可读业务失败返回

目标：

- JX3API 返回“参数错误、未找到、暂无数据”等用户可自行修正的业务失败时，直接把可读原因返回给群消息。

涉及文件：

- `src/main/java/com/grafie/botjava/jx3/http/action/base/Jx3ApiFailureMapper.java`
- `src/test/java/com/grafie/botjava/jx3/http/action/base/Jx3ApiFailureMapperTest.java`

调整内容：

- `400`、`404` 或包含“没有、木有、未找到、不存在、暂无、为空、换个名字、参数”等提示的 JX3API 失败，会返回脱敏后的上游 `message`，并保留请求编号。
- 认证失败、权限失败、限流、超时和未知异常继续使用固定安全文案，避免把 token、凭证或内部异常暴露到群聊。

### 2026-08-12 JX3API 原始响应日志

目标：

- JX3API HTTP 调用先接收原始响应字符串并记录，再解析为 `RequestResult` 和具体业务 DTO，避免 DTO 结构不匹配时缺少上游返回结构。

涉及文件：

- `src/main/java/com/grafie/botjava/jx3/http/util/Jx3RequestUtil.java`
- `src/main/java/com/grafie/botjava/jx3/http/RequestResult.java`

调整内容：

- `doPostRequest` 改为 `bodyToMono(String.class)`，先打印脱敏且最长 4000 字符的 `responseBody`，再解析成 `RequestResult`。
- `RequestResult` 增加 `rawResponseBody`，只用于本次日志排障，并通过 `@JsonIgnore` 避免进入缓存或序列化输出。
- 原始响应 JSON 解析失败、列表 DTO 转换失败、对象 DTO 转换失败都会在同一条错误日志里打印 `method/path`、脱敏原始响应和完整堆栈。
### 2026-08-12 物价查询物品别名解析预留

目标：

- 物价类查询支持后续通过配置别名匹配到 JX3API 原始物品名。

涉及文件：

- `src/main/java/com/grafie/botjava/jx3/http/action/base/Jx3BaseAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordsAction.java`

调整内容：

- 新增 `resolveTradeItemNameAlias(itemName)` 作为物价查询别名解析入口。
- 当前未接入配置，直接返回传入物品名；后续可在该入口接数据库或群配置别名。
- `物品价格` 和 `黑市物价` 的请求参数与图片模板展示名都先经过该入口。

### 2026-08-12 物品价格返回结构修正

目标：

- 适配 `trade.item.records` 当前 `data.list` 一维成交记录数组，避免成功响应在 DTO 转换阶段失败。

涉及文件：

- `src/main/java/com/grafie/botjava/jx3/http/data/trade/record/TradeRecordData.java`
- `src/test/java/com/grafie/botjava/jx3/http/Jx3ApiHttpContractTest.java`
- `docs/testing/jx3api-http-check.json`

调整内容：

- `TradeRecordData.data/list` 从 `List<List<SaleData>>` 调整为 `List<SaleData>`。
- 黑市物价 `trade.records` 继续使用 `OfficialQueryData.TradeRecords.list` 的二维结构，两类接口分开建模。
- Vue 模板 `物品价格.html` 已通过扁平化逻辑兼容一维和二维记录数组。
### 2026-08-12 QQ WebSocket Gateway 限流退避

目标：

- 避免 WebSocket 启动或重连时频繁调用 QQ `/gateway/bot`，触发业务码 `100017` 的接口频率限制。

涉及文件：

- `src/main/java/com/grafie/botjava/qq/QqOpenApiErrorMapper.java`
- `src/main/java/com/grafie/botjava/service/QqGatewayClient.java`
- `src/main/java/com/grafie/botjava/service/QqWebSocketGatewayService.java`
- `src/test/java/com/grafie/botjava/qq/QqOpenApiErrorMapperTest.java`
- `src/test/java/com/grafie/botjava/service/QqGatewayClientTest.java`

调整内容：

- QQ OpenAPI HTTP 400 且响应 `code=100017` 时归类为 `RATE_LIMIT`，并标记为可重试错误。
- `QqGatewayClient` 对 `/gateway` 和 `/gateway/bot` 响应做 10 分钟本地缓存，缓存有效期内不重复请求网关地址。
- WebSocket 连接前获取 gateway 失败且分类为限流时，重连退避固定为 60 秒，避免短周期重连继续打满 QQ 限流。

### 2026-08-12 外观名称别名配置

目标：

- 支持群内维护物价查询使用的外观名称别名，让用户可用常用简称查询 JX3API 原始物品名。

涉及文件：

- `src/main/java/com/grafie/botjava/entity/AppearanceNameAlias.java`
- `src/main/java/com/grafie/botjava/mapper/AppearanceNameAliasMapper.java`
- `src/main/java/com/grafie/botjava/service/AppearanceNameAliasService.java`
- `src/main/java/com/grafie/botjava/service/AppearanceNamePermissionConfiguration.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/AppearanceNameAliasAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordsAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/util/REGEX.java`
- `docs/database/appearance-name-alias.sql`

调整内容：

- 新增 `外观名称追加 原始名 别名1 别名2`、`外观名称列表`、`外观名称查询 名字`、`外观名称待审核列表`、`外观名称审核 名字 [别名]`、`外观名称删除 名字 [别名]` 六类指令，支持 `/指令` 前缀；追加指令中别名按空格、逗号、顿号和分号拆分。
- 数据按 `group_open_id` 隔离；同一个群内 `normalized_alias_name` 唯一，不同群可以维护不同映射。
- 名称规范化会忽略空白、`·`、点号、横线、括号等分隔符，因此 `金发因陀罗` 可以匹配 `金发·因陀罗`。
- `外观名称追加` 对群员公开，但新增别名默认写入 `PENDING`；`外观名称列表`、`外观名称查询`、`物价` 和 `黑市物价` 只读取 `APPROVED` 别名，未审核内容不会被查询到。
- `外观名称待审核列表`、`外观名称审核 名字 [别名]` 和 `外观名称删除 名字 [别名]` 不使用群主/群管理员权限，当前仅 `bot.qq.admin.master-openids` 配置的 QQ 主号或 `GroupCommandPermissionConfiguration` 数据库授权成员可操作。
- `物价` 和 `黑市物价` 请求 JX3API 前先调用当前群的已审核别名解析，未命中时保持原始输入。
- 权限前置预留为 `AppearanceNamePermissionConfiguration`；当前默认实现对新增/列表/查询公开，对待审核/审核/删除先匹配 QQ 主号 openid，再读取 `GroupCommandPermissionConfiguration` 的数据库授权结果，群主和群管理员不会天然获得外观名称审核权限。
### 2026-08-12 QQ Gateway Client 构造注入修正

目标：

- 修复 Spring 启动时 `QqGatewayClient` 因存在业务构造器和测试构造器而回退查找无参构造器的问题。

涉及文件：

- `src/main/java/com/grafie/botjava/service/QqGatewayClient.java`

调整内容：

- 在 `QqGatewayClient(QqOpenApiClient openApiClient)` 上显式标记 `@Autowired`。
- 保留包内可见的 `Clock` 测试构造器，用于网关缓存测试。
- 启动相关测试已覆盖该 bean 构造路径。
### 2026-08-12 物品价格分组返回与长图模板适配

目标：

- 适配 JX3API `/trade/item/records` 当前按业务分组返回的 `data.list` 结构，修复物价图片无价格记录的问题。
- 让 JX3API 原始响应日志在不泄露 token/ticket 的前提下完整可追踪，避免长响应被截断后排障困难。

涉及文件：

- `src/main/java/com/grafie/botjava/jx3/http/data/trade/record/TradeRecordData.java`
- `src/main/java/com/grafie/botjava/jx3/http/data/trade/record/SaleData.java`
- `src/main/java/com/grafie/botjava/jx3/http/data/official/OfficialQueryData.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/action/TradeRecordsAction.java`
- `src/main/java/com/grafie/botjava/jx3/http/util/Jx3RequestUtil.java`
- `src/main/java/com/grafie/botjava/util/SensitiveDataUtil.java`
- `src/main/resources/static/物品价格.html`
- `src/test/java/com/grafie/botjava/jx3/http/Jx3ApiHttpContractTest.java`
- `src/test/java/com/grafie/botjava/util/HtmlToImageUtlTest.java`

调整内容：

- `TradeRecordData` 支持新的 `list: [{name, list:[...]}]` 分组结构，保留 `groups` 给模板渲染，同时把有效价格记录扁平化到 `data` 兼容旧处理。
- `SaleData.index` 和黑市物价 `TradeListing.index` 改为 `String`，兼容 JX3API 返回的十六进制/字符串编号。
- `TradeRecordData.value` 改为 `String`，兼容旧文档里物品原价可能返回 `"280.00"`，新接口优先读取 `retail`。
- `物品价格.html` 改为外观预览、分组价格卡片和历史趋势长图布局，优先展示 `公示期`、`在售期`、当前服务器、电信区、双线区、无界区；趋势图优先使用当前服务器分组，没有时回退全部记录。
- `TradeRecordAction` 和 `TradeRecordsAction` 会把远程预览图转成 `previewImageDataUri` 后再交给 HTML 模板，避免截图浏览器直接访问外部图片导致渲染不稳定。
- `Jx3RequestUtil` 先接收原始响应字符串并记录；响应过长时按 3000 字符分段打印同一次请求的响应内容，反序列化失败时也带上脱敏后的原始响应和堆栈。
- `SensitiveDataUtil` 同时兼容自由文本 `token=...`、`ticket:...` 和 JSON `"token":"..."` 的脱敏格式。
- 新增 `JX3API_TRADE_ITEM_RECORDS_GROUPED_LIST_JSON` 契约测试和 `物品价格-金发因陀罗.png` 渲染预览测试，预览文件输出到 `target/test-output/html-image/物品价格-金发因陀罗.png`。

验证记录：

- 已通过 `mvn -Dtest=HtmlToImageUtlTest#shouldRenderTradeRecordPreviewWithGroupedJx3ApiData test`。
- 已通过 `mvn -Dtest=SensitiveDataUtilTest,CommandInvocationRecorderTest test`。
- 当前工作区没有配置 `JX3API_API_TOKEN`、`JX3API_API_V2_TOKEN`、`JX3API_TICKET`、`JX3API_WS_TOKEN` 环境变量，未直接调用真实 JX3API token；本次以用户日志中的真实返回结构和文档索引信息做契约适配，含 ticket 的接口继续跳过。
- 物品价格接口返回的 `data.view` 是外观预览图地址。图片模板按 `previewImageDataUri -> data.imageDataUri -> data.view` 的顺序取图；服务端优先下载 `data.view` 并转换为 data URI，下载失败时模板仍可直接使用原始 URL。
### 2026-08-12 JX3API 外观别名自动沉淀

- /trade/item/records 与黑市物价接口成功返回后，读取 data.name 作为正式名称，读取 data.alias 作为别名集合。
- data.alias 支持按 /、／、空格、逗号、顿号和分号拆分，例如 猴金/金发因陀罗 会保存为两个独立别名。
- JX3API 返回的数据视为可信来源，自动写入 appearance_name_alias 并标记为 APPROVED，创建者和审核者标记为 JX3API。
- 外观名称配置使用 __GLOBAL__ 数据库作用域，不再按 QQ 群隔离；任意群查询时都可复用已审核别名。
- 自动导入不会覆盖已指向其他正式名称的同名别名，冲突项只记录日志，原映射保持不变。人工追加当前通过审核策略直接进入 APPROVED；审核状态、待审核列表、审核指令和删除权限入口仍保留。
- 物品价格模板的预览图映射为 data.view，页脚品牌文字统一为“小猫饼”。
### 2026-08-12 外观名称临时免审核

- AppearanceNamePermissionConfiguration 保留 isAppearanceNameReviewPassed 审核策略方法，当前实现固定返回 true。
- 人工执行“外观名称追加”后直接写入 APPROVED 并立即参与别名查询，不再产生新的待审核记录。
- 返回文案根据审核策略动态显示“已新增”或“已提交审核”，未来恢复审核时无需修改指令层。
- 待审核列表、审核指令、PENDING 状态及其管理权限均保留；删除权限没有放宽。
### 2026-08-12 PostgreSQL + MongoDB 双数据库基础设施

- PostgreSQL/JPA 继续作为关系型权威数据源，MongoDB 作为可选的第二数据库同时运行，不做数据源切换。
- 增加 bot.mongodb.enabled/uri/database/max-document-bytes 配置；默认关闭，开启时 URI 必填且必须使用 MongoDB 协议。
- 排除 Spring Boot Mongo 自动配置，改为显式创建 botMongoClient 与 botMongoTemplate，避免未启用时自动连接 localhost。
- 增加 MongoDocumentStore，提供结构化 JSON object 的原子单文档 upsert、find、exists 和 delete，并校验集合名、文档 ID 与 payload 大小。
- MongoDB 不接管现有 JPA Repository，也不注册 Mongo 事务管理器；跨 PostgreSQL/MongoDB 写入需要由具体业务通过 outbox、幂等和补偿实现。
- 部署和开发说明见 docs/MONGODB.md。

### 2026-08-12 Lua 角色状态绑定边界修正

- Lua 维护的 MongoDB `roles` 类平铺集合按外部既有结构接入；Java 不改变索引、不新增或删除角色文档，只更新 PostgreSQL 白名单中明确标记为可写的顶层字段。
- QQ 用户绑定只接受“区服 + 角色名”，不要求账号、MongoDB `_id`、`全局ID` 等用户无法获知的内部标识。
- PostgreSQL 保存 `member_openid + 区服 + 角色名` 关系；后续状态查询按该业务键读取 MongoDB 全部精确匹配记录，不依赖唯一索引。
- 兼容同一区服、同一角色名命中多条 MongoDB 文档：0 条提示无状态，1 条正常展示，多条按记录分块展示，禁止默认选择第一条。
- 多结果默认不展示账号、`_id` 和 `全局ID`；读取、渲染与更新设置固定上限，超过上限时明确拒绝并记录脱敏告警。

### 2026-08-12 脚本状态查询与受控字段更新

- `绑定角色`、`添加角色` 调整为“区服 + 角色名必填，门派可选”，旧的带门派格式继续兼容；修改门派仍使用 `修改角色 区服 角色名 门派`。
- 新增 `脚本状态 [区服 角色名]`，省略参数时使用个人默认角色；只允许查询当前消息 `member_openid` 已绑定的区服角色。
- 新增 `脚本设置 [区服 角色名] 字段 值`，只允许修改当前成员已绑定角色且管理员标记为可写的字段。
- 新增 `LuaRoleStatusStore`，按“服务器字段 + 角色名字段”精确读取全部 MongoDB 文档；多条结果全部渲染，更新时按候选 `_id` 全部更新，不使用 `first()`。
- 新增 PostgreSQL `script_status_field` 表，保存 Mongo 字段、显示名、分组、类型、读写属性和排序；内部标识及凭证字段禁止配置。
- 主号 C2C 增加 `脚本字段列表/设置/删除`，字段配置和群内脚本更新继续写入 `bot_admin_audit_log`，不记录字段值或消息原文。
- 新增 Vue 模板 `脚本状态.html`，稳定数据为 `{server, roleName, matchCount, records:[{index, sections:[{name, values:[{name,value}]}]}]}`。
- QQ 官方唯一身份机制确认：相同 bot 在不同群为同一用户分配不同 `member_openid`，C2C 使用独立 `user_openid`；当前个人绑定按群隔离，不能自动跨群或私聊关联。`author.member_role=owner` 仅代表群主，不代表机器人拥有者。
### 2026-08-12 群主动推送任务与 WS 事件去重

- 主动推送明确拆分为 `WS_EVENT` 实时事件和 `SCHEDULED` 自定义定时任务，删除原 `WsDataPushService` 的星期过滤；所有 JX3API WS 事件收到后立即进入统一推送链路。
- 新增 `PushTaskRegistry`，为 39 个 JX3API WS 事件注册独立任务，并预留 `MONGO_DAILY_PROGRESS`（Mongo日常进度）定时推送任务。
- 新增群管理员指令 `推送列表`、`开启推送 任务名`、`关闭推送 任务名`；订阅按群和任务持久化，不存在记录时默认关闭，修改写入 `GROUP_PUSH` 审计日志。
- 新增 `GroupPushDispatcher` 有界线程池统一出口；WS 收包线程和未来定时任务均不直接调用 QQ，最终继续复用 `GroupMessageSender.sendActive` 的本地总开关、平台授权和共享频控。
- WS 事件 JSON 递归按字段名排序后计算 SHA-256 指纹，PostgreSQL `push_event_receipt` 使用 `task_code + event_fingerprint` 唯一键原子去重；重复帧、应用重启和多实例同时消费均不会重复推送。默认保留 7 天，定时清理。
- 新增 `ScheduledGroupPushTask` 扩展点，后续 Mongo 日常任务只负责按群拼装 `BotResponse`，订阅和投递由模板统一处理。
- Mongo 状态白名单额外保护实际配置的服务器字段和角色名字段，避免用户更新文档定位键。
- 数据库脚本：`docs/database/group-push-subscription.sql`、`docs/database/push-event-receipt.sql`。
- 聚焦测试 22 项通过，覆盖任务注册、默认关闭、群隔离、WS 重复事件拦截、投递目标和 Mongo 定位字段保护。
- 最终完整回归 mvn test 共 582 项通过，失败 0、错误 0；包含 17 项 Playwright HTML/Vue 图片渲染测试。

## 2026-08-13 群日常进度、全局外部限速与 QQ WS 稳定性

- 导入并核验 `D:/Download/roles.js`：384 条角色、13 个区服、317 个不同角色名、6 组重复“服务器 + 角色名”，单组最多 56 条；补充真实字段与时间格式回归样本。
- 所有外部 HTTP、MinIO、远程图片、富媒体分片和 WS 握手统一限制为每秒最多 2 次。
- 角色绑定加入群级隔离键；新增每天 10:00 的 `Mongo日常进度` 图片任务，无订阅、无绑定或无 Mongo 记录均不发送。
- 新增可配置推送字段及数据库授权 `DAILY_PUSH_FIELD_CONFIG`，配置操作写入现有管理员审计日志。
- 新增 Vue 模板 `日常进度.html`，采用物价页面的白底、青绿与粉色配色，使用动态表格且不显示外观图片。
- 修复 QQ WebSocket 旧 session 回调干扰新连接导致的 4902 Resume 重连风暴；新增 generation 隔离和重连合并。
- 数据库脚本：`docs/database/user-info.sql`、`docs/database/group-command-permission-grant.sql`、`docs/database/group-daily-push-field.sql`。


### 2026-08-13 最终稳定性与安全加固

- QQ WebSocket 重连增加 connection generation，旧 session 的关闭、传输错误、心跳与握手回调不能影响新连接；重复重连合并为一个任务，停机时作废代际并取消待执行任务，`RESUMED` 作为正常恢复确认。
- 日常推送字段仅添加/删除需要 PostgreSQL `DAILY_PUSH_FIELD_CONFIG` 授权，字段列表允许公开查看；权限主体在 `member_openid` 缺失时回退到 `author.id`。
- QQ Hook/Webhook 增加默认 300 秒签名时间窗和 1 MiB 请求体上限，分别由 `BOT_QQ_WEBHOOK_MAX_SIGNATURE_AGE_SECONDS`、`BOT_QQ_WEBHOOK_MAX_BODY_BYTES` 配置。
- HTML 模板数据改为 Base64 UTF-8 JSON 注入，阻断模板 Chromium 对公网 HTTP/HTTPS 的访问；图片上传完成后清理临时 PNG。
- 三轮人工安全检查覆盖入站鉴权、权限/群隔离、SQL/MongoDB、外部网络、HTML 渲染、文件与反序列化边界，确认并修复防重放、模板脚本逃逸、未认证请求体资源消耗和临时文件积累问题。
- Codex Security 标准扫描插件启动时，其自带 Python 运行时因 `_sqlite3` DLL 加载失败而未创建扫描任务；准确阻塞信息记录在 `docs/SECURITY_AUDIT_2026-08-13.md`。
- 最终 `mvn test`：596 项测试通过，失败 0、错误 0、跳过 0；UTF-8 校验覆盖 709 个文件，`git diff --check` 通过。


### 2026-08-13 权限配置 Bean 唯一性修复

- 修复 `DatabaseGroupCommandPermissionConfiguration` 与 `NoopGroupCommandPermissionConfiguration` 同时注册为 Spring Bean 导致 `GroupCommandExecutionService` 启动注入失败。
- 数据库权限实现是生产环境唯一组件；Noop 保留为隔离测试或非 Spring 场景的手动 fallback，不参与组件扫描。
- PostgreSQL/JPA 与 MongoDB/MongoTemplate 的双数据库架构保持不变；本问题属于业务策略 Bean 冲突，不引入面向多 JDBC 数据源路由的动态数据源依赖。
- 新增 Spring classpath 候选扫描回归测试，保证 `GroupCommandPermissionConfiguration` 在生产包中只有一个可注入实现。
### 2026-08-13 WSL MongoDB 与双数据库实测

- 在本机 Ubuntu WSL2 中部署官方 `mongo:8.0` 容器 `botjava-mongodb`，使用 `botjava-mongodb-data` 持久化卷和 `unless-stopped` 重启策略；端口仅绑定 `127.0.0.1:27017`。
- 将 `D:/Download/roles.js` 导入 `toy2.roles`，实测 384 条记录、13 个区服、317 个不同角色名；按“服务器 + 角色名”存在 6 组重复键，单组最多 56 条。
- 新增环境变量门控的 `DualDatabaseLiveSmokeIT`：在同一 Spring 上下文中执行 PostgreSQL `select 1`、MongoDB `ping`、集合数量断言和 `LuaRoleStatusStore` 多结果查询。
- 修复启用 MongoDB 后 `BotMongoProperties` 被组件扫描和配置属性机制重复注册的问题；配置属性改为由应用入口全局注册一次，MongoClient/MongoTemplate 仍只在 `bot.mongodb.enabled=true` 时创建。
- 修复 Noop 权限 fallback 被错误注册为生产 Bean、`jx3api.enabled=false` 时 `Jx3RequestUtil` 未随 HTTP 模块关闭的问题。
- 移除应用入口中与 `@SpringBootApplication` 重复的显式 `@ComponentScan`，恢复 `@DataJpaTest` 切片隔离；三个 Mapper 测试不再自建重复 JPA 启动配置。
- 最终 JAR 使用本机 PostgreSQL 与 WSL MongoDB 启动成功，Actuator 健康状态为 `UP`；全量 `mvn test` 共 597 项通过，失败 0、错误 0、跳过 0。
### 2026-08-13 JX3API WebSocket 防重连风暴

- `jx3api.enabled=true` 且 `jx3api.ws.enabled=true` 时继续接入独立的 JX3API 游戏事件 WebSocket，使用 `jx3api.ws.ws-url` 与专用 `jx3api.ws.ws-token`，不与 QQ WS token 混用。
- 新增 `jx3api.ws.re-connect-delay-seconds` / `JX3API_WS_RECONNECT_DELAY_SECONDS`，默认 30 秒，启动校验禁止配置为小于 30 秒。
- 移除无效的自调用 `@Async` 和无等待 while 重试，改为单线程定时调度；首次立即连接一次，失败或断线后的重试至少间隔 30 秒。
- 断线、传输异常、Ping 失败和连接未就绪统一进入同一调度入口；原子标记合并重复回调，同一时间最多存在一个待执行连接任务。
- 连接成功后重置重试计数；每次建立连接前清理旧连接状态，每次断线清理旧 Ping 线程，应用停机时取消重连和 Ping 任务。
- 新增聚焦测试，覆盖 29 秒配置拒绝、30 秒调度和连续重连请求合并。
- 最终全量 `mvn test` 共 599 项通过，失败 0、错误 0、跳过 0。
- 补齐连接调度、握手开始、连接确认、远端断开、传输异常、Ping 失败和应用主动停止日志；断开日志包含 closeCode、reason 与 nextRetrySeconds，所有日志均不输出 WS token。
### 2026-08-14 QQ 群消息内容排障日志

- `GroupCommandExecutionService` 在正则匹配前记录收到的群消息内容；未命中任何处理器时，未匹配日志同时携带同一份内容，继续复用外层 QQ WS 创建的 MDC `invocationId`。
- 消息内容先转为单行，连续空白和控制字符压缩为空格，再通过 `SensitiveDataUtil` 脱敏；日志上限为 1000 个字符，超长内容明确标记原长度。
- 遵循现有日志隐私约束，不在新增日志中打印群 openid、成员 openid 或消息 id。
- 新增日志回归测试，覆盖收到消息、未匹配消息、换行压平以及 `token` 字段脱敏。
- 最终全量 `mvn test` 共 600 项通过，失败 0、错误 0、跳过 0；UTF-8 校验覆盖 711 个文件，`git diff --check` 通过。
### 2026-08-14 JX3API WebSocket 启停状态可观测性

- 确认本地外置 `config/application.yml` 原先配置为 `jx3api.ws.enabled=false` 且 WS URL 为空，因此 JX3API WS 自动配置没有加载，日志完全静默；日志中已有的连接信息均来自 QQ WS。
- 新增常驻 `Jx3ApiWebSocketStatusReporter`，应用启动完成后始终输出 JX3API WS 的有效开关状态；关闭时给出具体配置原因，开启时只记录 endpoint 主机、token 是否已配置和重连间隔。
- 状态摘要禁止输出 WS token、URL 路径及查询参数；原连接调度、握手、连接成功、断开和退避重连日志继续由 WS 连接组件输出。
- 被 Git 忽略的本地外置配置已启用 JX3API WS，endpoint 设置为 `wss://socket.nicemoe.cn`，重连间隔保持 30 秒；仓库默认仍为安全的关闭状态，部署环境必须显式启用。
- 新增两项聚焦测试，覆盖关闭原因、开启摘要、endpoint 主机提取以及 token/URL 查询参数不泄露。
- 最终全量 `mvn test` 共 602 项通过，失败 0、错误 0、跳过 0。
### 2026-08-14 JX3API WS detail 消息兼容

- 根据真实 `action=2004` 八卦速报帧确认，线上事件正文位于根节点 `detail`，旧解析器只读取 `data`，导致 Jackson 返回空 DTO 后调用 `setAction` 触发 `NullPointerException`。
- `WsActionHandler` 现在依次兼容 `data` 与 `detail`；正文缺失或转换结果为空时记录 action 并安全忽略，不再影响后续 WS 收包。
- `WsDataAction2004` 补充线上实际字段 `tags` 与 `tieba`；推送字段映射为“分类”和“贴吧”，继续隐藏外部 URL。
- 使用线上同结构 payload 增加解析回归测试，验证 action、分类、区服、服务器、贴吧、标题、日期和 SHA-256 去重指纹；另增加缺失正文以及最终 QQ 推送文字测试。
- 最终全量 `mvn test` 共 605 项通过，失败 0、错误 0、跳过 0。

### 2026-08-14 万宝楼编号搜索

- 接入官方 LV.2 `GET /trade/wanbaolou`；群指令支持 `编号搜索 角色编号` 和 `万宝楼 角色编号`，请求 query 使用 `id`，不传上游内部字段 `zhanghaoId`。
- `MethodEnum` 增加 HTTP method 元数据，历史接口默认 POST；`Jx3BaseAction` 统一按枚举选择 GET query 或 POST JSON，继续复用 token 选择、每秒 2 次全局限流、缓存、脱敏和分段响应日志。
- 使用示例编号 `1405435120446099456` 和本地 LV.2 token 实测：POST 会返回成功但字段为空，按官方 OpenAPI 改为 GET 后获得完整账号详情。
- 新增类型化 `WanbaolouData` 与独立 `WanbaolouAction`；`replyContent` 按 `<br>` 与 `【标签】` 转换为有序明细，未带标签的行并入上一项，模板不使用 `v-html`。
- 新增 Vue 模板 `万宝楼.html`，展示区服、交易状态、时间、角色编号、角色等级、门派体型、阵营、价格、装分、资历、关注、奇遇和账号收集详情。
- HTTP 机器契约增加真实脱敏样例，指令覆盖矩阵同步 `Wanbaolou -> DATA_TRADE_WANBAOLOU`；专测覆盖 GET 参数、LV.2、明细解析和 Playwright 截图。
### 2026-08-14 JX3API 官方 HTTP 全量覆盖

- 重新下载并解析官网 `https://www.jx3api.com/openapi`：当前共有 78 条唯一路径；逐项对比 `MethodEnum` 后补齐 18 条缺失路径，并为先前只有枚举定义的 `/role/achievement` 补上 Action 与群指令。
- 新增独立 Action 与指令：成就查询、名片预设、角色聊天、奇遇攻略、跨服名剑、武林争霸、捕快荣誉、江湖浪客、决斗挑战、答案之书、分类语录、喝什么、吃什么、渣男语录、配装搜索、急速计算、成本计算和资历分布。
- `MethodEnum` 支持按接口声明 GET/POST；新增官方扩展接口使用 GET query。`Jx3BaseAction` 仍统一负责 token 等级选择、限流、原始响应日志、反序列化和错误返回。
- 修复 `REGEX.handleEncounter` 固定参数名单造成新命名组静默丢失的问题；改为从表达式自动发现命名组，已验证 `page`、`camp`、`source`、`category` 和 `subclass` 能完整进入 Action。
- HTTP 检查文档现有 80 个可执行 contract，覆盖官方 78 条唯一路径、活动日历的两种契约以及旧版搜索区服；元测试改为按唯一路径强制官方清单零遗漏。
- live smoke runner 增加 GET 结果记录，并独立读取 `JX3API_API_V2_TOKEN`（未配置时回退普通 token）；79 个非语音 contract 可由同一 runner 顺序验收。
- 使用本地凭证对无需 ticket 的新增接口做低频抽测，请求间隔至少 750ms；需要 ticket 的接口仅完成离线参数和返回契约，保留为待真实环境验收项。
- 最终全量 `mvn test` 共 646 项通过，失败 0、错误 0、跳过 0；其中 19 项为 HTML/Vue 浏览器渲染测试。
### 2026-08-14 万宝楼可空反序列化与图片降级

- 根据线上响应修复 `WanbaolouData.updatePrices`：上游实际为价格变更对象数组，不再错误声明为 `List<Integer>`；内部字段全部使用包装类型并允许缺失、`null` 与未知扩展字段。
- 新增统一 `JacksonConfiguration`，Spring ObjectMapper 与 `ObjectMapperUtil` 同步接受缺字段、未知字段、null creator/primitive、空字符串/空数组和单值数组；序列化默认忽略 null，空 Bean 不再直接失败。
- 宽松配置只处理“可选值”，不会把对象强行转换成整数等不兼容结构；此类上游 schema 变化仍必须修正 DTO，并保留原始响应和完整异常日志。
- 修复物品价格模板在预览图内嵌失败后回退访问原始外链的问题；浏览器不再直接请求外部 `view` URL，预览图不可用时显示占位，价格表与走势图继续生成。
- HTML 资源校验异常现在携带脱敏后的失败图片/样式地址；data URI 只显示媒体类型，不输出完整内容。
### 2026-08-14 搜索物品改为逐行文本

- `TradeItemSearchAction` 显式声明 `TEXT`，不再生成 `搜索物品.html` 图片响应；请求路径、token、参数匹配、限流和错误映射保持不变。
- 搜索结果最多展示 8 条，每件物品独占一行，只输出序号、名称和别名；分类、参考值、日期和说明不再进入回复内容。
- 上游 `view` 图片地址不再下载、转换或发送，避免搜索列表进入 HTML 渲染和 QQ 富媒体上传链路。
- 同步调整 Action 返回类型、文本内容和图片响应清单测试；原 `搜索物品.html` 暂时保留为可复用历史模板，但指令运行时不再引用。
- 最终全量 `mvn test` 共 650 项通过，失败 0、错误 0、跳过 0；其中 HTML/Vue 浏览器渲染测试 21 项。
### 2026-08-14 角色聊天真实列表适配

- 根据线上响应将 `/chat/records` 根 DTO 从通用 `FlexibleOfficialData` 改为专用 `ChatRecordsData`，稳定承接 `total` 和 `list`；列表项支持区服、服务器、角色名、角色编号、全局编号、频道、聊天内容及时间。
- `ChatRecordsAction` 显式返回 `IMAGE`，保留现有 LV.2 token、GET query、每秒 2 次限流和页码参数；单张图片最多展示当前页前 20 条，标题区同时展示本页数量和官方总数。
- 新增 Vue 模板 `角色聊天.html`，每行展示角色及区服、频道、聊天内容和北京时间；重复聊天记录按上游顺序保留，不做去重。
- `roleId` 与 `globalId` 只用于兼容反序列化，不进入稳定视图或 Vue 数据；外层响应时间同样不展示。
- HTTP 契约样例更新为线上真实字段，并新增真实 JSON 反序列化、GET 参数、图片返回、字段隔离和浏览器渲染测试。
- 最终全量 `mvn test` 共 653 项通过，失败 0、错误 0、跳过 0；其中 HTML/Vue 浏览器渲染测试 22 项。20 行角色聊天截图尺寸为 `1280×2182`，完整覆盖列表中下部。
### 2026-08-14 群 WS 推送开关表格与 QQ/JX3API 来源拆分

- `推送列表` 从纯文本改为 `推送列表.html` Vue 表格图片，按来源、分类、内容和本群状态列出任务注册表的全部项目；数据库没有记录时明确显示“未开启”。
- 推送来源从笼统的 `WS_EVENT` 拆为 `QQ_WS`、`JX3API_WS` 和 `SCHEDULED`。当前注册 QQ 的 4 类群生命周期/主动消息授权事件、JX3API 的全部 39 类实时事件及 Mongo 日常定时任务。
- 新增 `QqGroupLifecyclePushHandler`：QQ WS 群状态事件只进入事件所属群的投递路径；队列内再次读取 `group_open_id + task_code`，关闭时不占用去重记录、不调用 QQ。
- JX3API WS 继续通过 `findEnabledGroupOpenIds(task)` 只投递到开启对应任务的群。普通 QQ 群消息保持指令入口语义，不注册为推送任务，避免回显循环。
- QQ 与 JX3API WS 事件继续共用 SHA-256 持久化去重、有界线程池、主动消息策略和 QQ 发送器。群任务开关之外仍需满足本群主动消息总开关及 QQ 平台授权。
- 新增任务注册、Action 图片数据、群隔离、QQ WS 开关投递和 44 项长表 Playwright 渲染测试；完整表格输出尺寸为 `1280 × 2380`。
- 最终全量 `mvn test` 共 659 项通过，失败 0、错误 0、跳过 0；其中 HTML/Vue 浏览器渲染测试 23 项。
### 2026-08-16 状态检查与 JX3API WS 监控

- 新增独立系统指令 `状态检查`，同时兼容 `/状态检查`；与游戏服务器的 `开服/开服状态` 指令完全分离。
- 指令归入“群配置”分组，仅群主和群管理员可执行，返回 `/actuator/health`、`/actuator/prometheus` 的应用内可用状态与 JX3API WS 当前连接状态。
- 新增 `Jx3ApiWebSocketStatus` 作为脱敏状态快照；关闭 JX3API 或 WS 时显示未启用，启用后区分已连接、初始化中和已断开等待重连。
- 新增 Actuator `jx3ApiWebSocket` 健康项：显式启用但未连接时为 `DOWN`，配置关闭不影响应用整体健康。
- 新增 Prometheus Gauge `bot_jx3_websocket_connected`，连接为 1，断开或关闭为 0；状态指令、健康端点与指标共用同一状态来源。
- 状态读取不发起自 HTTP 或公网请求，不返回 token、WS URL、数据库连接或用户标识。
- 最终全量 `mvn test` 共 665 项通过，失败 0、错误 0、跳过 0；其中 HTML/Vue 浏览器渲染测试 23 项。
### 2026-08-19 活动日历数组字段适配

- 根据 `/active/calendar` 真实响应修复 `ActiveCurrentData.draw`：上游由字符串变为字符串数组，本地改为 `List<String>`，并继续接受历史单值响应。
- 上游宠物奇缘字段现为 `lucky`，DTO 同时兼容历史字段 `luck`，统一进入模板的 `luck` 展示列表。
- 新增 `weekly.conn` 与 `weekly.raid` 类型化 DTO；活动日历图片分别以“公共任务”和“团队秘境”前缀合并展示，同时保留旧 `team` 数据兼容。
- 活动日历 Vue 模板改用 `draws` 数组，以顿号连接多项美人图内容；空数组继续展示“今日暂无”。
- HTTP 契约和 Action 测试使用 2026-08-19 真实结构，覆盖 5 项美人图、3 项宠物奇缘、世界首领及 5 项周常任务；Playwright 样例同步使用新字段。
- 最终全量 `mvn test` 共 666 项通过，失败 0、错误 0、跳过 0；其中 HTML/Vue 浏览器渲染测试 23 项。
