package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.config.SoundProperties;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.sound.SoundConverterData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

@Jx3Action

/**
 * 语音合成 阿里云语音合成（TTS）。
 * @author grafie.chen
 * @since 2025/1/22  17:30
 */
public class SoundConverterAction extends Jx3BaseAction {
    private final SoundProperties soundProperties;

    public SoundConverterAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        this(apiProperties, jx3RequestUtil, groupConfigurationService, new SoundProperties());
    }

    @Autowired
    public SoundConverterAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                GroupConfigurationService groupConfigurationService, SoundProperties soundProperties) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.soundProperties = soundProperties;
    }

    @Override
    public BotResponse doAction(String requestRegex, REGEX regex) {
        if (!soundProperties.isEnabled()) {
            return BotResponse.text("语音功能尚未配置。");
        }
        return super.doAction(requestRegex, regex);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appkey", soundProperties.getAppkey());
        params.put("access", soundProperties.getAccess());
        params.put("secret", soundProperties.getSecret());
        params.put("voice", soundProperties.getVoice());
        params.put("format", soundProperties.getFormat());
        params.put("sample_rate", soundProperties.getSampleRate());
        params.put("volume", soundProperties.getVolume());
        params.put("speech_rate", soundProperties.getSpeechRate());
        params.put("pitch_rate", soundProperties.getPitchRate());
        params.put("text", currentArguments().get("text"));
        return params;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        SoundConverterData data = (SoundConverterData) baseResult.getData();
        if (data.getUrl() == null || data.getUrl().isBlank()) {
            return BotResponse.text("语音生成成功，但接口未返回音频地址。");
        }
        return BotResponse.audioUrl(data.getUrl());
    }
}
