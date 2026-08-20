package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqStreamMessageResultDto {

    private String id;
    private String timestamp;

    @JsonProperty("ext_info")
    private QqGroupMessageSendResultDto.ExtInfo extInfo;

    @JsonProperty("remain_msg_len")
    private Integer remainMsgLen;
}