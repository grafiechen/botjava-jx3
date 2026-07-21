package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QQ Markdown 消息按钮结构。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyboardDto {
    private String id;
    private Content content;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Row> rows;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private List<Button> buttons;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Button {
        private String id;
        @JsonProperty("render_data")
        private RenderData renderData;
        private Action action;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RenderData {
        private String label;
        @JsonProperty("visited_label")
        private String visitedLabel;
        private Integer style;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Action {
        private Integer type;
        private Permission permission;
        private String data;
        private Boolean reply;
        private Boolean enter;
        private Integer anchor;
        @JsonProperty("unsupport_tips")
        private String unsupportTips;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Permission {
        private Integer type;
        @JsonProperty("specify_user_ids")
        private List<String> specifyUserIds;
        @JsonProperty("specify_role_ids")
        private List<String> specifyRoleIds;
    }
}
