# botjava-jx3 设计文档

## 1. 项目定位

`botjava-jx3` 是一个基于 Spring Boot 的 QQ 机器人项目，当前主要面向剑网 3 群聊场景。

项目的核心目标是：

- 接收腾讯机器人开放平台推送的群消息事件。
- 根据群消息内容识别剑网 3 查询指令。
- 调用 JX3API 或项目内部逻辑获取数据。
- 根据业务需要返回纯文本消息或基于 HTML/Vue 模板渲染后的图片消息。

当前项目不以频道能力为主要目标，优先围绕群聊 webhook、群消息回复、群富媒体图片发送和 JX3 查询能力演进。

## 2. Feature 与未实现清单

### 2.1 已具备基础能力

- QQ 群聊 webhook 消息接收。
- QQ 回调校验。
- `GROUP_AT_MESSAGE_CREATE` 与 `GROUP_MESSAGE_CREATE` 统一进入群消息处理链路。
- `GROUP_ADD_ROBOT`、`GROUP_DEL_ROBOT`、`GROUP_MSG_RECEIVE` 和 `GROUP_MSG_REJECT` 统一进入类型化群生命周期处理链路。
- 基于 `REGEX` 的 JX3 指令识别。
- 基于 `MethodEnum` 的 JX3API HTTP 接口映射。
- 官方文档 61 个 HTTP 接口的离线反序列化契约测试。
- JX3API WebSocket 事件接入基础结构。
- 文本消息回复。
- 群聊富媒体图片消息基础链路。
- HTML/Vue 模板截图能力。
- MinIO 图片上传能力。
- 群默认服务器查询和保存基础能力。
- 业务返回 `BotResponse` 与 QQ 消息发送 `GroupMessageSender` 分层。
- 当前 `REGEX` 注册的 79 条指令均显式接管业务返回，不再以 `null` 结束已匹配指令。
- `Jx3CommandRegistry` 统一完成指令规范化、正则匹配、处理器定位和启动完整性校验。
- 指令定义具备展示名称、分类、用途和示例元数据，`帮助`、`菜单`、`查询帮助` 自动生成分组菜单。
- 支持 `/指令`、直接指令和显式别名，例如 `开服`、`开服状态` 指向同一处理器。
- 指令定义具备访问级别；当前绑定区服仅允许群主或管理员执行，查询指令默认公开。
- `QqGroupMessageClient` 统一封装群消息发送与图片上传路径。
- `PayloadActionRegistry` 统一管理 QQ dispatch 事件处理器，业务代码不使用静态 Spring Bean 查找。
- 已具备离线 webhook 路由、指令注册、JX3API contract、QQ 消息拼装和 Vue 截图测试。
- QQ/JX3 必需配置启动校验、敏感日志字段脱敏和数据库密码环境变量配置。
- QQ、数据库和阿里语音凭证均使用环境变量引用，应用配置文件不保存明文凭证。
- 提供部署指南，覆盖 Java 21、Docker 镜像构建、PostgreSQL、Playwright Chromium、MinIO 自有域名、QQ webhook 和发布检查。
- 提供 GitHub Actions CI，在 Ubuntu 22.04 + Java 21 上执行 UTF-8 校验、安装 Chromium 并运行完整 Maven 测试。
- JX3API 选择性数据库共享缓存，按接口设置 30 秒至 5 分钟 TTL，token 不进入缓存键，实时角色接口不缓存。
- 提供 UTF-8 群消息 payload 样例与本地 webhook 回放脚本。
- 群维度查询总开关和实验功能开关持久化；系统指令始终可用，实验指令默认关闭。
- 群管理员可按指令展示名设置群维度单项开关；关闭结果同时作用于执行策略和帮助菜单，系统指令不允许关闭。
- 群消息指令通过数据库按“群 openid + 指令”执行跨实例响应冷却，内部指令默认 5 秒，外部调用默认 30 秒。
- 群默认区服与个人角色绑定分表持久化；绑定区服按 `group_openid` 整群共享，角色等个人绑定按 `member_openid` 归属单独账号。
- 每个 QQ 账号可独立绑定默认门派；解绑角色只清理角色字段，不会误删门派偏好。
- 群指令调用记录以 best-effort 方式持久化，覆盖权限、策略、参数、冷却、执行和发送结果，不保存原始聊天内容与请求参数。
- 群指令调用记录默认保留 30 天并定时清理；群管理员可用 `群统计` 查看近期调用量、成功率和平均耗时。
- QQ 群消息发送器统一区分消息被动回复、事件被动回复和主动消息，并集中管理 `event_id`、`msg_id`、`msg_seq` 与消息引用。
- 群主动消息要求群管理员本地开关与 QQ 平台授权同时开启；两个状态独立保存，平台拒绝事件不会改写管理员设置。发送通过数据库原子占位实现跨实例的群级独立 60 秒频控，失败按 UUID 回滚本次占位，被动回复不受影响。
- 群管理员可使用 `群公告 内容` 触发主动消息；Action 仅声明 `DeliveryMode.ACTIVE`，统一执行模板负责发送，并在未授权或频控时通过来源消息返回原因。
- QQ OpenAPI 非 2xx 响应统一映射为类型化异常，保留 HTTP 状态、官方 code 和 trace id，不记录上游响应原文；401 刷新凭证后重试一次。
- 指令发布阶段支持 `PRODUCTION`、`TEST`、`REVIEW` 三种运行模式，执行策略和帮助菜单共同过滤正式、实验与审核指令。
- 官方 61 个 JX3API HTTP contract 均使用类型化返回 DTO，`MethodEnum` 不再以裸 `Map.class` 承接官方接口数据。
- JX3API 查询统一处理空数据、限流、认证失效、超时和未知异常，并向用户返回可追踪的请求编号。
- JX3API 外部调用日志统一记录请求编号、指令、接口路径和耗时，敏感上游错误不直接返回给用户。
- 群指令 `invocation_id` 作为同步执行链路的统一追踪编号，贯穿 JX3API 用户错误提示、业务失败日志和 QQ OpenAPI 日志，并在请求结束后清理线程上下文。
- Micrometer 统一采集群指令、JX3API 查询和 QQ OpenAPI 操作耗时，Prometheus 端点仅默认监听本机管理端口，指标不包含群、成员或消息内容。
- `bot.jx3.cache.requests` 按固定接口路径记录数据库共享缓存的 hit/miss，可直接计算真实缓存命中率，非缓存接口不进入分母。
- QQ 当前机器人身份使用官方 `GET /users/@me` 类型化资源客户端；可选 Actuator 健康指标通过该接口检查鉴权与连通性，默认关闭且不暴露机器人标识或凭证。
- QQ 消息入口、发送方式、返回类型、指令权限、项目频控和审核发布边界统一记录在 `QQ_CAPABILITY_POLICY.md`。
- `INTERACTION_CREATE` 已接入独立 Action；稳定外层字段使用 Java Bean，类型相关的 `resolved` 保留 JSON，并通过 `QqInteractionHandler` 扩展按钮、菜单和表单业务。
- `菜单` 已使用 Markdown + 自定义 Keyboard 返回四个帮助分类；点击后由只读帮助 handler 按 `event_id` 返回当前群可用指令，不执行任意回调文本。
- `CommandArguments` 在注册表中一次解析服务器、角色、关键词和数字参数，并随调用上下文传给 Action。
- `GroupCommandExecutionService` 统一编排权限、群策略、个人默认参数、冷却、业务 Action、QQ 发送和调用审计，消息 Action 只负责 payload 解析与委派。
- `GroupConfigurationService` 统一承接整群配置读写；Action 读取群默认区服、绑定区服和修改群功能开关均不直接操作 Mapper。
- 机器可读覆盖矩阵持续校验 `REGEX -> MethodEnum -> 官方 HTTP contract`，并明确区分官方与旧接口。
- 活动月历、行侠事件、科举答题、家园鲜花、家园装饰、器物图谱、新闻资讯和搜索区服已接入群指令，并由各自 Action 输出稳定 Vue 图片数据。
- 阵营拍卖、的卢记录、骗子查询、未做奇遇、名剑排行、名剑统计、角色信息、角色百战、心法阵眼和技能详情已接入会员群指令。
- 技改、小药、扶摇、近期奇遇、奇遇汇总、师徒、本服榜单、掉落统计、角色名片和所有名片已接入统一查询链路。
- 随机/缓存名片、资历排行、阵营与诛恶事件、关隘、赤兔、马场、试炼、贴吧/黑市物价、物品搜索、帮战、副本解密、统战、八卦和舔狗日记已接入扩展查询链路。
- 官方 61 个 HTTP contract 均已注册为群指令；阿里语音使用独立凭证配置和 QQ 群语音上传链路，默认关闭。

