package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 群聊富媒体分片完成通知请求。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqMediaUploadPartFinishRequestDto {

    @JsonProperty("upload_id")
    private String uploadId;

    @JsonProperty("part_index")
    private Integer partIndex;

    @JsonProperty("block_size")
    private String blockSize;

    private String md5;
}
