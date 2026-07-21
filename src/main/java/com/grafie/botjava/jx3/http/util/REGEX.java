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
    BindRole("^绑定角色 (?<server>\\S+) (?<roleName>\\S+)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "绑定角色", "设置个人默认区服和角色", "绑定角色 乾坤一掷 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    AddRole("^添加角色 (?<server>\\S+) (?<roleName>\\S+)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "添加角色", "保存个人常用角色", "添加角色 梦江南 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    SwitchRole("^切换角色 (?<server>\\S+) (?<roleName>\\S+)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "切换角色", "切换个人默认角色", "切换角色 梦江南 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    ShowRoleBinding("^(?:我的绑定|查看绑定|我的角色)$", UserBindingAction.class, null,
            CommandGroup.BASIC, "查看角色", "查看个人常用角色和默认角色", "我的角色",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    UnbindRole("^解绑角色(?: (?<server>\\S+) (?<roleName>\\S+))?$", UserBindingAction.class, null,
            CommandGroup.BASIC, "解绑角色", "删除单个角色或清除全部绑定", "解绑角色 梦江南 角色名",
            CommandAccess.PUBLIC, CommandAvailability.SYSTEM),
    GroupSettings("^群设置 (?<value>查询|实验|主动消息) (?<value1>开启|关闭)$", GroupSettingsAction.class, null,
            CommandGroup.BASIC, "群功能设置", "开启或关闭群查询、实验与主动消息功能", "群设置 主动消息 开启",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupCommandSettings("^群指令(?: (?<name>\\S+) (?<value1>开启|关闭))?$", GroupSettingsAction.class, null,
            CommandGroup.BASIC, "单项指令设置", "查看或设置本群的单项指令开关", "群指令 开服状态 关闭",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
    GroupStatistics("^群统计(?: (?<num>\\d+))?$", GroupStatisticsAction.class, null,
            CommandGroup.BASIC, "群调用统计", "查看本群近期指令调用统计", "群统计 7",
            CommandAccess.GROUP_ADMIN, CommandAvailability.SYSTEM),
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

    public int getDefaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    public boolean requiresRoleName() {
        return switch (this) {
            case LuckAdventure, LuckUnfinished, RoleDetailed, RoleMonster,
                    RoleShowCard, RoleShowCards, RoleShowCached, MatchRecent,
                    RoleAttribute, WatchRecord, DungeonRecord, DpsCompute -> true;
            default -> false;
        };
    }

    public boolean usesExternalCall() {
        return methodEnum != null || this == DpsCompute;
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
            resultMap.put("value", safeGroup(m, "value"));
            resultMap.put("value1", safeGroup(m, "value1"));
            resultMap.put("server", safeGroup(m, "server"));
            resultMap.put("server1", safeGroup(m, "server1"));
            resultMap.put("roleName", safeGroup(m, "roleName"));
            resultMap.put("loop", safeGroup(m, "loop"));
            resultMap.put("num", safeGroup(m, "num"));
            resultMap.put("limit", safeGroup(m, "limit"));
            resultMap.put("map", safeGroup(m, "map"));
            resultMap.put("subject", safeGroup(m, "subject"));
            resultMap.put("name", safeGroup(m, "name"));
            resultMap.put("mode", safeGroup(m, "mode"));
            resultMap.put("uid", safeGroup(m, "uid"));
            resultMap.put("type", safeGroup(m, "type"));
            resultMap.put("keyword", safeGroup(m, "keyword"));
            resultMap.put("name1", safeGroup(m, "name1"));
            resultMap.put("body", safeGroup(m, "body"));
            resultMap.put("force", safeGroup(m, "force"));
            resultMap.put("school", safeGroup(m, "school"));
            resultMap.put("tags", safeGroup(m, "tags"));
            resultMap.put("text", safeGroup(m, "text"));
            return resultMap;
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