### 2.2 JX3 查询能力待完善

- 当前文档中的 61 个 JX3API HTTP contract 已全部注册为群指令，并由覆盖矩阵做双向校验；官方新增接口时必须同步补充 contract、`MethodEnum`、`REGEX` 和返回处理。
- 根据接口后续字段变化继续细化嵌套 DTO；当前 61 个官方 HTTP contract 已全部移除裸 `Map.class` 返回。
- 为每个查询明确返回类型：文本或图片。
- 继续为其余高信息密度查询补齐 Vue 模板；活动日历、活动月历、行侠事件、科举答题、扶摇预测、家园鲜花、搜索区服、角色名片、所有名片、随机名片、缓存名片、新闻资讯、维护公告、骗子查询、搜索物品、角色信息、角色百战、角色奇遇、未做奇遇、近期奇遇、物品价格、黑市物价、贴吧物价、金币价格、角色装备、团队招募、师徒系统、奇遇统计/汇总、名剑排行/统计/战绩、本服榜单、掉落统计、资历排行、试炼排行、帮战记录、阵营拍卖、阵营沙盘、阵营事件、诛恶事件、百战首领、心法阵眼、技能详情、技改记录、小药推荐、家园装饰、器物图谱、关隘首领、本日赤兔、本周赤兔、马场刷新、的卢记录、烟花记录、奇穴详情、副本进度、副本解密和统战歪歪已接入图片返回；当前图片响应清单共 58 条。
- 为“角色装备”和“副本进度”寻找语义等价的官方替代能力；替代明确前继续使用旧接口并在覆盖矩阵中保持 `LEGACY` 标记。
- 使用真实阿里云凭证和 QQ 正式环境完成语音指令联调；当前离线测试已覆盖参数、音频 URL、`file_type=3` 上传和 media 消息拼装。
- `水墨圈圈` 与 `吃瓜` 目前只有 `EXPERIMENTAL` 指令入口，分别缺少已确认的专用图片模板/数据源和语义明确的查询接口；两者不会在 `PRODUCTION` 模式的帮助菜单或执行链路中开放，补完前不得拿“角色奇遇”或“八卦帖子”等相近接口冒充实现。
- 根据实际运行反馈继续细化 JX3API 错误码映射；当前已统一处理空数据、限流、认证失效、超时和未知异常。
- `invocation_id` 已写入 SLF4J MDC，并由日志 pattern 输出；未来选定集中式日志平台后只需采集现有日志字段。耗时和结果指标已接入 Micrometer/Prometheus。
- 根据运行数据继续调整缓存接口范围和 TTL；当前 PostgreSQL 实现已支持多实例共享，流量继续增长时可替换为 Redis。

### 2.3 QQ 官方能力待扩展

后续项目不只服务 JX3 查询，也会逐步支持腾讯机器人官方提供的更多指令和消息能力。

待实现方向：

- `QqOpenApiClient` 已统一 QQ token 缓存、认证重试、错误映射以及 GET/POST/PUT/DELETE 传输；`QqGroupMessageClient` 维护群消息与群文件资源，`QqBotIdentityClient` 维护官方 `/users/@me` 身份资源，后续按实际业务继续增加类型化客户端。
- 继续增加 Ark 和其他 Markdown/Keyboard 业务并完成平台联调；当前 `菜单` 已形成首个 Markdown + Keyboard 业务闭环。
- Embed 保留类型化模型供频道等场景扩展；QQ 群聊当前不支持，群发送器会明确拒绝。
- `群公告 内容` 已形成首个群主动消息业务触发；后续事件订阅等业务继续复用统一发送封装、本地开关、QQ 平台授权和独立频控。
- 使用 QQ 正式环境联调事件被动回复、消息引用和群主动消息；当前已完成离线结构与互斥规则测试。
- 为其他按钮和表单增加具体业务 handler；帮助分类按钮已接入，通用 `INTERACTION_CREATE -> QqInteractionHandler -> BotResponse -> event_id 回复` 链路已经具备。
- 继续支持 QQ 官方新增的群事件；当前已处理机器人加入群、退出群以及群主动消息允许、拒绝事件。当前官方事件定义未提供普通群成员加入、退出事件，不预造事件类型。
- 继续扩展官方事件类型；Payload Action 和互动业务 handler 两级可插拔路由已经具备。
- 根据 QQ 正式环境反馈继续补充细粒度官方 code 映射；当前已按 HTTP 状态统一处理认证、权限、限流、参数、目标不存在、服务端和网络异常。
- 权限、频率、消息能力和审核发布状态已统一记录在 `QQ_CAPABILITY_POLICY.md`；正式平台联调结果应持续回填。

### 2.4 机器人指令系统待完善

- 为统一指令注册表继续补充更细粒度权限元数据；当前已具备公开/群管理权限、发布阶段和群单项功能开关。
- 根据运行情况调整各指令冷却覆盖值；当前支持按 `REGEX` 枚举名单独配置。
- 继续扩展群维度配置，例如事件订阅；默认服务器、查询总开关、实验开关和单项指令开关已具备。
- 继续按实际查询需要扩展用户维度偏好；当前已支持默认门派、多个常用角色、默认角色切换和单个/全部解绑。
- 为未来审核指令补充 `REVIEW` 元数据并在 `valid` 分支使用；当前 feature 分支只提供通用隔离机制，不包含审核写死返回。

### 2.5 数据与持久化待完善

- 继续扩展现有群配置表；已保存群 openid、默认服务器、查询总开关、实验功能开关、主动消息本地开关、QQ 平台授权状态和共享频控占位。
- 单项指令覆盖值保存在 `group_command_setting`，未配置时按开启处理，系统指令始终忽略单项覆盖。
- 继续扩展用户配置；当前 `user_info` 按 `member_openid` 保存默认角色指针和默认门派，`user_role_binding` 保存该账号的常用角色集合。
- WebSocket 订阅配置表属于未来 WS 专项，不纳入当前 HTTP 群指令完成口径；启动该专项时再设计群订阅事件表。
- 为调用记录设计外部归档介质；当前已具备可配置保留周期、每日定时清理和按群聚合统计。
- 根据缓存命中率、数据库负载和实际吞吐评估是否将现有 PostgreSQL 共享缓存替换为 Redis。

### 2.6 运维与质量待完善

