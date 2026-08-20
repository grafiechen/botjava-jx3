# JX3API HTTP 接口清单

来源：`https://www.jx3api.com/openapi`，由官网 `https://www.jx3api.com/#/doc` 文档页加载。

说明：

- 本文件记录官方 HTTP 接口的当前路径、`x-level` 和 action 标识。
- `x-level=2` 的接口优先使用 `jx3api.api.api-v2-token`，未配置时回退 `jx3api.api.api-token`。
- `MethodEnum` 已同步当前 OpenAPI 的 78 条唯一路径；每条路径均具备 Action、REGEX 和可执行 contract。官方清单变化时，四层必须同步更新。
- `搜索区服`、`角色装备`、`副本进度`、音乐/聊天/成语等旧能力未在当前 OpenAPI 中出现时，继续按 legacy 能力单独验收。

| 分组 | 接口 | action | level | 请求路径 |
| --- | --- | --- | --- | --- |
| 百度贴吧 | 外观记录 | `item_records` | 1 | `/tieba/item/records` |
| 百度贴吧 | 百度贴吧 | `random` | 1 | `/tieba/random` |
| 百战记录 | 角色百战 | `records` | 2 | `/monster/records` |
| 百战记录 | 百战首领 | `weekly` | 2 | `/monster/weekly` |
| 帮战记录 | 帮战记录 | `records` | 0 | `/battle/records` |
| 赤兔查询 | 今日赤兔 | `records` | 2 | `/chitu/records` |
| 赤兔查询 | 本周赤兔 | `week_records` | 2 | `/chitu/week/records` |
| 大区状态 | 大区状态 | `status_check` | 0 | `/server/status/check` |
| 的卢拍卖 | 的卢拍卖 | `records` | 2 | `/steed/records` |
| 掉落统计 | 掉落统计 | `statistics` | 1 | `/reward/statistics` |
| 多玩语音 | 统战歪歪 | `statistics` | 0 | `/duowan/statistics` |
| 活动日历 | 活动日历 | `calendar` | 0 | `/active/calendar` |
| 活动日历 | 地图活动 | `celebs` | 0 | `/active/celebs` |
| 急速计算 | 急速计算 | `calculate` | 2 | `/skill/calculate` |
| 急速计算 | 技改记录 | `rework` | 0 | `/skill/rework` |
| 家园相关 | 家园鲜花 | `flower` | 0 | `/home/flower` |
| 家园相关 | 家园装饰 | `furniture` | 0 | `/home/furniture` |
| 家园相关 | 器物图谱 | `travel` | 0 | `/home/travel` |
| 角色名片 | 名片缓存 | `cached` | 2 | `/card/cached` |
| 角色名片 | 名片预设 | `preset` | 2 | `/card/preset` |
| 角色名片 | 随机名片 | `random` | 2 | `/card/random` |
| 角色名片 | 名片记录 | `record` | 2 | `/card/record` |
| 角色名片 | 名片历史 | `records` | 2 | `/card/records` |
| 角色信息 | 成就查询 | `achievement` | 2 | `/role/achievement` |
| 角色信息 | 角色详情 | `detail` | 1 | `/role/detail` |
| 科举搜索 | 科举答案 | `search` | 0 | `/exam/search` |
| 聊天记录 | 角色聊天 | `records` | 2 | `/chat/records` |
| 马场查询 | 马场预告 | `chat` | 2 | `/ranch/chat` |
| 马场查询 | 马场事件 | `records` | 1 | `/ranch/records` |
| 秘境方位 | 秘境方位 | `decrypt` | 1 | `/mech/decrypt` |
| 名剑相关 | 名剑排行 | `awesome` | 1 | `/arena/awesome` |
| 名剑相关 | 名剑战绩 | `recent` | 1 | `/arena/recent` |
| 名剑相关 | 名剑统计 | `schools` | 1 | `/arena/schools` |
| 排行榜单 | 跨服名剑 | `arena` | 2 | `/rank/arena` |
| 排行榜单 | 武林争霸 | `championship` | 2 | `/rank/championship` |
| 排行榜单 | 捕快荣誉 | `constable` | 2 | `/rank/constable` |
| 排行榜单 | 江湖浪客 | `outlaw` | 2 | `/rank/outlaw` |
| 排行榜单 | 排行统计 | `statistics` | 1 | `/rank/statistics` |
| 排行榜单 | 试炼之地 | `trials` | 1 | `/rank/trials` |
| 排行榜单 | 决斗挑战 | `wanted` | 2 | `/rank/wanted` |
| 骗子查询 | 骗子查询 | `detail` | 1 | `/fraud/detail` |
| 奇遇相关 | 奇遇汇总 | `collect` | 1 | `/event/collect` |
| 奇遇相关 | 未触发的 | `missing` | 1 | `/event/missing` |
| 奇遇相关 | 近期奇遇 | `recent` | 1 | `/event/recent` |
| 奇遇相关 | 奇遇记录 | `records` | 1 | `/event/records` |
| 奇遇相关 | 奇遇统计 | `statistics` | 1 | `/event/statistics` |
| 奇遇相关 | 奇遇攻略 | `strategy` | 1 | `/event/strategy` |
| 骚话语录 | 答案之书 | `answer` | 0 | `/saohua/answer` |
| 骚话语录 | 舔狗日志 | `content` | 0 | `/saohua/content` |
| 骚话语录 | 分类语录 | `context` | 2 | `/saohua/context` |
| 骚话语录 | 喝什么 | `drink` | 0 | `/saohua/drink` |
| 骚话语录 | 吃什么 | `eat` | 0 | `/saohua/eat` |
| 骚话语录 | 随机骚话 | `random` | 0 | `/saohua/random` |
| 骚话语录 | 渣男语录 | `zhanan` | 0 | `/saohua/zhanan` |
| 师徒相关 | 师徒列表 | `search` | 1 | `/mentor/search` |
| 世界首领 | 关隘首领 | `status` | 2 | `/castle/status` |
| 团队招募 | 团队招募 | `search` | 1 | `/recruit/search` |
| 物价交易 | 金价行情 | `demon` | 1 | `/trade/demon` |
| 物价交易 | 外观价格 | `item_records` | 1 | `/trade/item/records` |
| 物价交易 | 外观搜索 | `item_search` | 1 | `/trade/item/search` |
| 物价交易 | 成本计算 | `manufacture` | 2 | `/trade/manufacture` |
| 物价交易 | 物品价格 | `records` | 1 | `/trade/records` |
| 物价交易 | 编号搜索 | `wanbaolou` | 2 | `/trade/wanbaolou` |

