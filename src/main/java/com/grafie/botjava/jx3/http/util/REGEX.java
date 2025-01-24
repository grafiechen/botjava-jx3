package com.grafie.botjava.jx3.http.util;

import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.action.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author grafie.chen
 * @since 2025/1/22  15:36
 */
public enum REGEX {
    BindServerCalendar("^绑定 (?<server>\\S+)$", BindServerAction.class, null),
    ActiveCurrent("^日常$|^日常 (?<server>.+?)(?: (?<value>\\S+))?$", ActiveCurrentAction.class, MethodEnum.DATA_ACTIVE_CALENDAR),
    ServerCheck("^开服$|^开服 (?<server>\\S+)$", ServerCheckAction.class, MethodEnum.DATA_SERVER_CHECK),
    TradeDemon("^金价$|^金价 (?<server>\\S+)$", TradeDemonAction.class, MethodEnum.DATA_TRADE_DEMON),
    TradeRecord("^物价 (?<value>\\S+)$", TradeRecordAction.class, MethodEnum.DATA_TRADE_RECORD),
    SaohuaRandom("^骚话$", SaohuaRandomAction.class, MethodEnum.DATA_SAOHUA_RANDOM),
    NewsAnnounce("^更新$|^公告$|^更新公告$", NewsAnnounceAction.class, MethodEnum.DATA_WEB_NEWS_ANNOUNCE),
    LuckAdventure("^奇遇 (?<value>\\S+)$|^奇遇 (?<server>\\S+) (?<value1>\\S+)$", LuckAdventureAction.class, MethodEnum.DATA_LUCK_ADVENTURE),
    LuckStatistical("^汇总$|^汇总 (?<server>\\S+)$", LuckStatisticalAction.class, MethodEnum.DATA_LUCK_STATISTICAL),
    MatchRecent("^战绩 (?<value>\\S+)$|^战绩 (?<server>\\S+) (?<value1>\\S+)$", MatchRecentAction.class, MethodEnum.DATA_MATCH_RECENT),
    RoleAttribute("^(?:(?:装备)|(?:属性)) (?<value>\\S+)$|^(?:(?:装备)|(?:属性)) (?<server>\\S+) (?<value1>\\S+)$", RoleAttributeAction.class, MethodEnum.DATA_ROLE_ATTRIBUTE),
    WatchRecord("^烟花 (?<value>\\S+)$|^烟花 (?<server>\\S+) (?<value1>\\S+)$", WatchRecordAction.class, MethodEnum.DATA_WATCH_RECORD),
    MemberRecruit("^招募$|^招募 (?<server>\\S+)$|^招募 (?<server1>\\S+) (?<value>\\S+)$", MemberRecruitAction.class, MethodEnum.DATA_MEMBER_RECRUIT),
    ServerSand("^沙盘$|^沙盘 (?<server>\\S+)$", ServerSandAction.class, MethodEnum.DATA_SERVER_SAND),
    ActiveMonster("^百战$|^百战 (?<server>\\S+)$", ActiveMonsterAction.class, MethodEnum.DATA_ACTIVE_MONSTER),
    SchoolForce("^奇穴 (?<value>\\S+)$", SchoolForceAction.class, MethodEnum.DATA_SCHOOL_FORCE),
    DungeonRecord("^副本 (?<value>\\S+)$|^副本 (?<server>\\S+) (?<value1>\\S+)$", DungeonRecordAction.class, MethodEnum.DATA_ROLE_CDLIST),
    // 这三个需要特殊处理
    LuckAdventurePs("^水墨圈圈 (?<value>\\S+)$", LuckAdventurePsAction.class, null),
    DpsCompute("^[dD][pP][sS](?: (?<server>\\S+))? (?<roleName>\\S+)(?: (?<loop>\\S+))?", DpsComputeAction.class, null),
    ChiGua("^吃瓜 (?<value>\\S+)$", ChiGuaAction.class, null);

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


    /**
     * 处理指令解析
     */
    public Map<String, String> handleEncounter(String input) {
        Matcher m = this.getPattern().matcher(input);
        Map<String, String> resultMap = new HashMap<>();
        if (m.matches()) {
            resultMap.put("value", safeGroup(m, "value"));
            resultMap.put("value1", safeGroup(m, "value1"));
            resultMap.put("server", safeGroup(m, "server"));
            resultMap.put("server1", safeGroup(m, "server1"));
            resultMap.put("roleName", safeGroup(m, "roleName"));
            resultMap.put("loop", safeGroup(m, "loop"));
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
}
