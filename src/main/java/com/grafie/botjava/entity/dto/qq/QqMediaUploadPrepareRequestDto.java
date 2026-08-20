package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 群聊富媒体分片预上传请求。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqMediaUploadPrepareRequestDto {

    @JsonProperty("file_type")
    private Integer fileType;

    @JsonProperty("file_size")
    private String fileSize;

    @JsonProperty("file_name")
    private String fileName;

    private String md5;
    private String sha1;

    @JsonProperty("md5_10m")
    private String md5TenMb;
}
