package com.grafie.botjava.jx3.http.util;

import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.action.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum REGEX {
    BindServerCalendar("^绑定 (?<server>\\S+)$", ActiveCalendarAction.class, null),
    ActiveCalendar("^日常$|^日常 (?<server>\\S+)$", ActiveCalendarAction.class, MethodEnum.DATA_ACTIVE_LIST_CALENDAR),
    ServerCheck("^开服$|^开服 (?<server>\\S+)$", ServerCheckAction.class, MethodEnum.DATA_SERVER_CHECK),
    TradeDemon("^金价$|^金价 (?<server>\\S+)$", TradeDemonAction.class, MethodEnum.DATA_TRADE_DEMON),
    TradeRecord("^物价 (?<value1>\\S+)$", TradeRecordAction.class, MethodEnum.DATA_TRADE_RECORD),
    SaohuaRandom("^骚话$", SaohuaRandomAction.class, MethodEnum.DATA_SAOHUA_RANDOM),
    NewsAnnounce("^更新$|^公告$|^更新公告$", NewsAnnounceAction.class, MethodEnum.DATA_WEB_NEWS_ANNOUNCE),
    LuckAdventure("^奇遇 (?<value1>\\S+)$|^奇遇 (?<server>\\S+) (?<value2>\\S+)$", LuckAdventureAction.class, MethodEnum.DATA_LUCK_ADVENTURE),
    LuckStatistical("^汇总$|^汇总 (?<server>\\S+)$", LuckStatisticalAction.class, MethodEnum.DATA_LUCK_STATISTICAL),
    MatchRecent("^战绩 (?<value1>\\S+)$|^战绩 (?<server>\\S+) (?<value2>\\S+)$", MatchRecentAction.class, MethodEnum.DATA_MATCH_RECENT),
    RoleAttribute("^(?:(?:装备)|(?:属性)) (?<value1>\\S+)$|^(?:(?:装备)|(?:属性)) (?<server>\\S+) (?<value2>\\S+)$", RoleAttributeAction.class, MethodEnum.DATA_ROLE_ATTRIBUTE),
    WatchRecord("^烟花 (?<value1>\\S+)$|^烟花 (?<server>\\S+) (?<value2>\\S+)$", WatchRecordAction.class, MethodEnum.DATA_WATCH_RECORD),
    MemberRecruit("^招募$|^招募 (?<server1>\\S+)$|^招募 (?<server2>\\S+) (?<keyword>\\S+)$", MemberRecruitAction.class, MethodEnum.DATA_MEMBER_RECRUIT),
    ServerSand("^沙盘$|^沙盘 (?<server>\\S+)$", ServerSandAction.class, MethodEnum.DATA_SERVER_SAND),
    ActiveMonster("^百战$|^百战 (?<server>\\S+)$", ActiveMonsterAction.class, MethodEnum.DATA_ACTIVE_MONSTER),
    SchoolForce("^奇穴 (?<value1>\\S+)$", SchoolForceAction.class, MethodEnum.DATA_SCHOOL_FORCE),
    DungeonRecord("^副本 (?<value1>\\S+)$|^副本 (?<server>\\S+) (?<value2>\\S+)$", DungeonRecordAction.class, MethodEnum.DATA_ROLE_CDLIST),
    // 这三个不对外输出
    LuckAdventurePs("^水墨圈圈 (?<value1>\\S+)$", LuckAdventurePsAction.class, null),
    DpsCompute("^[dD][pP][sS] (?<server>\\S+) (?<roleName>\\S+)(?: (?<loop>\\S+))?", DpsComputeAction.class, null),
    ChiGua("^吃瓜 (?<value1>\\S+)$", ChiGuaAction.class, null);

    private final Pattern pattern;
    private final Class baseAction;
    private final MethodEnum methodEnum;

    REGEX(String regex, Class baseAction, MethodEnum methodEnum) {
        this.pattern = Pattern.compile(regex);
        this.baseAction = baseAction;
        this.methodEnum = methodEnum;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public MethodEnum getMethodEnum() {
        return methodEnum;
    }

    public static REGEX matchEnum(String input) {
        for (REGEX item : REGEX.values()) {
            Matcher matcher = item.getPattern().matcher(input);
            // 如果你的正则使用了 ^ 和 $，这里要用 matches()；否则可以用 find()
            if (matcher.matches()) {
                return item;
            }
        }
        // 没有任意一个枚举能匹配时
        return null;
    }

    public Class getBaseAction() {
        return baseAction;
    }
}