- 继续接入集中式日志平台；当前已提供 Prometheus 指标和贯穿群指令、JX3API、QQ OpenAPI 的 `invocation_id`，token、ticket、密码等敏感字段已脱敏，入口日志不记录完整 payload 和聊天内容。
- 在目标 GitHub 仓库首次运行并观察 CI，当前 workflow 已完成本地命令验证但尚无远端运行结果。

## 3. 当前模块划分

### 3.1 消息入口层

相关代码：

- `controller/BotMessageController.java`
- `service/BotMessageService.java`
- `service/PayloadActionRegistry.java`
- `entity/dto/payload/Payload.java`
- `contants/OpCode.java`
- `contants/PayloadTEnum.java`

职责：

- `BotMessageController` 接收 `/bot/message` 的 webhook 请求。
- `BotMessageService` 根据 `op` 区分回调校验和 dispatch 消息。
- `PayloadTEnum` 根据 `t` 字段把事件路由到对应 action。
- `PayloadActionRegistry` 在启动时验证每个 dispatch 事件都存在处理器。
- 当前 `GROUP_AT_MESSAGE_CREATE` 和 `GROUP_MESSAGE_CREATE` 统一进入 `GroupAtMessageAction`。
- `GROUP_ADD_ROBOT`、`GROUP_DEL_ROBOT`、`GROUP_MSG_RECEIVE` 和 `GROUP_MSG_REJECT` 进入 `GroupLifecycleAction`，再由 `QqGroupLifecycleHandlerRegistry` 分发给可插拔业务 handler。

设计约束：

- 群 at 消息和普通群消息需要兼容为同一套处理链路。
- 未识别事件类型应记录日志并安全返回。
- 临时审批用写死返回属于过渡代码，不进入长期设计。

### 3.2 群消息分发层

相关代码：

- `action/BaseAction.java`
- `action/GroupAtMessageAction.java`
- `jx3/http/command/Jx3CommandRegistry.java`
- `jx3/http/command/ResolvedJx3Command.java`
- `jx3/http/command/CommandArguments.java`
- `jx3/http/util/REGEX.java`
- `service/GroupCommandPolicy.java`
- `service/GroupCommandExecutionService.java`
- `service/GroupCommandCooldownService.java`
- `service/CommandInvocationRecorder.java`
- `entity/CommandInvocation.java`
- `config/CommandCooldownProperties.java`
- `config/CommandReleaseProperties.java`

职责：

- `GroupAtMessageAction` 从 payload 的 `d` 字段解析群消息 DTO，并委派给统一执行模板。
- 由 `Jx3CommandRegistry` 规范化 `content`，兼容 `/指令` 与 `指令`。
- 使用 `REGEX.matchEnum(content)` 识别具体 JX3 指令并一次性提取参数为只读 `CommandArguments`。
- `CommandArguments` 兼容现有 `server/server1/value/value1/roleName` 命名组，统一读取服务器、角色、关键词和整数参数。
- 注册表从 Spring Action 集合建立映射，并在启动时验证每条 `REGEX` 都有处理器；源码契约同时反向校验每个具体 `@Jx3Action` 都至少被一条 `REGEX` 引用，避免保留无法由指令到达的 Spring Action Bean。
- `GroupCommandExecutionService` 在权限和群功能策略通过后补齐个人默认参数，并按群与指令获取冷却执行权。
- 统一执行对应 `Jx3BaseAction#doRequest(...)`，再将子类返回的 `BotResponse` 交给 `GroupMessageSender`。
- `GroupMessageSender` 根据返回标识拼装 QQ 消息、补充 `msg_id`，并调用 `/v2/groups/{group_openid}/messages`。
- 无返回、发送失败或业务异常均由执行模板统一完成冷却收尾和调用审计。

默认参数优先级：

1. 本次指令显式传入的区服和角色。
2. `user_info` 中当前 `member_openid` 的个人默认区服、角色和门派。
3. `group_info` 中当前 `group_openid` 的群默认区服。
4. `jx3api.api.default-server` 全局默认区服。

`绑定 乾坤一掷` 是群管理员指令，只按当前 `group_openid` 修改 `group_info`，作用于整个群，不写入个人账号表。个人指令只按发送者 `member_openid` 操作，不附带 `group_openid`，因此同一个 QQ 账号在不同群中共用个人绑定：`绑定角色 区服 角色名` 添加并设为默认，`添加角色 区服 角色名` 只添加常用角色，`切换角色 区服 角色名` 切换默认角色，`我的角色` 列出全部角色，`解绑角色 区服 角色名` 删除单个角色，无参的 `解绑角色` 清空当前账号的全部角色；`绑定门派 门派名` 与 `解绑门派` 独立维护默认门派。依赖角色或门派的查询使用 `user_info` 中的个人默认值补齐参数，显式参数始终优先。

群指令冷却约定：

- 冷却只应用于群消息处理链路，不影响其他未来消息入口。
- 同一个群的同一指令共享冷却；不同群或不同指令互不影响。
- 不涉及外部调用的指令默认 5 秒；绑定了 `MethodEnum` 的 JX3API 指令默认 30 秒。
- 不通过 `MethodEnum` 但仍调用外部服务的指令必须显式声明，例如 DPS 为 30 秒。
- 权限不足或群功能关闭的请求不占用冷却。
- Action 执行期间保持占用，冷却从 Action 和 QQ 发送结束后开始计算。
- 冷却期间静默不响应，保证机器人两次响应的真实间隔不小于配置值。
- 冷却状态保存在 `group_command_cooldown`，通过条件更新和唯一键保证多个应用实例同一时刻只放行一个相同群、相同指令的请求。
- 每次执行使用独立 UUID 完成占位，旧请求不能释放新请求；实例异常退出时，占位在租约超时后可被其他实例接管，默认 300 秒。

群指令调用记录约定：

- 每次匹配成功的群指令生成独立 `invocation_id`，记录群、成员、指令、分类、是否外部调用、耗时和响应类型。
- `GroupCommandExecutionService` 在调用 Action 前建立 `RequestTraceContext`；JX3API 请求编号优先复用 `invocation_id`，QQ 日志同时区分内部编号和腾讯 `qqTraceId`。
- 追踪上下文支持嵌套恢复，并通过 `AutoCloseable` 作用域在成功、提前返回和异常路径统一清理，禁止在线程池中泄漏到下一条消息。
- 状态包括 `SUCCESS`、`PERMISSION_DENIED`、`FEATURE_DISABLED`、`INVALID_ARGUMENTS`、`COOLDOWN`、`NO_RESPONSE` 和 `FAILED`。
- 不保存原始 `content`、正则提取参数、JX3API 请求参数或完整异常堆栈，失败原因只保留最长 500 字符的脱敏摘要。
- 调用记录采用 best-effort 写入，审计表不可用时只写脱敏错误日志，不影响 QQ 消息处理主链路。
- Action 与 QQ 发送均完成且冷却状态成功释放后才记录 `SUCCESS`，避免一次调用同时出现成功和失败状态。

指令发布阶段约定：

- `SYSTEM` 指令不受查询总开关和运行模式影响，用于帮助、绑定与群设置等基础能力。
- `PRODUCTION` 指令在 `PRODUCTION`、`TEST`、`REVIEW` 三种模式均可使用，但仍受群查询总开关控制。
- `EXPERIMENTAL` 指令只在 `TEST` 模式开放，并且目标群必须由管理员开启实验功能。
- `REVIEW` 指令只在 `REVIEW` 模式开放；feature 分支当前没有审核专用指令。
- 默认运行模式为 `PRODUCTION`；开发 profile 默认使用 `TEST`，生产部署应显式设置 `BOT_COMMAND_RUNTIME_MODE=PRODUCTION`。
- 帮助菜单通过 `GroupCommandPolicy.availableDefinitions(...)` 读取当前群可执行指令，与实际执行判定保持一致。

