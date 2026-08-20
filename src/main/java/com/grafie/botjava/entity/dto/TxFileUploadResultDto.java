package com.grafie.botjava.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.entity.dto.common.MediaDto;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/24  16:30
 */
@Data
public class TxFileUploadResultDto {
    /**
     * 文件 ID
     */
    @JsonProperty(value = "file_uuid")
    private String fileUuid;
    /**
     * 文件信息，用于发消息接口的 media 字段使用
     */
    @JsonProperty(value = "file_info")
    private String fileInfo;
    /**
     * 有效期，表示剩余多少秒到期，到期后 file_info 失效，当等于 0 时，表示可长期使用
     */
    private Integer ttl;
    /**
     * 发送消息的唯一ID，当srv_send_msg设置为true时返回
     */
    private String id;
    /**
     * 分片上传合并后返回的临时下载地址。
     */
    @JsonProperty(value = "raw_url")
    private String rawUrl;
}
