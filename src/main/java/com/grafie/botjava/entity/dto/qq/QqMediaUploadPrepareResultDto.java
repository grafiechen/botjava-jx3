package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * QQ 群聊富媒体分片预上传结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqMediaUploadPrepareResultDto {

    @JsonProperty("upload_id")
    private String uploadId;

    @JsonProperty("block_size")
    private String blockSize;

    private List<UploadPart> parts;

    @JsonProperty("upload_config")
    private UploadConfig uploadConfig;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadPart {
        private Integer index;

        @JsonProperty("presigned_url")
        private String presignedUrl;

        @JsonProperty("block_size")
        private String blockSize;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadConfig {
        private Integer concurrency;

        @JsonProperty("retry_timeout")
        private Integer retryTimeout;

        @JsonProperty("retry_delay")
        private Integer retryDelay;
    }
}