逐指令覆盖示例：

```yaml
bot:
  command:
    cooldown-in-flight-timeout-seconds: 300
    cooldown-seconds:
      ServerCheck: 10
      Help: 0
```

配置键使用 `REGEX` 枚举名；`0` 表示关闭该指令冷却，允许范围为 0 至 86400 秒。执行中租约允许范围为 30 至 3600 秒。未知指令名和非法秒数会导致启动校验失败。

当前约束：

- feature 分支不保留临时审批写死返回；审核专用逻辑只允许留在审核分支或测试模拟层。
- 指令与 JX3API 的关系集中在 `REGEX`、`MethodEnum` 和 `Jx3CommandRegistry`，action 只负责参数、请求和返回数据拼装。

### 3.3 JX3 HTTP 业务层

相关代码：

- `jx3/http/action/base/Jx3BaseAction.java`
- `jx3/http/action/*.java`
- `jx3/http/MethodEnum.java`
- `jx3/http/util/Jx3RequestUtil.java`
- `jx3/http/data/**`

业务配置实体：

- `GroupInfo` / `group_info`：以唯一 `open_group_id` 查询，保存整群默认区服、查询开关、实验功能开关、主动消息本地开关、QQ 平台授权状态和共享频控占位。
- `GroupCommandSetting` / `group_command_setting`：以群 openid 与稳定的 `REGEX` 枚举名唯一定位单项指令开关。
- `UserInfo` / `user_info`：以唯一 `member_openid` 查询，保存单独 QQ 账号当前默认角色的区服、角色名和默认门派。
- `UserRoleBinding` / `user_role_binding`：以 `member_openid` 查询个人常用角色集合，同一账号下的区服与角色名组合唯一。
- `CommandInvocation` / `command_invocation`：保存群指令执行元数据和脱敏结果，不保存用户原始消息。
- `GroupCommandCooldown` / `group_command_cooldown`：保存群与指令的执行占位、租约和下次可执行时间，供多应用实例共享。
- `Jx3ApiCacheEntry` / `jx3_api_cache`：保存公共 JX3API 查询的 JSON 响应和过期时间，缓存键不包含 token。
- `GroupConfigurationService`：整群配置的统一业务读写入口，按 `group_openid` 查询或原子绑定默认区服，并分别维护群功能开关与 QQ 平台授权状态。
- `UserCommandPreferenceService`：负责个人绑定的增删查改，以及在 Action 执行前合并个人默认参数。

数据库访问边界：

- Action 只调用领域服务，不直接新增或修改 `GroupInfo`、`UserInfo` 和 `UserRoleBinding`。
- 整群配置以 `group_openid` 为作用域，只能通过 `GroupConfigurationService` 读写；不同群的数据互不影响。
- 个人角色、常用角色和门派以 `member_openid` 为作用域，只能通过 `UserCommandPreferenceService` 写入；同一账号在不同群中共用个人绑定。
- 群配置条件更新在 Mapper 方法内使用短事务；唯一键竞争失败后可在独立事务中重试，不在 QQ 或 JX3API 网络调用期间持有数据库事务。

开发环境由 Hibernate `ddl-auto=update` 创建或更新表；生产环境关闭自动建表时，先执行 `docs/database/group-info.sql`、`docs/database/user-info.sql`、`docs/database/group-command-setting.sql`、`docs/database/group-command-cooldown.sql`、`docs/database/jx3-api-cache.sql` 和 `docs/database/command-invocation.sql`。
- `service/GroupCommandPolicy.java`

职责：

- `REGEX` 识别用户输入。
- `MethodEnum` 描述 JX3API 接口路径、接口名称和返回数据实体。
- 每个 `Jx3BaseAction` 子类负责一个或一组业务指令。
- 子类必须显式实现 `dealAfterJx3ApiRequest(BaseResult baseResult)`；源码契约测试会检查所有已注册 Action 以及全部具体 `Jx3BaseAction` 源文件，并禁止该方法返回 `null`。
- `Jx3BaseAction` 提供通用请求、默认服务器、文本消息、图片消息等辅助能力。

当前模板方法约定：

```java
public BotResponse doRequest(GroupAtMessageCreateDto dto, String requestRegex, REGEX regex)
```

统一流程：

1. 保存上下文：消息、原始指令、匹配到的 `REGEX`。
2. 如果 `MethodEnum` 不为空，调用 JX3API。
3. 将 JX3API 响应转成 `BaseResult`。
4. 调用子类的 `dealAfterJx3ApiRequest`。
5. 子类返回 `BotResponse`，声明文本、图片、Markdown、Ark 或富媒体等业务结果。
6. `GroupMessageSender` 将 `BotResponse` 统一转换为 `TxMessageInfo` 并调用 QQ 接口。

并发约束：

- `Jx3BaseAction` 是 Spring 单例 Bean，不允许用普通实例字段保存当前消息上下文。
- 当前消息、原始指令和命中的 `REGEX` 使用请求线程上下文保存，并在模板方法的 `finally` 中清理。
- 子类通过 `currentMessage()`、`currentRequestRegex()`、`currentRegex()` 读取当前请求。

### 3.4 消息返回模型

相关代码：

- `entity/dto/common/TxMessageInfo.java`
- `entity/dto/common/MediaDto.java`
- `entity/dto/common/MarkdownDto.java`
- `entity/dto/common/KeyboardDto.java`
- `entity/dto/common/ArkDto.java`
- `entity/dto/common/EmbedDto.java`
- `service/GroupMessageSender.java`
- `qq/QqOpenApiClient.java`
- `qq/QqBotIdentityClient.java`
- `qq/QqOpenApiHealthIndicator.java`
- `service/QqGroupMessageClient.java`
- `util/RequestUtil.java`
- `entity/dto/qq/QqBotUserDto.java`
- `entity/dto/common/MessageReferenceDto.java`

当前支持：

- `msg_type = 0`：文本消息。
- `msg_type = 2`：Markdown 消息，支持自定义内容、模板参数和 Keyboard。
- `msg_type = 3`：Ark 模板消息；被动消息是否可用取决于 QQ 平台准入权限。
- `msg_type = 7`：群聊富媒体图片消息。

`BotResponse.DeliveryMode` 声明最外层发送方式：默认 `REPLY` 使用来源消息被动回复，`ACTIVE` 通过群主动消息发送。Action 不直接调用 QQ 客户端。

限制：

- Keyboard 不能脱离 Markdown 单独发送，自定义按钮最多 5 行、每行最多 5 个。
- 自定义按钮必须包含展示文案、合法 action 与 permission；`type=1` 回调按钮的 `action.data` 必填且最多 128 个字符。
- Embed 虽然在通用消息模型中保留，但 QQ 群聊接口不支持，不能从 `GroupMessageSender` 发送。

返回流程：

- 文本消息：设置 `content` 和 `msg_type=0`。
- 图片消息：生成图片、上传文件、取得 `file_info`，设置 `msg_type=7` 和 `media.file_info`。
- `QqOpenApiClient` 统一 token、401 单次认证重试、错误映射和 GET/POST/PUT/DELETE；具体资源客户端负责路径、查询参数、请求体和响应校验。
- 群资源路径只由 `QqGroupMessageClient` 维护，底层 HTTP 只由 `QqOpenApiClient` 执行；业务 Action 和发送器不拼接 OpenAPI URL。
- `QqBotIdentityClient` 使用官方 `GET /users/@me` 校验当前身份必须包含 id、username 且 `bot=true`；可选 HealthIndicator 只报告认证布尔值或安全错误分类。

