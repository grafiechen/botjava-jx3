# JX3API HTTP 接口清单

来源：`https://www.jx3api.com/#/doc` 左侧菜单树及对应 Markdown 文档。

说明：

- 本文件只记录 HTTP 接口，socket/ws 事件类接口本轮不处理。
- 完整请求参数与返回 JSON 示例已按文档名下载到 `target/jx3api-docs/*.md`，用于本地核对。
- 项目代码优先同步 `MethodEnum` 请求路径；DTO 按实际使用逐步类型化。
- 群指令覆盖关系记录在 `docs/testing/jx3api-command-coverage.json`，并由测试与 `REGEX`、61 个 HTTP contract 双向校验。
- 当前 61 个官方 HTTP 接口均已注册为群指令；“角色装备”和“副本进度”仍分别使用旧 `/data/role/attribute`、`/data/role/teamCdList` 接口并标记为 `LEGACY`。阿里语音默认关闭，配置独立第三方凭证并开启群实验功能后可用。

| 接口 | 文档 | 最新请求地址 |
| --- | --- | --- |
| 活动日历 | `doc/active.calendar.md` | `/data/active/calendar` |
| 活动月历 | `doc/active.list.calendar.md` | `/data/active/list/calendar` |
| 行侠事件 | `doc/active.celebs.md` | `/data/active/celebs` |
| 科举答题 | `doc/exam.search.md` | `/data/exam/search` |
| 家园鲜花 | `doc/home.flower.md` | `/data/home/flower` |
| 家园装饰 | `doc/home.furniture.md` | `/data/home/furniture` |
| 器物图谱 | `doc/home.travel.md` | `/data/home/travel` |
| 新闻资讯 | `doc/news.allnews.md` | `/data/news/allnews` |
| 维护公告 | `doc/news.announce.md` | `/data/news/announce` |
| 搜索区服 | `doc/master.search.md` | `/data/master/search` |
| 开服状态 | `doc/status.check.md` | `/data/status/check` |
| 技改记录 | `doc/skill.rework.md` | `/data/skill/rework` |
| 小药推荐 | `doc/school.foods.md` | `/data/school/foods` |
| 百战首领 | `doc/active.monster.md` | `/data/active/monster` |
| 扶摇预测 | `doc/active.next.event.md` | `/data/active/next/event` |
| 阵营拍卖 | `doc/auction.records.md` | `/data/auction/records` |
| 的卢记录 | `doc/steed.records.md` | `/data/steed/records` |
| 烟花记录 | `doc/show.records.md` | `/data/show/records` |
| 骗子查询 | `doc/fraud.detail.md` | `/data/fraud/detail` |
| 角色奇遇 | `doc/event.records.md` | `/data/event/records` |
| 未做奇遇 | `doc/event.unfinished.md` | `/data/event/unfinished` |
| 近期奇遇 | `doc/event.recent.md` | `/data/event/recent` |
| 奇遇统计 | `doc/event.statistics.md` | `/data/event/statistics` |
| 奇遇汇总 | `doc/event.collect.md` | `/data/event/collect` |
| 名剑战绩 | `doc/arena.recent.md` | `/data/arena/recent` |
| 名剑排行 | `doc/arena.awesome.md` | `/data/arena/awesome` |
| 名剑统计 | `doc/arena.schools.md` | `/data/arena/schools` |
| 团队招募 | `doc/recruit.search.md` | `/data/recruit/search` |
| 师徒系统 | `doc/mentor.search.md` | `/data/mentor/search` |
| 本服榜单 | `doc/rank.statistical.md` | `/data/rank/statistics` |
| 掉落统计 | `doc/reward.statistics.md` | `/data/reward/statistics` |
| 角色信息 | `doc/role.detail.md` | `/data/role/detail` |
| 角色名片 | `doc/card.record.md` | `/data/card/record` |
| 所有名片 | `doc/card.records.md` | `/data/card/records` |
| 随机名片 | `doc/card.random.md` | `/data/card/random` |
| 缓存名片 | `doc/card.cached.md` | `/data/card/cached` |
| 角色百战 | `doc/role.monster.md` | `/data/role/monster` |
| 心法阵眼 | `doc/school.matrix.md` | `/data/school/matrix` |
| 奇穴详情 | `doc/school.talent.md` | `/data/school/talent` |
| 技能详情 | `doc/school.skills.md` | `/data/school/skills` |
| 资历排行 | `doc/school.seniority.md` | `/data/school/seniority` |
| 阵营沙盘 | `doc/sand.records.md` | `/data/sand/records` |
| 阵营事件 | `doc/fenxian.records.md` | `/data/fenxian/records` |
| 诛恶事件 | `doc/smite.records.md` | `/data/smite/records` |
| 关隘首领 | `doc/mine.cart.md` | `/data/mine/cart` |
| 本日赤兔 | `doc/chitu.records.md` | `/data/chitu/records` |
| 本周赤兔 | `doc/chitu.week.records.md` | `/data/chitu/week/records` |
| 马场刷新 | `doc/ranch.records.md` | `/data/ranch/records` |
| 试炼排行 | `doc/rank.trials.md` | `/data/rank/trials` |
| 贴吧物价 | `doc/tieba.item.records.md` | `/data/tieba/item/records` |
| 金币价格 | `doc/trade.demon.md` | `/data/trade/demon` |
| 黑市物价 | `doc/trade.records.md` | `/data/trade/records` |
| 搜索物品 | `doc/trade.item.search.md` | `/data/trade/item/search` |
| 物品价格 | `doc/trade.item.records.md` | `/data/trade/item/records` |
| 帮战记录 | `doc/battle.records.md` | `/data/battle/records` |
| 副本解密 | `doc/mech.calculator.md` | `/data/mech/calculator` |
| 统战歪歪 | `doc/duowan.statistics.md` | `/data/duowan/statistics` |
| 八卦帖子 | `doc/tieba.random.md` | `/data/tieba/random` |
| 世界骚话 | `doc/saohua.random.md` | `/data/saohua/random` |
| 舔狗日记 | `doc/saohua.content.md` | `/data/saohua/content` |
| 阿里语音 | `doc/sound.converter.md` | `/data/sound/converter` |
