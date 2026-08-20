package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * QQ 群基础信息。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGroupInfoDto {

    @JsonProperty("group_openid")
    private String groupOpenid;

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("group_finger_memo")
    private String groupFingerMemo;

    @JsonProperty("group_class_text")
    private String groupClassText;

    @JsonProperty("group_tags")
    private List<String> groupTags;

    @JsonProperty("group_member_num")
    private Integer groupMemberNum;
}
