package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QQ Markdown 消息结构。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkdownDto {
    private String content;
    @JsonProperty("custom_template_id")
    private String customTemplateId;
    private List<Param> params;

    public static MarkdownDto content(String content) {
        MarkdownDto markdown = new MarkdownDto();
        markdown.setContent(content);
        return markdown;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Param {
        private String key;
        private List<String> values;
    }
}
