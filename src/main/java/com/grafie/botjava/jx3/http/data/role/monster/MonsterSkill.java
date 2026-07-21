package com.grafie.botjava.jx3.http.data.role.monster;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MonsterSkill {
    @JsonProperty("dwInSkillID")
    private Long inputSkillId;
    @JsonProperty("dwOutSkillID")
    private Long outputSkillId;
    @JsonProperty("skill_color")
    @JsonAlias("nColor")
    private Integer color;
    @JsonProperty("skill_cost")
    @JsonAlias("nCost")
    private Integer cost;
    @JsonProperty("skill_level")
    @JsonAlias("nLevel")
    private Integer level;
    @JsonProperty("leader_name")
    @JsonAlias("szBossName")
    private String leaderName;
    @JsonProperty("skill_name")
    @JsonAlias("szSkillName")
    private String skillName;
    @JsonProperty("szType")
    private String type;
    @JsonProperty("is_deprecated")
    @JsonAlias({"bDeprecated", "bDeprecatedl"})
    private Boolean deprecated;
}