发送上下文由 `GroupMessageSender` 统一处理：

- `send(sourceMessage, response)`：消息被动回复，自动使用来源消息 `id`，默认 `msg_seq=1`，允许业务返回指定 1 至 5 的序号。
- `sendEventReply(groupOpenId, eventId, response)`：事件被动回复，只设置 `event_id`。
- `sendActive(groupOpenId, response)`：群主动消息，不设置 `event_id`、`msg_id` 或 `msg_seq`。
- `BotResponse.referenceSourceMessage()`：请求引用来源消息，由发送器生成 `message_reference`，Action 不直接填写消息 ID。
- 主动消息和事件回复不能携带消息回复序号或引用来源消息；非法组合在调用 QQ 前直接拒绝。
- 即使使用过渡 `RAW_MESSAGE`，其中的传输上下文字段也会先被清除，再由发送器按当前模式统一填写。

群聊被动回复必须设置：

- `msg_id`：来自用户原始消息 id。
- `msg_seq`：发送器显式设置，默认是 1；同一 `msg_id + msg_seq` 不重复使用。

### 3.5 QQ 互动业务层

相关代码：

- `action/InteractionCreateAction.java`
- `entity/dto/interaction/**`
- `qq/interaction/QqInteractionHandler.java`
- `qq/interaction/QqInteractionHandlerRegistry.java`
- `qq/interaction/HelpInteractionHandler.java`
- `service/HelpMenuService.java`

约定：

- `InteractionCreateAction` 只解析事件、选择 handler，并将 `BotResponse` 交给事件被动回复发送器。
- `data.resolved` 是随互动类型变化的 JSON，具体 handler 只读取自己负责的字段。
- handler 必须使用固定业务前缀识别回调，不允许把任意 `button_data` 直接交给群指令执行链路。
- 当前帮助按钮使用 `jx3:help:<CommandGroup>`；回调只读取 `REGEX` 元数据和群功能策略，不写数据库、不调用外部接口。
- 未匹配互动安全忽略；同一互动匹配多个 handler 时拒绝执行，防止重复业务响应。

## 4. 图片消息设计

### 4.1 目标

部分 JX3 查询结果适合用图片展示，例如角色装备、团队招募、奇遇统计、物品价格等。

长期设计不应让 Java 拼整段 HTML，而应：

- HTML 模板负责页面结构。
- Vue 负责模板渲染。
- Java 负责准备数据。
- Playwright 负责截图。
- MinIO 负责图片公网访问。
- QQ 群富媒体接口负责发送。

### 4.2 当前通用链路

相关代码：

- `util/HtmlToImageUtl.java`
- `minio/MinioUtil.java`
- `service/QqGroupMessageClient.java`
- `jx3/http/action/base/Jx3BaseAction.java`

流程：

```text
JX3API 数据
-> 子类 buildTemplateData
-> HtmlToImageUtl 注入 window.__BOT_DATA__
-> Vue 模板渲染
-> Playwright 截图为 PNG
-> MinIO 上传得到公网 URL
-> QqGroupMessageClient 上传图片得到 file_info
-> 返回 msg_type=7 的群富媒体消息
```

子类需要明确声明返回类型：

```java
@Override
protected BotResponse.ResponseType getResponseType() {
    return BotResponse.ResponseType.IMAGE;
}
```

图片类子类需要实现：

```java
@Override
protected String getTemplatePath() {
    return "团队招募";
}

@Override
protected Object buildTemplateData(BaseResult baseResult) {
    return data;
}

@Override
protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
    return buildMessageByTemplate(baseResult);
}
```

模板直接使用 Vue 语法读取根数据，`HtmlToImageUtl` 会注入 `window.__BOT_DATA__`、挂载 `#main` 并设置渲染完成标识：

上游结构存在分组、内部标识或状态码时，具体 Action 应先转换为稳定的图片视图模型，模板不直接依赖接口嵌套结构。例如关隘首领接口的 `List<{server, data[]}>` 在 Java 中扁平化为 `{zone, server, leader, campName, castle, statusText}[]`，`id` 和数字 `status` 不进入模板数据。

结构相同但语义模式不同的查询可以共享 Vue 模板，但 Java Action 仍分别接管请求和数据转换。本日赤兔与本周赤兔共同使用 `赤兔记录.html`，分别传入 `{mode: "本日", server: "全服", data}` 和 `{mode: "本周", server: "全服", data}`；两类 Action 各自将官方 DTO 裁剪为 `{server, mapName, horse, date}[]`，不向模板传递 `id` 或含义未确认的 `send`。

接口使用动态对象键表达业务分组时，Action 必须在 Java 中转换为稳定数组，避免 Vue 模板依赖会变化的 JSON 属性名。马场刷新将官方 `Map<地图, List<预测文本>>` 转换为 `{mapName, predictions}[]`，完整模板数据为 `{zone, server, note, data}`；地图、预测文本和说明均按普通文本插值展示。

JX3API 的秒级时间戳属于游戏业务时间，统一由 `TimeUtils.timeFormatting` 按 `Asia/Shanghai` 转换，不依赖应用服务器所在时区。的卢记录在 DTO 层完成北京时间转换，Action 再裁剪为 `{zone, server, mapName, refreshTime, captureRoleName, captureCampName, captureTime, auctionRoleName, auctionCampName, auctionTime, auctionAmount, startTime, endTime}[]`，内部 `id`、历史兼容的 `name` 和 `level` 不进入模板数据。

官方字段改名时，DTO 应以当前官方字段作为主属性，并使用 `@JsonAlias` 兼容仍可能出现的旧字段；不允许只因其他字段可反序列化就忽略业务字段丢失。阵营拍卖以 `map_name/item_name/item_amount` 为当前字段，同时兼容旧 `name/amount`，Action 输出 `{zone, server, mapName, roleName, campName, itemName, itemAmount, time}[]`，内部 `id` 不进入模板。

百战首领当前官方根结构使用 `week/boss/list`，历史响应中的根列表字段 `data` 通过 `@JsonAlias` 兼容；列表项同时映射 `index/name/skill/data`。`ActiveMonsterAction` 不将上游 DTO 直接交给 Vue，而是输出稳定结构 `{server, week, boss, start, end, data: [{index, name, skills, extraName, effects, description}]}`，附加机制中的内部 `id` 和时间字段不进入模板。

角色百战以官方 `zone/server/role_name/skill_energy/skill_stamina/skill_count/skill_list/update_time` 为主字段，并兼容旧客户端 camelCase 字段和技能 `nCost/nColor/nLevel/szBossName/szSkillName/bDeprecated`。Action 输出 `{zone, server, roleName, skillEnergy, skillStamina, skillCount, updateTime, skills: [{name, leaderName, cost, color, level, deprecated}]}`；角色 `role_id/global_id`、技能输入输出 ID 和旧类型字段只留在 DTO 兼容边界，不进入 Vue 数据。