编号搜索实现说明：官方 OpenAPI 定义为 `GET`，query 参数为角色编号 `id` 与 LV.2 `token`；群指令为 `编号搜索 角色编号` 或 `万宝楼 角色编号`，返回 Vue 模板图片。`zhanghaoId` 是上游内部两跳查询字段，不由机器人传入。
| 小吃小药 | 小吃小药 | `list` | 0 | `/food/list` |
| 心法配装 | 阵眼效果 | `matrix` | 1 | `/school/matrix` |
| 心法配装 | 配装搜索 | `search` | 2 | `/school/search` |
| 心法配装 | 资历榜单 | `seniority` | 1 | `/school/seniority` |
| 心法配装 | 技能信息 | `skills` | 1 | `/school/skills` |
| 心法配装 | 奇穴信息 | `talent` | 1 | `/school/talent` |
| 新闻公告 | 维护公告 | `announce` | 0 | `/news/announce` |
| 新闻公告 | 新闻资讯 | `records` | 0 | `/news/records` |
| 烟花事件 | 烟花记录 | `records` | 2 | `/firework/records` |
| 语音合成 | 语音合成 | `converter` | 0 | `/sound/converter` |
| 阵营竞拍 | 阵营拍卖 | `records` | 2 | `/auction/records` |
| 阵营沙盘 | 沙盘据点 | `records` | 1 | `/sand/records` |
| 阵营事件 | 阵营奉献 | `records` | 2 | `/fenxian/records` |
| 诛恶事件 | 诛恶事件 | `records` | 2 | `/wicked/records` |
| 资历分布 | 资历分布 | `achievement` | 2 | `/tuilan/achievement` |

## 实现状态（2026-08-14）

- 当前 OpenAPI 78 条唯一路径全部存在于 `MethodEnum`，缺失路径为 0；历史兼容路径继续单独标记，不冒充官方接口。
- 本轮补齐成就查询、名片预设、角色聊天、奇遇攻略、五类榜单、答案之书、分类语录、喝什么、吃什么、渣男语录、配装搜索、急速计算、成本计算和资历分布。
- 新增官方扩展接口均由独立 Action 组装参数；通用基类只负责安全裁剪文本。命名正则组由中央解析器自动发现，`page`、`camp`、`source`、`category`、`subclass` 等参数不会再被静默丢弃。
- `/trade/wanbaolou` 及本轮补充接口按官方定义使用 GET query；普通 token、LV.2 token 和 ticket 继续按接口等级与参数要求分别注入。
- 已对无需 ticket 的新增接口按每次至少 750ms 间隔进行低频在线抽测；`/card/preset`、`/school/search` 等需要 ticket 的接口只完成离线参数与反序列化契约，不能标记为真实环境已验收。