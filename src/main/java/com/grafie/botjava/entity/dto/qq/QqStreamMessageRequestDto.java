package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqStreamMessageRequestDto {

    @JsonProperty("input_mode")
    private String inputMode;

    @JsonProperty("input_state")
    private Integer inputState;

    private Integer index;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("content_raw")
    private String contentRaw;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("msg_id")
    private String msgId;

    @JsonProperty("stream_msg_id")
    private String streamMsgId;

    @JsonProperty("msg_seq")
    private Integer msgSeq;

    @JsonProperty("is_wakeup")
    private Boolean wakeup;
}