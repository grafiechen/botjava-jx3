package com.grafie.botjava.jx3.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * JX3API 阿里语音转换所需的独立第三方配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jx3api.sound")
public class SoundProperties {

    private boolean enabled;
    private String appkey;
    private String access;
    private String secret;
    private String voice = "Aitong";
    private String format = "mp3";
    private int sampleRate = 16000;
    private int volume = 50;
    private int speechRate;
    private int pitchRate;

    @PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }
        Assert.hasText(appkey, "jx3api.sound.appkey 不能为空");
        Assert.hasText(access, "jx3api.sound.access 不能为空");
        Assert.hasText(secret, "jx3api.sound.secret 不能为空");
        Assert.hasText(voice, "jx3api.sound.voice 不能为空");
        Assert.isTrue(Set.of("mp3", "wav", "pcm").contains(format.toLowerCase()),
                "jx3api.sound.format 仅支持 mp3、wav、pcm");
        Assert.isTrue(sampleRate == 8000 || sampleRate == 16000,
                "jx3api.sound.sample-rate 仅支持 8000 或 16000");
        Assert.isTrue(volume >= 0 && volume <= 100, "jx3api.sound.volume 必须在 0 到 100 之间");
        Assert.isTrue(speechRate >= -500 && speechRate <= 500,
                "jx3api.sound.speech-rate 必须在 -500 到 500 之间");
        Assert.isTrue(pitchRate >= -500 && pitchRate <= 500,
                "jx3api.sound.pitch-rate 必须在 -500 到 500 之间");
    }
}
