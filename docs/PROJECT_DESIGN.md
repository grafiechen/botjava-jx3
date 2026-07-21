# botjava-jx3 变更历史

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
