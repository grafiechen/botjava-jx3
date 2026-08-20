package com.grafie.botjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * QQ 媒体上传策略配置。
 */
@Component
@ConfigurationProperties(prefix = "bot.qq.media")
public class QqMediaUploadProperties {

    private ImageUploadMode imageUploadMode = ImageUploadMode.CHUNK;

    public ImageUploadMode getImageUploadMode() {
        return imageUploadMode;
    }

    public void setImageUploadMode(ImageUploadMode imageUploadMode) {
        if (imageUploadMode == null) {
            throw new IllegalArgumentException("bot.qq.media.image-upload-mode 不能为空");
        }
        this.imageUploadMode = imageUploadMode;
    }

    public boolean useChunkUploadForImage() {
        return imageUploadMode == ImageUploadMode.CHUNK;
    }

    public enum ImageUploadMode {
        CHUNK,
        MINIO
    }
}
