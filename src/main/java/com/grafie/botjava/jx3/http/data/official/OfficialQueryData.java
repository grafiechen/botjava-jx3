package com.grafie.botjava.jx3.http.data.official;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 尚未拆分为独立领域包的官方 HTTP 返回模型。
 */
public final class OfficialQueryData {

    private OfficialQueryData() {
    }

    public static final class HomeFlower {
        private final Map<String, List<Flower>> servers = new LinkedHashMap<>();

        @JsonAnySetter
        public void putServer(String server, List<Flower> flowers) {
            servers.put(server, flowers);
        }

        @JsonAnyGetter
        public Map<String, List<Flower>> jsonServers() {
            return servers;
        }

        @JsonIgnore
        public Map<String, List<Flower>> getServers() {
            return servers;
        }
    }

    @Data
    public static class Flower {
        private String name;
        private String color;
        private Double price;
        private List<String> line;
    }

    @Data
    public static class SkillRework {
        private String id;
        private String title;
        private String url;
        private String time;
    }

    @Data
    public static class SchoolFood {
        private Integer id;
        private String school;
        private String kungfu;
        private String color;
        @JsonProperty("class")
        private String category;
        private String name;
        private String boost;
    }

    @Data
    public static class ActiveNextEvent {
        private String zone;
        private String server;
        private Integer status;
        private Long time;
    }

    @Data
    public static class ArenaRecent {
        private String zoneName;
        private String serverName;
        private String roleName;
        private String roleId;
        private String globalId;
        private String forceName;
        private Integer forceId;
        private String bodyName;
        private Integer bodyId;
        private String tongName;
        private Integer tongId;
        private String campName;
        private String campId;
        private ArenaPerformance performance;
        private List<ArenaHistory> history;
    }

    @Data
    public static class ArenaPerformance {
        @JsonProperty("5v5")
        private JsonNode fiveVsFive;
        @JsonProperty("3v3")
        private JsonNode threeVsThree;
        @JsonProperty("2v2")
        private JsonNode twoVsTwo;
    }

    @Data
    public static class ArenaHistory {
        private String zone;
        private String server;
        private Integer avgGrade;
        private Integer totalMmr;
        private Integer mmr;
        private String kungfu;
        private Integer pvpType;
        private Boolean won;
        private Boolean mvp;
        private Long startTime;
        private Long endTime;
    }

    @Data
    public static class RankStatistical {
        private Integer id;
        private String zone;
        private String server;
        private String name;
        private List<RankEntry> data;
        private Long time;
    }

    @Data
    public static class RankEntry {
        @JsonProperty("max_count")
        private Integer maxCount;
        @JsonProperty("now_count")
        private Integer nowCount;
        @JsonProperty("tong_name")
        private String tongName;
        @JsonProperty("castle_name")
        private String castleName;
        @JsonProperty("master_name")
        private String masterName;
        @JsonProperty("total_score")
        private Long totalScore;
    }

    @Data
    public static class SchoolSeniority {
        private String zoneName;
        private String serverName;
        private String roleName;
        private Long roleId;
        private String forceName;
        private Integer forceId;
        private String forceIcon;
        private String avatarUrl;
        private Long seniority;
    }

    @Data
    public static class MineCart {
        private String server;
        private List<MineCartRecord> data;
    }

    @Data
    public static class MineCartRecord {
        private Long id;
        private String zone;
        private String server;
        private String leader;
        @JsonProperty("camp_name")
        private String campName;
        private String castle;
        private Integer status;
        @JsonProperty("str_status")
        private String statusText;
    }

    @Data
    public static class ChituRecord {
        private Long id;
        private String server;
        @JsonProperty("map_name")
        private String mapName;
        private String horse;
        private Integer send;
        private String date;
    }

    @Data
    public static class ChituWeekRecord {
        private String server;
        @JsonProperty("map_name")
        private String mapName;
        private String horse;
        private String date;
    }

    @Data
    public static class TrialRank {
        private Integer id;
        private String zone;
        private String server;
        private String name;
        private List<TrialRankEntry> data;
        private Long time;
    }

    @Data
    public static class TrialRankEntry {
        @JsonProperty("max_level")
        private Integer maxLevel;
        @JsonProperty("role_name")
        private String roleName;
        @JsonProperty("equip_score")
        private Long equipScore;
        @JsonProperty("total_score")
        private Long totalScore;
    }

    @Data
    public static class TradeRecords {
        @JsonProperty("class")
        private String category;
        private String subclass;
        private String name;
        private String alias;
        private String value;
        private String desc;
        private String date;
        private String view;
        private List<List<TradeListing>> list;
    }

    @Data
    public static class TradeListing {
        private String id;
        private Integer index;
        private String zone;
        private String server;
        private Long value;
        private Integer sale;
        private String token;
        private String date;
        private Integer source;
        private Integer status;
    }

    @Data
    public static class TradeItem {
        @JsonProperty("class")
        private String category;
        private String subclass;
        private String name;
        private String alias;
        private String wblalias;
        private String value;
        private String desc;
        private String date;
        private String view;
    }

    @Data
    public static class BattleRecord {
        private String zoneName;
        private String serverName;
        private String declaringTongName;
        private String acceptingTongName;
        private Long startTime;
        private Long matchDuration;
        private Long endTime;
    }

    @Data
    public static class MechCalculator {
        private MechNode curr;
        private MechNode next;
        private String time;
        private String cdtn;
        @JsonProperty("now_time")
        private String legacyNowTime;
        @JsonProperty("now_node")
        private String legacyNowNode;
        @JsonProperty("now_result")
        private String legacyNowResult;
        @JsonProperty("next_node")
        private String legacyNextNode;
        @JsonProperty("next_result")
        private String legacyNextResult;
        @JsonProperty("interval_time")
        private String legacyIntervalTime;
    }

    @Data
    public static class MechNode {
        private String node;
        private String data;
    }

    @Data
    public static class DuowanStatistics {
        private String server;
        private List<DuowanChannel> data;
    }

    @Data
    public static class DuowanChannel {
        private Long sid;
        private String logoUrl;
        private Integer users;
        private String snick;
        private Integer limit;
        private Integer logo;
        private Integer asid;
        private String esid;
        private String campName;
    }
}