心法阵眼当前官方效果列表字段为 `data`，DTO 通过 `@JsonAlias("descs")` 兼容旧字段；Action 输出 `{name, skillName, effects: [{level, name, description}]}`。技能详情的官方根结构为套路分组列表，Action 输出 `{name, groups: [{category, skills: [{name, summary, description, specialDescription, interval, consumption, distance, kind, subKind, releaseType, weapon}]}]}`。两个 Action 分别负责转换，模板不直接依赖官方嵌套结构；上游第三方技能图标 URL 不进入稳定视图，也不会在截图阶段加载。

未做奇遇当前官方列表项为 `{name, type, level}`；历史 `last` 结构继续留在 DTO 接收边界，但 Action 只输出 `{server, name, data: [{name, type, level}]}`。近期奇遇复用事件 DTO 接收官方 `id/zone/server/name/event/source/status/time`，Action 输出 `{server, data: [{zone, server, name, event, time}]}`；内部 `id/source/status` 不进入模板，也不在官方语义未确认时自行转换状态文案。

师徒系统官方根结构为 `{zone, server, type, data, time}`，招募项包含角色、等级、阵营、帮会、体型、门派和说明，同时带有多类数字 ID。`MemberTeacherAction` 输出 `{zone, server, time, data: [{roleName, roleLevel, campName, tongName, tongMasterName, bodyName, forceName, comment}]}`；查询类型和 `roleId/bodyId/forceId` 只留在 DTO 边界。阵营沙盘官方根结构为 `{zone, server, reset, update, data}`，Action 输出 `{zone, server, update, data: [{castleName, tongName, masterName, campName}]}`，不向模板传递 `reset` 及帮会、领地、首领、阵营 ID。

阵营事件 DTO 以当前官方 `camp_name/fenxian_name/friend_name/role_name/seize_time` 为主字段，并通过别名兼容旧服务器和时间字段；Action 输出 `{scope, data: [{campName, fenxianName, friendName, roleName, seizeTime}]}`。诛恶事件输出 `{scope, data: [{zone, server, mapName, time}]}`；两类事件的记录 `id` 均不进入 Vue 数据。

技改记录官方列表项包含 `id/title/url/time`，`SkillReworkAction` 只输出 `{data: [{title, time}]}`；内部编号和外部详情 URL 不进入 Vue 数据。小药推荐官方列表项包含 `id/school/kungfu/color/class/name/boost`，`SchoolFoodsAction` 输出 `{data: [{school, kungfu, color, category, name, boost}]}`，其中 Java 视图使用 `category` 承接官方 `class` 字段，内部 `id` 不进入模板。

家园装饰与器物图谱使用同一类官方物品结构，接收 `id/name/type/color/source/architecture/limit/quality/view/practical/hard/geomantic/interesting/produce/image/tip`。两个 Action 分别接管请求与模板，并统一裁剪为 `{data: [{name, source, limit, quality, view, practical, hard, geomantic, interesting, produce, tip}]}`；内部编号、分类码、颜色码、建筑码和外部图片 URL 只留在 DTO 接收边界，不参与截图渲染。

活动日历接收官方 `{date, week, war, battle, orecar, school, rescue, draw, leader, team, luck, card}`，`ActiveCurrentAction` 输出 `{server, data: {date, week, war, battle, orecar, school, rescue, draw, leaders, teams, luck, cards}}`，并过滤列表中的空字符串，不再假设 `team` 固定存在三个元素。活动月历将 `{today, data[]}` 转换为 `{today: {date, week}, data: [{date, week, war, battle, orecar, school, rescue, luck, cards}]}`；冗余的 `year/month/day` 不进入模板。

新闻资讯与维护公告的当前官方列表项为 `{id, catid, type, title, date, url}`，DTO 以 `catid/type` 为主字段，并使用 `@JsonAlias` 兼容旧 `token/class`。两个 Action 共享 `新闻资讯.html`，分别传入 `{title: "新闻资讯", data}` 和 `{title: "维护公告", data}`；稳定列表项仅保留 `{type, title, date}`，内部 `id/catid` 和外部详情 URL 不进入 Vue 数据。

骗子查询把官方 `[{server, tieba, data: [{title, tid, text, time, url}]}]` 扁平化为 `{uid, data: [{server, tieba, title, text, time}]}`，帖子编号和帖子 URL 不进入模板。搜索物品把官方 `{class, subclass, name, alias, wblalias, value, desc, date, view}` 裁剪为 `{name, data: [{category, subclass, name, alias, value, description, date, imageDataUri}]}`，最多展示 8 条；`view` 只允许经 `RemoteImageDataUriLoader` 完成 HTTPS、白名单、地址、大小、类型、签名和像素校验后转换为 data URI，原始 URL 与 `wblalias` 不交给浏览器。

贴吧物价把官方记录 `{id, zone, server, name, url, context, reply, token, floor, time}` 转换为 `{server, name, data: [{zone, server, name, context, reply, floor, time}]}`，只保留可读区服、物品、帖子内容、回复数、楼层和时间，内部 `id/token` 与外部帖子 URL 不进入 Vue 数据。金币价格把当前官方 `{zone, server, tieba, wanbaolou, dd373, date}` 转换为 `{server, data: [{zone, server, tieba, wanbaolou, dd373, date}]}`；旧响应中的 `uu898/5173/7881/time` 只保留反序列化兼容，不进入稳定图片视图。

副本解密当前官方结构为 `{curr: {node, data}, next: {node, data}, time, cdtn}`，Action 转换为 `{data: {current: {node, result}, next: {node, result}, time, condition}}`。历史 `now_time/now_node/now_result/next_node/next_result/interval_time` 字段继续兼容，并统一落入同一稳定视图，Vue 模板不区分上游版本。统战歪歪把官方 `[{server, data: [{sid, logoUrl, users, snick, limit, logo, asid, esid, campName}]}]` 转换为 `{server, data: [{server, channels: [{name, campName, users, limit}]}]}`，频道与阵营名称、在线人数和容量用于展示，内部编号及图片地址全部剔除。

行侠事件当前官方示例使用 `map/stage/site/desc/icon/time`，DTO 以 `map_name/event` 为主字段并通过 `@JsonAlias` 兼容 `map/stage`。`ActiveCelebritiesAction` 输出 `{name, data: [{mapName, event, site, description, time}]}`，内部图标编号不进入 Vue 数据。家园鲜花接口使用动态服务器名作为 JSON key，`HomeFlowerAction` 将其转换为 `{server, name, data: [{server, flowers: [{name, color, price, lines}]}]}`，`line` 数组按线路逐项展示。

角色名片、所有名片、随机名片和缓存名片共享 `角色名片.html` 与稳定视图 `{title, server, name/filter, data: [{zone, server, roleName, showIndex, active, cacheTime, imageDataUri}]}`。`showHash/global`、原始 `showAvatar/avatar` URL 和随机接口 `status` 不进入 Vue 数据；同一响应最多展示 12 张名片，同一源 URL 只下载一次。图片先由 `RemoteImageDataUriLoader` 校验 HTTPS、精确域名白名单、无重定向、非内网解析地址、超时、响应字节数、MIME、文件签名和像素上限，再编码为 data URI；校验失败时模板显示占位，不影响名片元数据。

科举答题把官方 `[{id, question, answer, correctness, index, pinyin}]` 转换为 `{subject, data: [{question, answer}]}`，最多展示请求限制内的 5 条问答；题库 ID、正确率、内部索引和拼音检索字段不进入模板。搜索区服以当前官方 `{id, center, zone, name, event, voice, alias, slave}` 为主结构，并通过 `@JsonAlias` 兼容旧 `column/abbreviation/subordinate`；Action 输出 `{query, data: {zone, name, center, aliases, slaves}}`，内部 ID、事件状态和动态语音对象不进入 Vue。

