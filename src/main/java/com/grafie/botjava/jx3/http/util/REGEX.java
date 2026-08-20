package com.grafie.botjava.jx3.http.util;

import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.action.*;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.entity.dto.common.AuthorDto;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author grafie.chen
 * @since 2025/1/22  15:36
 */
public enum REGEX {
    CommandList("^指令$", CommandListAction.class, null,
            CommandGroup.BASIC, "指令列表", "查看所有已注册指令", "指令",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    Help("^(?:帮助|菜单|查询帮助)$", HelpAction.class, null,
            CommandGroup.BASIC, "帮助菜单", "查看当前可用指令", "帮助",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    BindServerCalendar("^绑定 (?<server>\\S+)$", BindServerAction.class, null,
            CommandGroup.BASIC, "绑定区服", "设置当前群默认服务器", "绑定 乾坤一掷",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    BindSchool("^绑定门派 (?<school>\\S+)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "绑定门派", "设置个人默认门派", "绑定门派 万花",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    UnbindSchool("^解绑门派$", UserBindingAction.class, null,
            CommandGroup.BASIC, "解绑门派", "清除个人默认门派", "解绑门派",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    BindRole("^绑定角色 (?<server>\\S+) (?<roleName>\\S+)(?: (?<school>\\S+))?$", UserBindingAction.class, null,
            CommandGroup.BASIC, "绑定角色", "设置个人默认角色，门派可选", "绑定角色 乾坤一掷 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    AddRole("^添加角色 (?<server>\\S+) (?<roleName>\\S+)(?: (?<school>\\S+))?$", UserBindingAction.class, null,
            CommandGroup.BASIC, "添加角色", "保存个人常用角色，门派可选", "添加角色 梦江南 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    ModifyRole("^修改角色 (?<server>\\S+) (?<roleName>\\S+) (?<school>\\S+)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "修改角色门派", "修改个人角色的门派，区服和角色名需要删除后重新添加", "修改角色 梦江南 角色名 万花",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    ShowRoleBinding("^(?:我的绑定|查看绑定|我的角色)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "查看角色", "查看个人常用角色和默认角色", "我的角色",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    UnbindRole("^(?:删除角色|解绑角色) (?<server>\\S+) (?<roleName>\\S+)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "删除角色", "删除个人角色，必须提供区服和角色名", "删除角色 梦江南 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    ScriptStatus("^脚本状态(?: (?<server>\\S+) (?<roleName>\\S+))?$", ScriptStatusAction.class, null,
            CommandGroup.BASIC, "脚本状态", "查看自己已绑定角色的脚本状态", "脚本状态 乾坤一掷 角色名",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION, 30),
    ScriptStatusUpdate("^脚本设置(?: (?<server>\\S+) (?<roleName>\\S+))? (?<name>\\S{1,100}) (?<text>[\\s\\S]{1,500})$", ScriptStatusAction.class, null,
            CommandGroup.BASIC, "脚本设置", "修改自己已绑定角色允许写入的脚本字段", "脚本设置 秘籍 完成",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION, 30),
    GroupSettings("^群设置 (?<value>查询|实验|主动消息) (?<value1>开启|关闭)$", GroupSettingsAction.class, null,
            CommandGroup.BASIC, "群功能设置", "开启或关闭群查询、实验与主动消息功能", "群设置 主动消息 开启",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupCommandSettings("^群指令(?: (?<name>\\S+) (?<value1>开启|关闭))?$", GroupSettingsAction.class, null,
            CommandGroup.BASIC, "单项指令设置", "查看或设置本群的单项指令开关", "群指令 开服状态 关闭",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupStatistics("^群统计(?: (?<num>\\d+))?$", GroupStatisticsAction.class, null,
            CommandGroup.BASIC, "群调用统计", "查看本群近期指令调用统计", "群统计 7",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    SystemStatus("^状态检查$", SystemStatusAction.class, null,
            CommandGroup.BASIC, "状态检查", "查看应用健康、监控接口和 JX3API WS 状态", "状态检查",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupPushList("^(?:推送列表|查看推送)$", GroupPushSubscriptionAction.class, null,
            CommandGroup.BASIC, "推送列表", "查看本群所有实时与定时推送任务", "推送列表",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupPushEnable("^开启推送 (?<name>[\\s\\S]{1,100})$", GroupPushSubscriptionAction.class, null,
            CommandGroup.BASIC, "开启推送", "开启本群指定的主动推送任务", "开启推送 开服状态",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupPushDisable("^关闭推送 (?<name>[\\s\\S]{1,100})$", GroupPushSubscriptionAction.class, null,
            CommandGroup.BASIC, "关闭推送", "关闭本群指定的主动推送任务", "关闭推送 开服状态",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),    DailyPushFieldList("^日常推送字段$", DailyPushFieldAction.class, null,
            CommandGroup.BASIC, "日常推送字段", "查看本群日常进度图片的动态字段", "日常推送字段",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    DailyPushFieldAdd("^日常推送字段添加 (?<name>\\S{1,100}) (?<name1>\\S{1,100})(?: (?<type>TEXT|INTEGER|DECIMAL|BOOLEAN|DATETIME))?(?: (?<num>\\d{1,5}))?$", DailyPushFieldAction.class, null,
            CommandGroup.BASIC, "添加日常推送字段", "添加或修改本群日常进度图片字段", "日常推送字段添加 每日签到任务 上次日常完成 DATETIME 10",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    DailyPushFieldDelete("^日常推送字段删除 (?<name>\\S{1,100})$", DailyPushFieldAction.class, null,
            CommandGroup.BASIC, "删除日常推送字段", "从本群日常进度图片移除字段", "日常推送字段删除 精力",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    GroupRequirementList("^(?:查看需求列表|需求列表)$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "需求列表", "查看本群创建的需求列表", "需求列表",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementCreate("^创建需求 (?<name>\\S{1,80})$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "创建需求", "创建一个群需求主题", "创建需求 奶花配装",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementRename("^修改需求 (?<name>\\S{1,80}) (?<name1>\\S{1,80})$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "修改需求", "修改自己创建的需求名称", "修改需求 奶花配装 奶秀配装",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementStatus("^需求状态 (?<name>\\S{1,80}) (?<item>[\\s\\S]{1,1000}) (?<status>\\S{1,100})$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "需求状态", "修改需求下某条追加内容的状态", "需求状态 奶花配装 acb 完成",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementAppend("^追加需求 (?<name>\\S{1,80})(?: (?<text>[\\s\\S]{0,1000}))?$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "追加需求", "给需求追加自己的描述", "追加需求 奶花配装 我可以做",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementCancel("^取消需求 (?<name>\\S{1,80}) (?<num>\\d+)$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "取消需求", "按序号删除自己在需求下追加的内容", "取消需求 奶花配装 1",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementDelete("^删除需求 (?<name>\\S{1,80})$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "删除需求", "删除自己创建的需求", "删除需求 奶花配装",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupRequirementDetail("^需求详情 (?<name>\\S{1,80})$", GroupRequirementAction.class, null,
            CommandGroup.BASIC, "需求详情", "查看需求标题、追加人和描述", "需求详情 奶花配装",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    GroupAnnouncement("^群公告 (?<text>[\\s\\S]{1,500})$", GroupAnnouncementAction.class, null,
            CommandGroup.BASIC, "群公告", "通过群主动消息发布管理员公告", "群公告 今晚八点开团",
            CommandAccess.GROUP_ADMIN, CommandAvailability.PRODUCTION),
    ActiveCurrent("^日常$|^日常 (?<server>.+?)(?: (?<value>\\S+))?$", ActiveCurrentAction.class, MethodEnum.DATA_ACTIVE_CALENDAR,
            CommandGroup.FREE, "活动日历", "查询日常活动", "日常 乾坤一掷"),
    ActiveListCalendar("^月历(?: (?<num>\\d+))?$", ActiveListCalendarAction.class, MethodEnum.DATA_ACTIVE_LIST_CALENDAR,
            CommandGroup.FREE, "活动月历", "查询近期活动月历", "月历 15"),
    ActiveCelebrities("^行侠 (?<name>楚天社|云从社|披风会)$", ActiveCelebritiesAction.class, MethodEnum.DATA_ACTIVE_CELEBS,
            CommandGroup.FREE, "行侠事件", "查询行侠社群活动", "行侠 楚天社"),
    ExamAnswer("^科举 (?<subject>.+)$", ExamAnswerAction.class, MethodEnum.DATA_EXAM_ANSWER,
            CommandGroup.FREE, "科举答题", "搜索科举题目答案", "科举 古琴有几根弦"),
    HomeFlower("^鲜花(?: (?<server>\\S+))?(?: (?<name>\\S+))?$", HomeFlowerAction.class, MethodEnum.DATA_HOME_FLOWER,
            CommandGroup.FREE, "家园鲜花", "查询服务器鲜花价格", "鲜花 乾坤一掷 绣球花"),
    HomeFurniture("^家具 (?<name>.+)$", HomeFurnitureAction.class, MethodEnum.DATA_HOME_FURNITURE,
            CommandGroup.FREE, "家园装饰", "查询家园装饰详情", "家具 龙门香梦"),
    HomeTravel("^器物 (?<name>\\S+)$", HomeTravelAction.class, MethodEnum.DATA_HOME_TRAVEL,
            CommandGroup.FREE, "器物图谱", "查询地图器物产出", "器物 万花"),
    NewsAllNews("^新闻(?: (?<limit>\\d+))?$", NewsAllNewsAction.class, MethodEnum.DATA_WEB_NEWS_ALLNEWS,
            CommandGroup.FREE, "新闻资讯", "查询最新官方新闻", "新闻 3"),
    ServerMaster("^区服 (?<name>\\S+)$", ServerMasterAction.class, MethodEnum.DATA_SERVER_MASTER,
            CommandGroup.FREE, "搜索区服", "搜索主服务器和附属服务器", "区服 双梦镇"),
    SkillRework("^技改$", SkillReworkAction.class, MethodEnum.DATA_SKILL_REWORK,
            CommandGroup.FREE, "技改记录", "查询近期武学调整", "技改"),
    SchoolFoods("^小药$", SchoolFoodsAction.class, MethodEnum.DATA_SCHOOL_FOODS,
            CommandGroup.FREE, "小药推荐", "查询各心法小药推荐", "小药"),
    ServerCheck("^(?:开服|开服状态)(?: (?<server>\\S+))?$", ServerCheckAction.class, MethodEnum.DATA_SERVER_CHECK,
            CommandGroup.FREE, "开服状态", "查询服务器状态", "开服 乾坤一掷"),
    TradeDemon("^金价$|^金价 (?<server>\\S+)$", TradeDemonAction.class, MethodEnum.DATA_TRADE_DEMON,
            CommandGroup.MEMBER, "金币价格", "查询服务器金币价格", "金价 乾坤一掷"),
    TradeRecord("^物价 (?<value>\\S+)$", TradeRecordAction.class, MethodEnum.DATA_TRADE_RECORD,
            CommandGroup.MEMBER, "物品价格", "查询物品价格趋势", "物价 五行石"),
    AppearanceNameAliasAdd("^外观名称追加 (?<name>\\S{1,100}) (?<text>[\\s\\S]{1,500})$", AppearanceNameAliasAction.class, null,
            CommandGroup.MEMBER, "外观名称追加", "给外观正式名称追加别名", "外观名称追加 金发·因陀罗 猴金 后进",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    AppearanceNameAliasList("^外观名称列表$", AppearanceNameAliasAction.class, null,
            CommandGroup.MEMBER, "外观名称列表", "查看本群外观名称别名", "外观名称列表",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    AppearanceNameAliasQuery("^外观名称查询 (?<name>\\S{1,100})$", AppearanceNameAliasAction.class, null,
            CommandGroup.MEMBER, "外观名称查询", "通过外观正式名称或别名查询映射", "外观名称查询 猴金",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    AppearanceNameAliasPendingList("^外观名称待审核列表$", AppearanceNameAliasAction.class, null,
            CommandGroup.MEMBER, "外观名称待审核列表", "查看本群待审核的外观名称别名", "外观名称待审核列表",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    AppearanceNameAliasApprove("^外观名称审核 (?<name>\\S{1,100})(?: (?<text>[\\s\\S]{1,500}))?$", AppearanceNameAliasAction.class, null,
            CommandGroup.MEMBER, "外观名称审核", "审核通过整组外观名称或指定别名", "外观名称审核 金发·因陀罗 猴金",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    AppearanceNameAliasDelete("^外观名称删除 (?<name>\\S{1,100})(?: (?<text>[\\s\\S]{1,500}))?$", AppearanceNameAliasAction.class, null,
            CommandGroup.MEMBER, "外观名称删除", "删除整组外观名称或指定别名", "外观名称删除 金发·因陀罗 猴金",
            CommandAccess.PUBLIC, CommandAvailability.PRODUCTION),
    SaohuaRandom("^骚话$", SaohuaRandomAction.class, MethodEnum.DATA_SAOHUA_RANDOM,
            CommandGroup.OTHER, "世界骚话", "随机返回一句趣味内容", "骚话"),
    NewsAnnounce("^更新$|^公告$|^更新公告$", NewsAnnounceAction.class, MethodEnum.DATA_WEB_NEWS_ANNOUNCE,
            CommandGroup.FREE, "维护公告", "查询最新维护公告", "更新公告"),
    LuckAdventure("^奇遇(?: (?<value>\\S+))?$|^奇遇 (?<server>\\S+) (?<value1>\\S+)$", LuckAdventureAction.class, MethodEnum.DATA_LUCK_ADVENTURE,
            CommandGroup.MEMBER, "角色奇遇", "查询角色奇遇记录", "奇遇 乾坤一掷 角色名"),
    LuckStatistical("^汇总$|^汇总 (?<server>\\S+)$", LuckStatisticalAction.class, MethodEnum.DATA_LUCK_STATISTICAL,
            CommandGroup.MEMBER, "奇遇统计", "查询服务器奇遇统计", "汇总 乾坤一掷"),
    AuctionRecords("^拍卖(?: (?<server>\\S+))?(?: (?<name>\\S+))?$", AuctionRecordsAction.class, MethodEnum.DATA_AUCTION_RECORDS,
            CommandGroup.MEMBER, "阵营拍卖", "查询服务器阵营拍卖记录", "拍卖 乾坤一掷 玄晶"),
    HorseRecords("^的卢(?: (?<server>\\S+))?$", HorseRecordsAction.class, MethodEnum.DATA_HORSE_RECORDS,
            CommandGroup.MEMBER, "的卢记录", "查询服务器的卢记录", "的卢 乾坤一掷"),
    FraudDetail("^骗子 (?<uid>\\d{5,12})$", FraudDetailAction.class, MethodEnum.DATA_FRAUD_DETAIL,
            CommandGroup.MEMBER, "骗子查询", "按 QQ 号查询避雷记录", "骗子 570790267"),
    LuckUnfinished("^未做奇遇(?: (?<value>\\S+))?$|^未做奇遇 (?<server>\\S+) (?<value1>\\S+)$", LuckUnfinishedAction.class, MethodEnum.DATA_LUCK_UNFINISHED,
            CommandGroup.MEMBER, "未做奇遇", "查询角色尚未触发的奇遇", "未做奇遇 乾坤一掷 角色名"),
    MatchAwesome("^名剑排行(?: (?<mode>22|33|55))?(?: (?<limit>\\d+))?$", MatchAwesomeAction.class, MethodEnum.DATA_MATCH_AWESOME,
            CommandGroup.MEMBER, "名剑排行", "查询名剑大会排行榜", "名剑排行 33 10"),
    MatchSchools("^名剑统计(?: (?<mode>22|33|55))?$", MatchSchoolsAction.class, MethodEnum.DATA_MATCH_SCHOOLS,
            CommandGroup.MEMBER, "名剑统计", "查询名剑门派统计", "名剑统计 33"),
    RoleDetailed("^角色(?: (?<value>\\S+))?$|^角色 (?<server>\\S+) (?<value1>\\S+)$", RoleDetailedAction.class, MethodEnum.DATA_ROLE_DETAILED,
            CommandGroup.MEMBER, "角色信息", "查询角色基础信息", "角色 乾坤一掷 角色名"),
    RoleMonster("^角色百战(?: (?<value>\\S+))?$|^角色百战 (?<server>\\S+) (?<value1>\\S+)$", RoleMonsterAction.class, MethodEnum.DATA_ROLE_MONSTER,
            CommandGroup.MEMBER, "角色百战", "查询角色百战进度", "角色百战 乾坤一掷 角色名"),
    SchoolMatrix("^阵眼 (?<name>\\S+)$", SchoolMatrixAction.class, MethodEnum.DATA_SCHOOL_MATRIX,
            CommandGroup.MEMBER, "心法阵眼", "查询心法阵眼效果", "阵眼 花间游"),
    SchoolSkills("^技能 (?<name>\\S+)$", SchoolSkillsAction.class, MethodEnum.DATA_SCHOOL_SKILL,
            CommandGroup.MEMBER, "技能详情", "查询心法技能详情", "技能 花间游"),
    ActiveNextEvent("^扶摇(?: (?<server>\\S+))?$", ActiveNextEventAction.class, MethodEnum.DATA_ACTIVE_NEXT_EVENT,
            CommandGroup.MEMBER, "扶摇预测", "查询服务器扶摇事件预测", "扶摇 乾坤一掷"),
    LuckRecent("^近期奇遇(?: (?<server>\\S+))?$", LuckRecentAction.class, MethodEnum.DATA_LUCK_RECENT,
            CommandGroup.MEMBER, "近期奇遇", "查询服务器近期奇遇", "近期奇遇 乾坤一掷"),
    LuckCollect("^奇遇汇总(?: (?<server>\\S+))?(?: (?<num>\\d+))?$", LuckCollectAction.class, MethodEnum.DATA_LUCK_COLLECT,
            CommandGroup.MEMBER, "奇遇汇总", "汇总服务器近期奇遇", "奇遇汇总 乾坤一掷 7"),
    MemberTeacher("^师徒 (?<type>1|2)(?: (?<server>\\S+))?(?: (?<keyword>\\S+))?$", MemberTeacherAction.class, MethodEnum.DATA_MEMBER_TEACHER,
            CommandGroup.MEMBER, "师徒系统", "查询服务器师徒招募", "师徒 1 乾坤一掷 PVE"),
    RankStatistical("^榜单 (?<name>\\S+)$|^榜单 (?<server>\\S+) (?<name1>\\S+)$", RankStatisticalAction.class, MethodEnum.DATA_RANK_STATISTICAL,
            CommandGroup.MEMBER, "本服榜单", "查询服务器风云榜单", "榜单 乾坤一掷 名士五十强"),
    ValuablesStatistical("^掉落 (?<value>\\S+)$|^掉落 (?<server>\\S+) (?<value1>\\S+)$", ValuablesStatisticalAction.class, MethodEnum.DATA_VALUABLES_STATISTICAL,
            CommandGroup.MEMBER, "掉落统计", "查询服务器稀有掉落", "掉落 乾坤一掷 玄晶"),
    RoleShowCard("^名片(?: (?<value>\\S+))?$|^名片 (?<server>\\S+) (?<value1>\\S+)$", RoleShowCardAction.class, MethodEnum.DATA_ROLE_SHOW_CARD,
            CommandGroup.MEMBER, "角色名片", "查询角色当前名片", "名片 乾坤一掷 角色名"),
    RoleShowCards("^所有名片(?: (?<value>\\S+))?$|^所有名片 (?<server>\\S+) (?<value1>\\S+)$", RoleShowCardAction.class, MethodEnum.DATA_ROLE_SHOW_CARDS,
            CommandGroup.MEMBER, "所有名片", "查询角色全部名片", "所有名片 乾坤一掷 角色名"),
    RoleShowRandom("^随机名片(?: (?<server>\\S+))?(?: (?<body>\\S+))?(?: (?<force>\\S+))?$", RoleShowRandomAction.class, MethodEnum.DATA_ROLE_SHOW_RANDOM,
            CommandGroup.MEMBER, "随机名片", "按区服体型和门派随机名片", "随机名片 乾坤一掷 萝莉 万花"),
    RoleShowCached("^缓存名片(?: (?<value>\\S+))?$|^缓存名片 (?<server>\\S+) (?<value1>\\S+)$", RoleShowCachedAction.class, MethodEnum.DATA_ROLE_SHOW_CACHED,
            CommandGroup.MEMBER, "缓存名片", "查询角色缓存名片", "缓存名片 乾坤一掷 角色名"),
    SchoolSeniority("^资历(?: (?<server>\\S+))?(?: (?<school>\\S+))?$", SchoolSeniorityAction.class, MethodEnum.DATA_SCHOOL_SENIORITY,
            CommandGroup.MEMBER, "资历排行", "查询服务器门派资历排行", "资历 乾坤一掷 万花"),
    ServerEvent("^阵营事件$", ServerEventAction.class, MethodEnum.DATA_SERVER_EVENT,
            CommandGroup.MEMBER, "阵营事件", "查询跨服阵营事件", "阵营事件"),
    ServerAntivice("^诛恶事件$", ServerAntiviceAction.class, MethodEnum.DATA_SERVER_ANTIVICE,
            CommandGroup.MEMBER, "诛恶事件", "查询近期诛恶事件", "诛恶事件"),
    MineCart("^关隘首领$", MineCartAction.class, MethodEnum.DATA_MINE_CART,
            CommandGroup.MEMBER, "关隘首领", "查询关隘首领记录", "关隘首领"),
    ChituRecords("^本日赤兔$", ChituRecordsAction.class, MethodEnum.DATA_CHITU_RECORDS,
            CommandGroup.MEMBER, "本日赤兔", "查询本日赤兔记录", "本日赤兔"),
    ChituWeekRecords("^本周赤兔$", ChituWeekRecordsAction.class, MethodEnum.DATA_CHITU_WEEK_RECORDS,
            CommandGroup.MEMBER, "本周赤兔", "查询本周赤兔记录", "本周赤兔"),
    HorseEvent("^马场(?: (?<server>\\S+))?$", HorseEventAction.class, MethodEnum.DATA_HORSE_EVENT,
            CommandGroup.MEMBER, "马场刷新", "查询服务器马场刷新", "马场 乾坤一掷"),
    RankTrials("^试炼 (?<value>\\S+)$|^试炼 (?<server>\\S+) (?<value1>\\S+)$", RankTrialsAction.class, MethodEnum.DATA_RANK_TRIALS,
            CommandGroup.MEMBER, "试炼排行", "查询心法试炼排行", "试炼 乾坤一掷 花间游"),
    TiebaItemRecords("^贴吧物价 (?<value>\\S+)$|^贴吧物价 (?<server>\\S+) (?<value1>\\S+)$", TiebaItemRecordsAction.class, MethodEnum.DATA_TIEBA_ITEM_RECORDS,
            CommandGroup.MEMBER, "贴吧物价", "查询物品贴吧价格记录", "贴吧物价 乾坤一掷 狐金"),
    TradeRecords("^黑市物价 (?<value>\\S+)$|^黑市物价 (?<server>\\S+) (?<value1>\\S+)$", TradeRecordsAction.class, MethodEnum.DATA_TRADE_RECORDS,
            CommandGroup.MEMBER, "黑市物价", "查询物品黑市价格", "黑市物价 乾坤一掷 狐金"),
    TradeItemSearch("^搜索物品 (?<name>.+)$", TradeItemSearchAction.class, MethodEnum.DATA_TRADE_ITEM_SEARCH,
            CommandGroup.MEMBER, "搜索物品", "模糊搜索物品", "搜索物品 十五"),
    Wanbaolou("^(?:编号搜索|万宝楼) (?<value>\\d{6,30})$", WanbaolouAction.class, MethodEnum.DATA_TRADE_WANBAOLOU,
            CommandGroup.MEMBER, "万宝楼编号搜索", "按角色编号查询万宝楼账号详情", "编号搜索 1405435120446099456"),
    RoleAchievement("^成就查询 (?<server>\\S+) (?<roleName>\\S+) (?<name>\\S+)$", RoleAchievementAction.class, MethodEnum.DATA_ROLE_ACHIEVEMENT,
            CommandGroup.MEMBER, "成就查询", "查询角色是否完成指定成就", "成就查询 乾坤一掷 角色名 阴阳两界"),
    CardPreset("^名片预设 (?<server>\\S+) (?<roleName>\\S+)$", CardPresetAction.class, MethodEnum.DATA_CARD_PRESET,
            CommandGroup.MEMBER, "名片预设", "查询角色名片预设", "名片预设 乾坤一掷 角色名"),
    ChatRecords("^角色聊天 (?<server>\\S+) (?<roleName>\\S+)(?: (?<limit>\\d+))?(?: (?<page>\\d+))?$", ChatRecordsAction.class, MethodEnum.DATA_CHAT_RECORDS,
            CommandGroup.MEMBER, "角色聊天", "查询角色聊天记录", "角色聊天 乾坤一掷 角色名 20 1"),
    EventStrategy("^奇遇攻略 (?<name>\\S+)$", EventStrategyAction.class, MethodEnum.DATA_EVENT_STRATEGY,
            CommandGroup.MEMBER, "奇遇攻略", "查询指定奇遇攻略", "奇遇攻略 阴阳两界"),
    RankArena("^跨服名剑(?: (?<server>\\S+))?(?: (?<mode>0|1|2))?$", RankArenaAction.class, MethodEnum.DATA_RANK_ARENA,
            CommandGroup.MEMBER, "跨服名剑", "查询跨服名剑榜", "跨服名剑 乾坤一掷 0"),
    RankChampionship("^武林争霸(?: (?<server>\\S+))?(?: (?<camp>1|2))?$", RankChampionshipAction.class, MethodEnum.DATA_RANK_CHAMPIONSHIP,
            CommandGroup.MEMBER, "武林争霸", "查询武林争霸榜", "武林争霸 乾坤一掷 1"),
    RankConstable("^捕快荣誉(?: (?<server>\\S+))?$", RankConstableAction.class, MethodEnum.DATA_RANK_CONSTABLE,
            CommandGroup.MEMBER, "捕快荣誉", "查询捕快荣誉榜", "捕快荣誉 乾坤一掷"),
    RankOutlaw("^江湖浪客(?: (?<server>\\S+))?$", RankOutlawAction.class, MethodEnum.DATA_RANK_OUTLAW,
            CommandGroup.MEMBER, "江湖浪客", "查询江湖浪客榜", "江湖浪客 乾坤一掷"),
    RankWanted("^决斗挑战(?: (?<server>\\S+))?(?: (?<mode>1|2))?$", RankWantedAction.class, MethodEnum.DATA_RANK_WANTED,
            CommandGroup.MEMBER, "决斗挑战", "查询决斗挑战榜", "决斗挑战 乾坤一掷 1"),
    SaohuaAnswer("^答案之书$", SaohuaAnswerAction.class, MethodEnum.DATA_SAOHUA_ANSWER,
            CommandGroup.OTHER, "答案之书", "随机获取一个答案", "答案之书"),
    SaohuaContext("^分类语录 (?<name>疯狂星期四|彩虹屁|毒鸡汤|朋友圈)$", SaohuaContextAction.class, MethodEnum.DATA_SAOHUA_CONTEXT,
            CommandGroup.OTHER, "分类语录", "获取指定分类语录", "分类语录 疯狂星期四"),
    SaohuaDrink("^喝什么$", SaohuaDrinkAction.class, MethodEnum.DATA_SAOHUA_DRINK,
            CommandGroup.OTHER, "喝什么", "随机推荐饮品", "喝什么"),
    SaohuaEat("^吃什么$", SaohuaEatAction.class, MethodEnum.DATA_SAOHUA_EAT,
            CommandGroup.OTHER, "吃什么", "随机推荐食物", "吃什么"),
    SaohuaZhanan("^渣男语录$", SaohuaZhananAction.class, MethodEnum.DATA_SAOHUA_ZHANAN,
            CommandGroup.OTHER, "渣男语录", "随机返回一条语录", "渣男语录"),
    SchoolSearch("^配装搜索 (?<name>\\S+)(?: (?<mode>\\S+))?$", SchoolSearchAction.class, MethodEnum.DATA_SCHOOL_SEARCH,
            CommandGroup.MEMBER, "配装搜索", "按门派和玩法搜索配装", "配装搜索 万花 PVE"),
    SkillCalculate("^急速计算(?: (?<cooldown>\\d+(?:\\.\\d+)?))?$", SkillCalculateAction.class, MethodEnum.DATA_SKILL_CALCULATE,
            CommandGroup.MEMBER, "急速计算", "按技能 CD 计算急速档位", "急速计算 1.5"),
    TradeManufacture("^成本计算 (?<server>\\S+) (?<name>\\S+)(?: (?<source>0|1))?$", TradeManufactureAction.class, MethodEnum.DATA_TRADE_MANUFACTURE,
            CommandGroup.MEMBER, "成本计算", "查询成品制作成本", "成本计算 乾坤一掷 成品名 0"),
    TuilanAchievement("^资历分布 (?<server>\\S+) (?<roleName>\\S+)(?: (?<category>[1-3]))?(?: (?<subclass>\\S+))?$", TuilanAchievementAction.class, MethodEnum.DATA_TUILAN_ACHIEVEMENT,
            CommandGroup.MEMBER, "资历分布", "查询角色资历分类分布", "资历分布 乾坤一掷 角色名 1"),
    BattleRecords("^帮战(?: (?<server>\\S+))?$", BattleRecordsAction.class, MethodEnum.DATA_BATTLE_RECORDS,
            CommandGroup.MEMBER, "帮战记录", "查询服务器帮战记录", "帮战 乾坤一掷"),
    MechCalculator("^副本解密$", MechCalculatorAction.class, MethodEnum.DATA_MECH_CALCULATOR,
            CommandGroup.MEMBER, "副本解密", "查询一之窟解密结果", "副本解密"),
    DuowanStatistics("^统战(?: (?<server>\\S+))?$", DuowanStatisticsAction.class, MethodEnum.DATA_DUOWAN_STATISTICS,
            CommandGroup.MEMBER, "统战歪歪", "查询服务器统战频道", "统战 乾坤一掷"),
    TiebaRandom("^八卦 (?<tags>818|616|鬼网三|鬼网3|树洞|记录|教程|街拍|故事|避雷|吐槽|提问)(?: (?<server>\\S+))?$", TiebaRandomAction.class, MethodEnum.DATA_TIEBA_RANDOM,
            CommandGroup.OTHER, "八卦帖子", "随机查询指定分类帖子", "八卦 818 乾坤一掷"),
    SaohuaContent("^舔狗日记$", SaohuaContentAction.class, MethodEnum.DATA_SAOHUA_CONTENT,
            CommandGroup.OTHER, "舔狗日记", "随机返回一条舔狗日记", "舔狗日记"),
    SoundConverter("^语音 (?<text>.{1,200})$", SoundConverterAction.class, MethodEnum.DATA_SOUND_CONVERTER,
            CommandGroup.OTHER, "阿里语音", "将文字转换为群语音", "语音 剑网三真好玩",
            CommandAccess.PUBLIC, CommandAvailability.EXPERIMENTAL),
    MatchRecent("^战绩(?: (?<value>\\S+))?$|^战绩 (?<server>\\S+) (?<value1>\\S+)$", MatchRecentAction.class, MethodEnum.DATA_MATCH_RECENT,
            CommandGroup.MEMBER, "名剑战绩", "查询角色近期名剑战绩", "战绩 乾坤一掷 角色名"),
    RoleAttribute("^(?:装备|属性)(?: (?<value>\\S+))?$|^(?:装备|属性) (?<server>\\S+) (?<value1>\\S+)$", RoleAttributeAction.class, MethodEnum.DATA_ROLE_ATTRIBUTE,
            CommandGroup.MEMBER, "角色装备", "查询角色装备与属性", "装备 乾坤一掷 角色名"),
    WatchRecord("^烟花(?: (?<value>\\S+))?$|^烟花 (?<server>\\S+) (?<value1>\\S+)$", WatchRecordAction.class, MethodEnum.DATA_WATCH_RECORD,
            CommandGroup.MEMBER, "烟花记录", "查询角色烟花记录", "烟花 乾坤一掷 角色名"),
    MemberRecruit("^招募$|^招募 (?<server>\\S+)$|^招募 (?<server1>\\S+) (?<value>\\S+)$", MemberRecruitAction.class, MethodEnum.DATA_MEMBER_RECRUIT,
            CommandGroup.MEMBER, "团队招募", "查询服务器团队招募", "招募 乾坤一掷"),
    ServerSand("^沙盘$|^沙盘 (?<server>\\S+)$", ServerSandAction.class, MethodEnum.DATA_SERVER_SAND,
            CommandGroup.MEMBER, "阵营沙盘", "查询服务器阵营沙盘", "沙盘 乾坤一掷"),
    ActiveMonster("^百战$|^百战 (?<server>\\S+)$", ActiveMonsterAction.class, MethodEnum.DATA_ACTIVE_MONSTER,
            CommandGroup.MEMBER, "百战首领", "查询本周百战首领", "百战"),
    SchoolForce("^奇穴 (?<value>\\S+)$", SchoolForceAction.class, MethodEnum.DATA_SCHOOL_FORCE,
            CommandGroup.MEMBER, "奇穴详情", "查询心法奇穴详情", "奇穴 冰心诀"),
    DungeonRecord("^副本(?: (?<value>\\S+))?$|^副本 (?<server>\\S+) (?<value1>\\S+)$", DungeonRecordAction.class, MethodEnum.DATA_ROLE_CDLIST,
            CommandGroup.MEMBER, "副本进度", "查询角色副本进度", "副本 乾坤一掷 角色名"),
    // 这三个需要特殊处理
    LuckAdventurePs("^水墨圈圈 (?<value>\\S+)$", LuckAdventurePsAction.class, null,
            CommandGroup.OTHER, "水墨圈圈", "生成奇遇主题图片", "水墨圈圈 角色名",
            CommandAccess.PUBLIC, CommandAvailability.EXPERIMENTAL),
    DpsCompute("^[dD][pP][sS](?: (?<server>\\S+) (?<roleName>\\S+)(?: (?<loop>\\S+))?| (?<value>\\S+))?$", DpsComputeAction.class, null,
            CommandGroup.OTHER, "DPS 计算", "计算角色 DPS", "DPS 乾坤一掷 角色名",
            CommandAccess.PUBLIC, CommandAvailability.EXPERIMENTAL, 30),
    ChiGua("^吃瓜 (?<value>\\S+)$", ChiGuaAction.class, null,
            CommandGroup.OTHER, "吃瓜查询", "查询相关趣闻", "吃瓜 关键词",
            CommandAccess.PUBLIC, CommandAvailability.EXPERIMENTAL);

    private static final Pattern NAMED_GROUP_PATTERN = Pattern.compile("\\(\\?<([A-Za-z][A-Za-z0-9]*)>");

    private final Pattern pattern;
    private final Class<? extends Jx3BaseAction> baseAction;
    private final MethodEnum methodEnum;
    private final CommandGroup commandGroup;
    private final String displayName;
    private final String description;
    private final String example;
    private final CommandAccess commandAccess;
    private final CommandAvailability commandAvailability;
    private final int defaultCooldownSeconds;

    REGEX(String regex, Class<? extends Jx3BaseAction> baseAction, MethodEnum methodEnum,
          CommandGroup commandGroup, String displayName, String description, String example) {
        this(regex, baseAction, methodEnum, commandGroup, displayName, description, example, CommandAccess.PUBLIC);
    }

    REGEX(String regex, Class<? extends Jx3BaseAction> baseAction, MethodEnum methodEnum,
          CommandGroup commandGroup, String displayName, String description, String example,
          CommandAccess commandAccess) {
        this(regex, baseAction, methodEnum, commandGroup, displayName, description, example,
                commandAccess, CommandAvailability.PRODUCTION);
    }

    REGEX(String regex, Class<? extends Jx3BaseAction> baseAction, MethodEnum methodEnum,
          CommandGroup commandGroup, String displayName, String description, String example,
          CommandAccess commandAccess, CommandAvailability commandAvailability) {
        this(regex, baseAction, methodEnum, commandGroup, displayName, description, example,
                commandAccess, commandAvailability, methodEnum == null ? 5 : 30);
    }

    REGEX(String regex, Class<? extends Jx3BaseAction> baseAction, MethodEnum methodEnum,
          CommandGroup commandGroup, String displayName, String description, String example,
          CommandAccess commandAccess, CommandAvailability commandAvailability, int defaultCooldownSeconds) {
        this.pattern = Pattern.compile(regex);
        this.baseAction = baseAction;
        this.methodEnum = methodEnum;
        this.commandGroup = commandGroup;
        this.displayName = displayName;
        this.description = description;
        this.example = example;
        this.commandAccess = commandAccess;
        this.commandAvailability = commandAvailability;
        this.defaultCooldownSeconds = defaultCooldownSeconds;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public MethodEnum getMethodEnum() {
        return methodEnum;
    }

    /**
     * 外部指令统一规范化：
     * 支持 "/开服" 和 "开服" 两种形式，只去掉开头的指令斜杠。
     */
    public static String normalizeCommand(String input) {
        if (input == null) {
            return "";
        }
        String command = input.trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        return command;
    }

    public static REGEX matchEnum(String input) {
        String command = normalizeCommand(input);
        for (REGEX item : REGEX.values()) {
            Matcher matcher = item.getPattern().matcher(command);
            // 如果你的正则使用了 ^ 和 $，这里要用 matches()；否则可以用 find()
            if (matcher.matches()) {
                return item;
            }
        }
        // 没有任意一个枚举能匹配时
        return null;
    }

    public static REGEX findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (REGEX definition : values()) {
            if (definition.name().equalsIgnoreCase(name)
                    || definition.displayName.equals(name.trim())) {
                return definition;
            }
        }
        return null;
    }

    public Class<? extends Jx3BaseAction> getBaseAction() {
        return baseAction;
    }

    public CommandGroup getCommandGroup() {
        return commandGroup;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getExample() {
        return example;
    }

    public CommandAccess getCommandAccess() {
        return commandAccess;
    }

    public CommandAvailability getCommandAvailability() {
        return commandAvailability;
    }

    public CommandListGroup getCommandListGroup() {
        return switch (this) {
            case CommandList, Help -> CommandListGroup.BASIC_HELP;
            case BindServerCalendar, GroupSettings, GroupCommandSettings,
                    GroupStatistics, SystemStatus, GroupPushList, GroupPushEnable, GroupPushDisable,
                    DailyPushFieldList, DailyPushFieldAdd, DailyPushFieldDelete,
                    GroupAnnouncement -> CommandListGroup.GROUP_CONFIG;
            case GroupRequirementList, GroupRequirementCreate, GroupRequirementRename,
                    GroupRequirementStatus, GroupRequirementAppend, GroupRequirementCancel,
                    GroupRequirementDelete, GroupRequirementDetail -> CommandListGroup.GROUP_REQUIREMENT;
            case BindSchool, UnbindSchool, BindRole, AddRole, ModifyRole,
                    ShowRoleBinding, UnbindRole -> CommandListGroup.USER_BINDING;
            case ActiveCurrent, ActiveListCalendar, ActiveCelebrities, NewsAllNews,
                    NewsAnnounce, ServerCheck, ServerMaster, SkillRework, SchoolFoods -> CommandListGroup.DAILY_NEWS;
            case HomeFlower, HomeFurniture, HomeTravel, ExamAnswer, SchoolMatrix,
                    SchoolSkills, SchoolForce, MechCalculator, SchoolSearch, SkillCalculate -> CommandListGroup.HOME_WIKI;
            case RoleDetailed, RoleMonster, RoleShowCard, RoleShowCards, RoleShowRandom,
                    RoleShowCached, RoleAttribute, DungeonRecord, DpsCompute, ScriptStatus, ScriptStatusUpdate,
                    WatchRecord, FraudDetail, CardPreset, ChatRecords, TuilanAchievement,
                    RoleAchievement -> CommandListGroup.ROLE_QUERY;
            case MemberTeacher, MemberRecruit -> CommandListGroup.RECRUIT_SOCIAL;
            case LuckAdventure, LuckStatistical, LuckUnfinished, LuckRecent,
                    LuckCollect, EventStrategy -> CommandListGroup.LUCK_QUERY;
            case MatchAwesome, MatchSchools, MatchRecent, RankStatistical,
                    RankTrials, SchoolSeniority, RankArena, RankChampionship,
                    RankConstable, RankOutlaw, RankWanted -> CommandListGroup.RANK_MATCH;
            case TradeDemon, TradeRecord, AppearanceNameAliasAdd, AppearanceNameAliasList,
                    AppearanceNameAliasQuery, AppearanceNameAliasPendingList, AppearanceNameAliasApprove,
                    AppearanceNameAliasDelete, TradeRecords, TradeItemSearch, Wanbaolou,
                    TiebaItemRecords, ValuablesStatistical, TradeManufacture -> CommandListGroup.TRADE_PRICE;
            case AuctionRecords, HorseRecords, ActiveNextEvent, ServerEvent,
                    ServerAntivice, MineCart, ChituRecords, ChituWeekRecords,
                    HorseEvent, BattleRecords, ServerSand, ActiveMonster,
                    DuowanStatistics -> CommandListGroup.CAMP_EVENT;
            case SaohuaRandom, SaohuaContent, TiebaRandom, SoundConverter,
                    LuckAdventurePs, ChiGua, SaohuaAnswer, SaohuaContext,
                    SaohuaDrink, SaohuaEat, SaohuaZhanan -> CommandListGroup.FUN_OTHER;
        };
    }

    public boolean isShownInCommandList() {
        return switch (this) {
            case BindSchool, UnbindSchool -> false;
            default -> true;
        };
    }

    public int getDefaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    public boolean requiresRoleName() {
        return switch (this) {
            case LuckAdventure, LuckUnfinished, RoleDetailed, RoleMonster,
                    RoleShowCard, RoleShowCards, RoleShowCached, MatchRecent,
                    RoleAttribute, WatchRecord, DungeonRecord, DpsCompute, ScriptStatus, ScriptStatusUpdate -> true;
            default -> false;
        };
    }

    public boolean usesExternalCall() {
        return methodEnum != null || this == DpsCompute || this == ScriptStatus || this == ScriptStatusUpdate;
    }

    public static String buildHelpText() {
        return buildHelpText(List.of(values()));
    }

    public static String buildHelpText(Collection<REGEX> availableDefinitions) {
        StringBuilder help = new StringBuilder("可用指令（支持 /指令）：");
        for (CommandGroup group : CommandGroup.values()) {
            List<REGEX> groupDefinitions = availableDefinitions.stream()
                    .filter(definition -> definition.commandGroup == group)
                    .toList();
            if (groupDefinitions.isEmpty()) {
                continue;
            }
            help.append("\n\n【").append(group.getDisplayName()).append("】");
            for (REGEX definition : groupDefinitions) {
                help.append("\n")
                        .append(definition.displayName)
                        .append("：")
                        .append(definition.example);
            }
        }
        return help.toString();
    }


    /**
     * 处理指令解析
     */
    public Map<String, String> handleEncounter(String input) {
        Matcher m = this.getPattern().matcher(normalizeCommand(input));
        Map<String, String> resultMap = new HashMap<>();
        if (m.matches()) {
            Matcher groupMatcher = NAMED_GROUP_PATTERN.matcher(this.getPattern().pattern());
            while (groupMatcher.find()) {
                String groupName = groupMatcher.group(1);
                resultMap.putIfAbsent(groupName, safeGroup(m, groupName));
            }            return resultMap;
        }
        return resultMap;
    }

    /**
     * 小工具：安全地取命名组，没这个组名时返回null
     */
    private String safeGroup(Matcher m, String groupName) {
        try {
            return m.group(groupName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public enum CommandGroup {
        BASIC("基础"),
        FREE("免费查询"),
        MEMBER("会员查询"),
        OTHER("其他");

        private final String displayName;

        CommandGroup(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum CommandListGroup {
        BASIC_HELP("基础与帮助"),
        GROUP_CONFIG("群配置"),
        GROUP_REQUIREMENT("群需求"),
        USER_BINDING("个人绑定"),
        DAILY_NEWS("日常资讯"),
        HOME_WIKI("家园百科"),
        ROLE_QUERY("角色查询"),
        RECRUIT_SOCIAL("招募社交"),
        LUCK_QUERY("奇遇查询"),
        RANK_MATCH("排行战绩"),
        TRADE_PRICE("交易物价"),
        CAMP_EVENT("阵营事件"),
        FUN_OTHER("趣味其他");

        private final String displayName;

        CommandListGroup(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum CommandAccess {
        PUBLIC,
        GROUP_ADMIN;

        public boolean allows(String memberRole) {
            if (this == PUBLIC) {
                return true;
            }
            return AuthorDto.canManageGroup(memberRole);
        }
    }

    public enum CommandAvailability {
        SYSTEM,
        PRODUCTION,
        EXPERIMENTAL,
        REVIEW
    }
}
