package com.grafie.botjava.jx3.ws.data;

/**
 * JX3API WebSocket 官方推送事件定义。
 *
 * 数据来源：
 * - https://www.jx3api.com/openapi.socket.json
 * - wss://socket.nicemoe.cn
 */
public enum Jx3WsEventEnum {
    LUCK_TRIGGER(1001, 3, "奇遇触发", "奇遇事件"),
    HORSE_REFRESH(1002, 3, "马驹刷新", "马场事件"),
    HORSE_CAPTURE(1003, 3, "马驹捕获", "马场事件"),
    FUYAO_OPEN(1005, 3, "扶摇开启", "扶摇事件"),
    FUYAO_CALL(1006, 3, "扶摇点名", "扶摇事件"),
    DILU_DAILY(1008, 3, "的卢每日", "的卢事件"),
    DILU_REFRESH(1009, 3, "的卢刷新", "的卢事件"),
    DILU_CAPTURE(1010, 3, "的卢捕获", "的卢事件"),
    DILU_AUCTION(1011, 3, "的卢拍卖", "的卢事件"),
    REWARD_DROP(1012, 3, "副本掉落 / 私货", "掉落交易"),
    CAMP_AUCTION(1013, 3, "阵营拍卖", "掉落交易"),
    WICKED_EVENT(1014, 3, "诛恶事件", "江湖事件"),
    ZHUIHUN_CALL(1015, 3, "追魂点名", "江湖事件"),
    CAMP_SACRIFICE(1017, 3, "阵营祭祀", "阵营祭祀"),
    CASTLE_LEADER(1018, 3, "关隘首领", "关隘首领"),
    TERRITORY_BATTLE_START(1101, 3, "领地宣战·开始", "宣战约战"),
    TERRITORY_BATTLE_END(1102, 3, "领地宣战·结束", "宣战约战"),
    GUILD_BATTLE_START(1103, 3, "帮会宣战·开始", "宣战约战"),
    GUILD_BATTLE_END(1104, 3, "帮会宣战·结束", "宣战约战"),
    GUILD_MATCH_WIN(1105, 3, "帮会约战·完胜", "宣战约战"),
    FRONTLINE_BARN(1111, 3, "抢占粮仓", "据点攻防"),
    FRONTLINE_FLAG_RESET(1112, 3, "大旗重置", "据点攻防"),
    FRONTLINE_FLAG_TAKEN(1113, 3, "大旗被夺", "据点攻防"),
    FRONTLINE_OCCUPIED(1114, 3, "据点占领", "据点攻防"),
    FRONTLINE_OCCUPIED_NO_GUILD(1115, 3, "据点占领（无帮会）", "据点攻防"),
    FRONTLINE_SMALL_CONTRIBUTION_IDLE(1116, 3, "小攻防贡献（非开战）", "据点攻防"),
    FRONTLINE_SMALL_CONTRIBUTION(1117, 3, "小攻防贡献", "据点攻防"),
    FRONTLINE_BIG_CONTRIBUTION(1118, 3, "大攻防贡献", "据点攻防"),
    FRONTLINE_LOOT_AUCTION(1119, 3, "战利品竞拍", "据点攻防"),
    FRONTLINE_SMALL_BONUS(1120, 3, "小攻防分红", "据点攻防"),
    FRONTLINE_BIG_BONUS(1121, 3, "大攻防分红", "据点攻防"),
    FRONTLINE_BIG_BONUS_WITH_LEADER(1122, 3, "大攻防分红（含指挥）", "据点攻防"),
    WEIBO_UPDATE(1201, 3, "微博更新", "社交动态"),
    SERVER_STATUS(2001, 0, "开服状态", "系统通知"),
    OFFICIAL_NEWS(2002, 0, "官方新闻", "系统通知"),
    GAME_UPDATE(2003, 0, "版本更新", "系统通知"),
    TIEBA_NEWS(2004, 0, "八卦速报", "系统通知"),
    SYSTEM_CASTLE_LEADER(2005, 0, "关隘首领", "系统通知"),
    YUNCONG_NOTICE(2006, 0, "云从预告", "系统通知");

    private final int actionCode;
    private final int level;
    private final String summary;
    private final String category;

    Jx3WsEventEnum(int actionCode, int level, String summary, String category) {
        this.actionCode = actionCode;
        this.level = level;
        this.summary = summary;
        this.category = category;
    }

    public int getActionCode() {
        return actionCode;
    }

    public int getLevel() {
        return level;
    }

    public String getSummary() {
        return summary;
    }

    public String getCategory() {
        return category;
    }

    public static Jx3WsEventEnum findByActionCode(int actionCode) {
        for (Jx3WsEventEnum event : values()) {
            if (event.actionCode == actionCode) {
                return event;
            }
        }
        return null;
    }
}