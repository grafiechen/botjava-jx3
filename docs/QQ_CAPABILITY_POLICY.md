# QQ 能力权限、频率与发布政策

本文记录项目对 QQ 官方能力的使用边界。平台准入、审核结果和动态频率限制以腾讯机器人开放平台控制台及官方文档为准；本文只声明仓库已经实现并可验证的本地约束。

官方入口：

- [腾讯机器人开放平台文档](https://bot.q.qq.com/wiki/develop/api/)
- [腾讯机器人 OpenAPI v2 文档](https://bot.q.qq.com/wiki/develop/api-v2/)

## 消息入口

| 事件 | 本地处理 | 权限边界 |
| --- | --- | --- |
| `GROUP_MESSAGE_CREATE` | 进入群指令统一执行链 | 指令根据 `CommandAccess` 判断公开或群管理权限 |
| `GROUP_AT_MESSAGE_CREATE` | 与普通群消息共用同一执行链 | 与普通群消息完全相同 |
| `INTERACTION_CREATE` | 进入类型化互动 handler | 只接受已注册业务前缀，不执行任意回调文本 |
| `GROUP_ADD_ROBOT` / `GROUP_DEL_ROBOT` | 同步群生命周期状态 | 不执行群指令 |
| `GROUP_MSG_RECEIVE` / `GROUP_MSG_REJECT` | 同步 QQ 主动消息授权 | 不改写管理员设置的本地开关 |

## 消息发送

| 发送方式 | 必需关联字段 | 项目约束 | 平台联调状态 |
| --- | --- | --- | --- |
| 消息被动回复 | `msg_id`，可选 `msg_seq` | `msg_seq` 限 1 至 5，可选择引用来源消息 | 离线结构测试完成，正式环境待验收 |
| 事件被动回复 | `event_id` | 禁止同时设置 `msg_id`、`msg_seq` 和消息引用 | 离线结构测试完成，正式环境待验收 |
| 群主动消息 | 不携带上述关联字段 | 必须同时开启本地开关并收到平台授权；默认每群 60 秒一条，数据库跨实例频控 | `群公告` 已接入，正式环境待验收 |

项目的 60 秒限制是自身保护策略，不代表 QQ 平台的全部频率规则。平台返回限流时统一映射为类型化 QQ OpenAPI 错误，业务代码不得自行无限重试。

## 返回类型

| `BotResponse` 类型 | `msg_type` | 当前状态 |
| --- | ---: | --- |
| `TEXT` | 0 | 群聊已接入 |
| `MARKDOWN` | 2 | 菜单与 Keyboard 已形成业务闭环，其他模板按平台准入扩展 |
| `ARK` | 3 | 类型与发送校验已具备，正式平台能力待验收 |
| `EMBED` | 4 | 群聊发送器明确拒绝，仅保留未来其他场景模型 |
| `IMAGE` / `IMAGE_URL` / `AUDIO_URL` / `MEDIA` | 7 | 图片链路已接入；语音默认关闭并等待正式凭证联调 |

真实群发送验收使用 `QqGroupMessageLiveSmokeIT`，必须同时提供 `QQ_GROUP_LIVE_SMOKE=true`、确认短语 `SEND_TO_TEST_GROUP`、测试群 openid 和单一模式。runner 每次最多发送一条最终群消息，不由普通测试或 CI 执行；`ACTIVE` 模式只验证 OpenAPI 主动消息传输，不替代应用数据库授权与跨实例频控验收。

## 指令权限和发布阶段

- `PUBLIC`：群成员均可调用。
- `GROUP_ADMIN`：只允许 payload 中 `author.member_role` 为 `owner` 或 `admin`；角色缺失时拒绝。
- `PRODUCTION`：正式能力，在正式运行模式开放。
- `EXPERIMENTAL`：只在 `TEST` 模式且目标群开启实验功能时开放。
- `REVIEW`：只在 `REVIEW` 模式开放；feature 分支不保存审核写死返回。
- 群管理员可以关闭非系统单项指令；系统指令不接受单项关闭。

`绑定区服` 属于 `GROUP_ADMIN` 本地数据库指令，按 `group_openid` 对整群生效。角色、常用角色和门派绑定属于个人数据库指令，按 `member_openid` 保存。

`群公告 内容` 属于 `GROUP_ADMIN` 正式指令。Action 只返回 `DeliveryMode.ACTIVE` 和公告文本，统一执行模板负责调用主动消息发送器；本地开关未开启、平台未授权或频控未结束时，模板改用来源消息进行被动提示，不把拒绝结果作为 webhook 异常抛出。

## 冷却与审计

- 群消息的同群同指令共享数据库冷却，不同群或不同指令互不影响。
- 本地指令默认 5 秒，外部调用默认 30 秒；可按 `REGEX` 枚举名单独覆盖。
- 权限、发布策略和参数校验失败不消耗冷却。
- 调用审计不保存消息原文、请求参数或上游响应，只保存固定指令名、状态、耗时和脱敏失败摘要。

## 发布检查

1. 确认 QQ 控制台已为目标环境开通所需事件和消息能力。
2. 使用 `PRODUCTION`、`TEST` 或 `REVIEW` 明确选择发布模式。
3. 在正式环境分别验收消息回复、事件回复、媒体上传及需要的平台模板能力。
4. 观察类型化错误、`invocationId` 日志和 Micrometer 指标，不通过日志打印 token、openid 或消息正文。
5. 平台规则变化时同步更新本文件、DTO、发送校验和离线 fixture。
