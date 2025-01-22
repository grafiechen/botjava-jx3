package com.grafie.botjava.jx3.http.util;

import java.util.regex.Pattern;

public enum REGEX {
    日常任务("^日常$|^日常 (?<server>\\S+)$"),
    开服检查("^开服$|^开服 (?<server>\\S+)$"),
    金价比例("^金价$|^金价 (?<server>\\S+)$"),
    推荐小药("^小药 (?<value1>\\S+)$|^(?<value2>\\S+)小药$"),
    配装推荐("^配装 (?<value1>\\S+)$|^(?<value2>\\S+)配装$"),
    阵眼效果("^阵眼 (?<value1>\\S+)$|^(?<value2>\\S+)阵眼$"),
    物品价格("^物价 (?<value1>\\S+)$"),
    随机骚话("^骚话$"),
    奇遇前置("^(?:(?:前置)|(?:条件)) (?<value1>\\S+)$"),
    奇遇攻略("^攻略 (?<value1>\\S+)$|^(?<value2>\\S+)攻略$"),
    更新公告("^更新$|^公告$|^更新公告$"),
    奇遇查询("^奇遇 (?<value1>\\S+)$|^奇遇 (?<server>\\S+) (?<value2>\\S+)$"),
    奇遇汇总("^汇总$|^汇总 (?<server>\\S+)$"),
    比赛战绩("^战绩 (?<value1>\\S+)$|^战绩 (?<server>\\S+) (?<value2>\\S+)$"),
    装备属性("^(?:(?:装备)|(?:属性)) (?<value1>\\S+)$|^(?:(?:装备)|(?:属性)) (?<server>\\S+) (?<value2>\\S+)$"),
    烟花记录("^烟花 (?<value1>\\S+)$|^烟花 (?<server>\\S+) (?<value2>\\S+)$"),
    招募查询("^招募$|^招募 (?<server1>\\S+)$|^招募 (?<server2>\\S+) (?<keyword>\\S+)$"),
    沙盘查询("^沙盘$|^沙盘 (?<server>\\S+)$"),
    资历榜("^资历榜$|^资历榜 (?<server1>\\S+)$|^资历榜 (?<server2>\\S+) (?<keyword>\\S+)$"),
    声望榜("^(?<type1>声望榜)$|^(?<type2>声望榜) (?<server>\\S+)$"),
    老江湖("^(?<type1>老江湖)$|^(?<type2>老江湖) (?<server>\\S+)$"),
    兵甲榜("^(?<type1>兵甲榜)$|^(?<type2>兵甲榜) (?<server>\\S+)$"),
    名师榜("^(?<type1>名师榜)$|^(?<type2>名师榜) (?<server>\\S+)$"),
    战阶榜("^(?<type1>战阶榜)$|^(?<type2>战阶榜) (?<server>\\S+)$"),
    薪火榜("^(?<type1>薪火榜)$|^(?<type2>薪火榜) (?<server>\\S+)$"),
    梓行榜("^(?<type1>梓行榜)$|^(?<type2>梓行榜) (?<server>\\S+)$"),
    爱心榜("^(?<type1>爱心榜) (?<value1>\\S+)$|^(?<type2>爱心榜) (?<server>\\S+) (?<value2>\\S+)$"),
    神兵榜("^(?<type1>神兵榜) (?<value1>\\S+)$|^(?<type2>神兵榜) (?<server>\\S+) (?<value2>\\S+)$"),
    试炼榜("^试炼榜 (?<value1>\\S+)$|^试炼榜 (?<server>\\S+) (?<value2>\\S+)$"),
    百战异闻录("^百战$|^百战 (?<server>\\S+)$"),
    活动日历("^活动日历$|^活动日历 (?<server>\\S+)$"),
    奇穴查询("^奇穴 (?<value1>\\S+)$"),
    水墨圈圈("^水墨圈圈 (?<value1>\\S+)$"),
    DPS计算("^[dD][pP][sS] (?<server>\\S+) (?<roleName>\\S+)(?: (?<loop>\\S+))?"),
    DPS计算支持循环("^支持循环 (?<value1>\\S+)$"),
    DPS计算支持心法("^支持心法$"),
    DPS计算支持心法循环("^心法循环$"),
    吃瓜("^吃瓜 (?<value1>\\S+)$"),
    技改("^技改$"),
    副本记录("^副本 (?<value1>\\S+)$|^副本 (?<server>\\S+) (?<value2>\\S+)$");

    private final Pattern pattern;

    REGEX(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public Pattern getPattern() {
        return pattern;
    }


}
