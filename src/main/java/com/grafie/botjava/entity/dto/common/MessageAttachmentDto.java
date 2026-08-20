package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 消息附件。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageAttachmentDto {

    private String url;
    private String filename;
    private Integer width;
    private Integer height;
    private Long size;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("voice_wav_url")
    private String voiceWavUrl;

    @JsonProperty("asr_refer_text")
    private String asrReferText;
}