扶摇预测把官方 `[{zone, server, status, time}]` 转换为 `{server, data: [{zone, server, time}]}`，秒级时间戳统一转换为北京时间。当前官方类型只把 `status` 定义为数字而未提供稳定的可读枚举语义，因此状态码保留在 DTO 接收边界，Action 不生成猜测性中文文案。

八卦帖子保持 `TEXT` 返回：接口请求固定 `limit=1`，将当前官方 `[{id, tags, zone, server, name, title, url, date}]` 转换为分类、区服、角色、标题和日期组成的简短中文文本，内部 ID 与帖子 URL 不进入 QQ 消息。DTO 以 `tags` 为当前字段，并通过 `@JsonAlias("class")` 兼容旧响应。

```html
<main id="main" v-cloak>
    <h1>{{ name }}</h1>
    <p>{{ data.serverName }}</p>
</main>
```

### 4.3 模板约束

- HTML 模板放在 `src/main/resources/static` 下。
- 模板应优先使用 Vue 渲染数据。
- Java 不负责拼接复杂 HTML。
- 模板中相对路径资源由 `HtmlToImageUtl` 注入的 `<base>` 辅助解析。
- 正式图片模板不直接依赖接口返回的第三方图片或脚本 URL；需要的静态资源应放入 classpath、自包含 HTML/CSS/SVG，或先经过 `RemoteImageDataUriLoader` 的受控下载与校验后以内嵌 data URI 进入模板。
- 上游文本默认使用 Vue 文本插值，不使用 `v-html`；确需渲染受控 HTML 时必须先建立白名单清洗边界。
- 截图前等待页面网络空闲及 `window.__BOT_RENDERED__`，确保 Vue 和图表完成渲染。

### 4.4 折线图模板约定

后续如果某个接口需要展示价格、排名、数量变化等趋势，可以在 Vue 模板内直接根据 JSON 数据生成图表。

当前 `物品价格.html` 使用 Vue + SVG 生成折线图，避免截图阶段依赖外部脚本加载。Java 只需要把 JX3API 返回数据传给模板：

```java
String imagePath = HtmlToImageUtl.renderTemplateToImage("物品价格", data);
```

字段对应关系：

- Java 统一传入 `{ "mode", "server", "name", "data" }`；`data` 是具体物品价格对象。
- 物品基础信息读取 `name`、`alias`、`classType/class`、`subclass`、`level`、`value`、`desc`、`date`，不读取接口提供的外部图片 URL。
- 价格记录兼容 `data` 和 `list` 两种二维数组字段，每个子数组代表一个接口分组。
- 单条价格记录读取 `date`、`datetime`、`zone`、`server`、`value` 以及 `sales/sale`，不展示 `id`、`token`、`source`、`status`。
- 折线图从全部有效价格记录中按时间排序，最多展示最近 8 条；近期记录表最多展示 6 条。

如果后续需要更复杂的图表，可以引入 ECharts，但需要确认截图工具能稳定加载本地脚本，并在图表渲染完成后设置 `window.__BOT_RENDERED__ = true`。

## 5. 通用接口设计

### 5.1 背景

`REGEX`、`MethodEnum`、具体 action 之间保留三层映射：

- 输入指令通过 `REGEX` 匹配。
- `REGEX` 绑定 action 和 `MethodEnum`。
- action 自己生成请求参数和返回内容。

项目通过 `Jx3BaseAction` 模板方法和 `GroupCommandExecutionService` 执行模板减少重复编排代码。

### 5.2 当前抽象

现有 `Jx3BaseAction` 子类实现以下扩展点：

```java
protected abstract Map<String, Object> getRequestParam(String requestRegex, REGEX regex);

protected abstract BotResponse dealAfterJx3ApiRequest(BaseResult baseResult);
```

保留当前强约束：

- 所有子类必须实现 `dealAfterJx3ApiRequest`。
- 子类只返回业务响应对象 `BotResponse`，不直接调用 QQ 发送接口。
- 子类通过 `BotResponse` 声明返回类型，例如 `TEXT`、`IMAGE`、`IMAGE_URL`、`AUDIO_URL`、`MARKDOWN`、`ARK`、`MEDIA`。
- `GroupMessageSender` 根据 `BotResponse.responseType` 统一拼装 `TxMessageInfo` 并调用 QQ 接口。
- 子类可以调用 `buildMessageByTemplate(baseResult)` 复用文本或图片模板返回。
- 模板方法在进入子类成功结果处理前统一识别空数据和错误响应。
- 每次外部调用生成短请求编号，日志记录接口路径与耗时；面向用户的错误提示携带同一编号。
- 上游响应中的 token、ticket 或其他错误详情不会原样发送到群聊。
- QQ 错误响应体只在请求层内部用于提取 `code` 与 `trace_id`，异常消息和日志不包含原始 `message`；除 401 刷新 token 外不自动重发消息。

### 5.3 通用模板方法

当前形成两条清晰路径：

文本路径：

```text
Jx3CommandRegistry -> getRequestParam -> JX3API -> buildTextContent -> BotResponse.text -> GroupMessageSender -> QqGroupMessageClient -> QQ
```

图片路径：

```text
Jx3CommandRegistry -> getRequestParam -> JX3API -> buildTemplateData -> BotResponse.image -> GroupMessageSender -> Vue HTML -> image -> QqGroupMessageClient -> QQ
```

子类通过 `getResponseType()` 手动声明：

```java
protected BotResponse.ResponseType getResponseType() {
    return BotResponse.ResponseType.TEXT;
}
```

默认是文本；需要图片的子类 override 为 `IMAGE`。

## 6. 数据与配置

### 6.1 数据实体

JX3API 返回实体集中在：

- `jx3/http/data/**`

建议：

- DTO 字段尽量与接口字段稳定映射。
- API 返回结构变化时先更新 DTO，再更新 action。
- 对于列表和嵌套结构，优先使用类型化 DTO，不要在业务层大量使用裸 `Map`。
- 官方 contract 测试强制 `MethodEnum.resultBeanClass != Map.class`；动态 JSON key 使用 `@JsonAnySetter`，同字段多形态数据使用 `JsonNode` 明确保留结构。

### 6.2 配置

相关配置类：

- `config/TxBotProperty.java`
- `jx3/config/ApiProperties.java`
- `jx3/config/RemoteImageProperties.java`
- `jx3/config/WebSocketProperties.java`
- `minio/MinioConfig.java`

关键配置：

- QQ bot openapi 地址、token 地址、appId、appSecret、1 至 60 秒请求超时，以及默认关闭的远程健康检查开关。
- JX3API ticket/token/name/defaultServer。
- DPS 实验指令的 token、服务地址、请求路径和模型名均由 `jx3api.api.dps-*` 配置提供，默认保持现有第三方服务和“旗舰”模型。
- MinIO endpoint、bucket、access-key、secret-key。
- WebSocket 开关和连接参数。
- `bot.command.runtime-mode`：指令运行模式，允许 `PRODUCTION`、`TEST`、`REVIEW`。
- `bot.command.audit.*`：调用记录保留天数、清理 cron、默认统计天数和最大统计天数。
- `bot.qq.active-message-min-interval-seconds`：同一群两次主动消息之间的最小秒数，默认 60。
- `jx3api.cache.cleanup-cron`：数据库共享缓存的过期记录清理计划，默认每天 `03:45`。
- `jx3api.remote-image.*`：远程图片开关、精确域名白名单、1 至 15 秒超时、最大字节数和最大像素数；默认仅允许已知 JX3API 名片图片域名。

