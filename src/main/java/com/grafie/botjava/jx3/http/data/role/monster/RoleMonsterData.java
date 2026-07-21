package com.grafie.botjava.jx3.http.data.role.monster;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

import java.util.List;

/**
 * 百战精耐
 *
 * @author Grafie
 * @since 2024/9/25  11:12
 */
@Data
public class RoleMonsterData {
    @JsonProperty("zone")
    @JsonAlias("zoneName")
    private String zone;
    @JsonProperty("server")
    @JsonAlias("serverName")
    private String server;
    @JsonProperty("role_name")
    @JsonAlias("roleName")
    private String roleName;
    @JsonProperty("role_id")
    @JsonAlias("roleId")
    private String roleId;
    @JsonProperty("global_id")
    @JsonAlias("globalRoleId")
    private String globalRoleId;
    @JsonProperty("skill_energy")
    @JsonAlias("gameEnergy")
    private Long skillEnergy;
    @JsonProperty("skill_stamina")
    @JsonAlias("gameStamina")
    private Long skillStamina;
    @JsonProperty("skill_count")
    @JsonAlias("skillCount")
    private Integer skillCount;
    @JsonProperty("update_time")
    @JsonAlias({"updateTime", "time"})
    private String updateTime;
    @JsonProperty("skill_list")
    @JsonAlias("skillList")
    private List<MonsterSkill> skillList;

    public void setUpdateTime(Long updateTime) {
        this.updateTime = TimeUtils.timeFormatting(updateTime);
    }
}

