package com.grafie.botjava.entity.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 富媒体单聊的file_info
 * <a href="https://bot.q.qq.com/wiki/develop/api-v2/server-inter/message/send-receive/rich-media.html#%E7%94%A8%E4%BA%8E%E5%8D%95%E8%81%8A">富媒体单聊的file_info</a>
 *
 * @author grafie.chen
 * @since 2025/1/23  15:57
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MediaDto {
    private String file_info;
}
