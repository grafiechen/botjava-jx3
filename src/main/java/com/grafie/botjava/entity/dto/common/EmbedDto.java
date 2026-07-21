package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QQ Embed 消息结构。当前 QQ 群聊接口不支持发送，仅供其他场景扩展。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbedDto {
    private String title;
    private String prompt;
    private String description;
    private Thumbnail thumbnail;
    private List<Field> fields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Thumbnail {
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Field {
        private String name;
    }
}