## 7. WebSocket 推送设计

相关代码：

- `jx3/ws/CustomWebSocketHandler.java`
- `jx3/ws/WebSocketClientInitializer.java`
- `jx3/ws/action/WsActionHandler.java`
- `jx3/ws/data/**`
- `jx3/ws/service/WsDataPushService.java`

职责：

- 接入 JX3API WebSocket 事件。
- 根据事件 action 分发到数据模型。
- 将事件推送到目标群或业务服务。

当前 HTTP 查询和 WS 事件应保持独立：

- HTTP 查询：用户主动输入指令。
- WS 事件：外部事件推送触发。

当前 `feature-jx3api` 的完成口径只覆盖 HTTP 群指令、QQ 消息发送、模板截图和相关持久化能力，不扩展 WebSocket 订阅配置表。后续重启 WS 专项时，需要另行补充订阅表、事件授权、推送频控和真实事件验收。

## 8. 开发约定

### 8.1 编码

- 所有 Java、Markdown、HTML 模板文件统一使用 UTF-8。
- 修改中文注释和中文文案前后都要用 UTF-8 读取检查。
- 不使用 PowerShell 默认编码写入文件。

### 8.2 新增一个 JX3 指令

建议步骤：

1. 在 `MethodEnum` 中确认或新增 JX3API 接口定义。
2. 在 `REGEX` 中新增指令匹配和对应 action。
3. 新增或更新 `jx3/http/data/**` DTO。
4. 新增 `Jx3BaseAction` 子类。
5. 实现 `getRequestParam`。
6. 实现 `dealAfterJx3ApiRequest`。
7. 文本返回可调用 `buildTextMessage` 或 `buildMessageByTemplate`。
8. 图片返回实现 `getResponseType=IMAGE`、`getTemplatePath`、`buildTemplateData`。
9. 在本地用样例 payload 验证。

### 8.3 不写入设计文档的内容

以下内容属于临时开发和审核阶段，不纳入设计文档：

- 临时写死的审批返回。
- 为平台审核准备的示例文案。
- 与真实接口无关的 mock 数据。

这些代码应在审核完成后删除或迁移到测试/模拟层。

### 8.4 本地 payload 回放

样例文件：

- `docs/testing/payloads/group-message-create.json`
- `docs/testing/payloads/interaction-create.json`

默认向本地 `8081` 端口回放：

```powershell
.\tools\replay-payload.ps1
```

指定 payload 和目标地址：

```powershell
.\tools\replay-payload.ps1 .\docs\testing\payloads\group-message-create.json http://localhost:8081/bot/message
```

脚本使用严格 UTF-8 读取 JSON，并在发送前执行 JSON 结构校验。

## 9. 变更历史

详细变更历史维护在 [PROJECT_DESIGN.md](PROJECT_DESIGN.md)。

## 10. 实施状态口径

项目使用四个独立口径描述完成度：

- HTTP 接口覆盖：`MethodEnum` 与官方返回 JSON 是否具备可执行的反序列化契约，当前覆盖 61 个接口。
- DTO 覆盖：61 个官方 HTTP contract 均使用类型化根 DTO，当前裸 `Map.class` 数量为 0。
- 群指令覆盖：接口是否已经在 `REGEX` 注册，并有明确的参数解析规则，当前注册 79 条指令（含帮助、群设置、群公告、调用统计和个人绑定指令）；61 个官方 HTTP contract 已全部覆盖，另有 2 个外部查询仍使用旧接口。
- 展示覆盖：已注册指令是否有经过业务确认的文本文案或 Vue 图片模板。当前 58 条查询已接入图片模板，其余已注册 JX3API 指令使用经过业务裁剪的精简文本返回。classpath 资源路由及模板字段修复已经通过 Playwright 回归和截图抽查。

只有四个口径都满足，才能把某个功能标记为完整的用户可用能力。`MethodEnum` 中存在接口定义或 DTO 能反序列化，不代表该接口已经开放为 QQ 群指令。

### 10.1 外部环境验收门槛

本地测试只能证明仓库内的结构、编排和渲染行为。下列项目必须保留可追踪的真实环境证据后，才能把对应能力标记为“已验收”；JX3API WebSocket 不在当前 HTTP 群指令验收范围内。

| 验收项 | 完成证据 | 当前状态 |
| --- | --- | --- |
| JX3API 在线查询 | 使用目标环境 token 对 61 个官方 contract 按免费/会员权限分组执行 smoke test，保存接口路径、HTTP 结果、官方 code 和脱敏后的 invocation id；不得保存 token 或完整用户数据 | 离线 contract、参数和返回测试完成；60 个非语音接口的显式 live runner 已具备，阿里语音独立验收；当前环境无凭证，真实 smoke test 待执行 |
| QQ webhook 与群消息 | 正式或沙箱机器人分别收到 `GROUP_MESSAGE_CREATE`、`GROUP_AT_MESSAGE_CREATE`，完成普通成员查询、管理员绑定区服及权限拒绝，并保存平台 trace id | 离线路由和权限测试完成；无副作用的 `/users/@me` 身份 live runner 已具备；当前环境无凭证，平台验收待执行 |
| QQ 被动与主动发送 | 真实验证 `msg_id` 回复、`event_id` 回复、消息引用、图片、Markdown + Keyboard、群公告主动消息及平台限流/拒绝反馈 | 离线结构与互斥规则完成；单模式、强确认的真实群发送 runner 已具备；当前环境无凭证和测试群参数，平台验收待执行 |
| 自有媒体域名 | 生成 PNG、上传目标 MinIO/S3、通过自有 HTTPS 域名匿名读取，并由 QQ 成功拉取和发送 | 存储 endpoint 与公网 `minio.public-url` 已分离；Vue PNG 生成、唯一对象上传、HTTPS 回读和 PNG 校验 runner 已具备；当前环境无凭证和媒体域名，真实回读与 QQ `IMAGE` 拉取待执行 |
| 阿里语音 | 使用正式凭证生成音频，经 QQ `file_type=3` 上传并发送群语音，日志确认凭证与音频 URL 未泄露 | 默认关闭，离线链路完成；语音生成 live runner 与 QQ 群消息 `AUDIO` live smoke 入口已具备，正式凭证联调待执行 |
| PostgreSQL 多实例行为 | 两个应用实例共享群绑定、个人绑定、指令冷却、主动消息频控、JX3 缓存和调用审计，验证竞争与租约恢复 | Mapper/服务并发语义测试完成；仅允许空的 `botjava_smoke_*` 专用 schema、带强确认的双上下文 live runner 已具备；当前环境无目标数据库凭证，真实验收待执行 |
| 可观测性 | 目标 Prometheus 采集 `bot_*` 指标，集中日志平台可按 `invocation_id` 检索且不出现敏感字段 | 本地指标、MDC、Prometheus scrape 契约与目标环境 Prometheus live runner 完成；日志平台接入待执行 |
| GitHub Actions | 目标仓库 workflow 在 Ubuntu 22.04 + Java 21 上成功完成 UTF-8、变更空白、Chromium 和完整 Maven 测试，记录 run URL 与 commit SHA | workflow 已编写并完成本地等价命令验证；远端首次运行待执行 |

外部验收记录应按 `docs/testing/external-acceptance-record-template.md` 脱敏整理后追加到变更历史，至少包含环境、日期、版本或 commit、结果和脱敏证据位置。不得为了让清单显示完成而在仓库中写入真实密钥、openid、消息正文或上游完整响应。
