package com.grafie.botjava.entity.dto.common;

import lombok.Data;

/**
 * 机器人业务返回结果。
 * <p>
 * 子类 Action 只负责声明返回类型和业务数据，具体 QQ 消息结构由外层发送器统一拼装。
 *
 * @author grafie.chen
 */
@Data
public class BotResponse {

    private ResponseType responseType;
    private DeliveryMode deliveryMode = DeliveryMode.REPLY;
    private String content;
    private String templateName;
    private Object templateData;
    private String imageUrl;
    private String audioUrl;
    private MarkdownDto markdown;
    private KeyboardDto keyboard;
    private ArkDto ark;
    private EmbedDto embed;
    private MediaDto media;
    private TxMessageInfo rawMessage;
    private Integer msgSeq;
    private boolean referenceSourceMessage;
    private boolean ignoreReferenceError = true;

    public static BotResponse text(String content) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.TEXT);
        response.setContent(content);
        return response;
    }

    public static BotResponse image(String templateName, Object templateData) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.IMAGE);
        response.setTemplateName(templateName);
        response.setTemplateData(templateData);
        return response;
    }

    public static BotResponse imageUrl(String imageUrl) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.IMAGE_URL);
        response.setImageUrl(imageUrl);
        return response;
    }

    public static BotResponse audioUrl(String audioUrl) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.AUDIO_URL);
        response.setAudioUrl(audioUrl);
        return response;
    }

    public static BotResponse markdown(MarkdownDto markdown, KeyboardDto keyboard) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.MARKDOWN);
        response.setMarkdown(markdown);
        response.setKeyboard(keyboard);
        return response;
    }

    public static BotResponse ark(ArkDto ark) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.ARK);
        response.setArk(ark);
        return response;
    }

    public static BotResponse embed(EmbedDto embed) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.EMBED);
        response.setEmbed(embed);
        return response;
    }

    public static BotResponse media(MediaDto media) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.MEDIA);
        response.setMedia(media);
        return response;
    }

    public static BotResponse message(TxMessageInfo rawMessage) {
        BotResponse response = new BotResponse();
        response.setResponseType(ResponseType.RAW_MESSAGE);
        response.setRawMessage(rawMessage);
        return response;
    }

    /**
     * 指定同一来源消息的回复序号，范围为 1 至 5。
     */
    public BotResponse withMsgSeq(int msgSeq) {
        this.msgSeq = msgSeq;
        return this;
    }

    /**
     * 在被动回复中引用来源消息。默认忽略引用消息已不可获取的错误。
     */
    public BotResponse referenceSourceMessage() {
        return referenceSourceMessage(true);
    }

    public BotResponse referenceSourceMessage(boolean ignoreReferenceError) {
        this.referenceSourceMessage = true;
        this.ignoreReferenceError = ignoreReferenceError;
        return this;
    }

    /**
     * 由最外层发送模板按群主动消息发送，不携带 msg_id 或 event_id。
     */
    public BotResponse asActiveMessage() {
        this.deliveryMode = DeliveryMode.ACTIVE;
        return this;
    }

    public enum DeliveryMode {
        REPLY,
        ACTIVE
    }

    public enum ResponseType {
        TEXT(0),
        MARKDOWN(2),
        ARK(3),
        EMBED(4),
        IMAGE(7),
        IMAGE_URL(7),
        AUDIO_URL(7),
        MEDIA(7),
        RAW_MESSAGE(null);

        private final Integer msgType;

        ResponseType(Integer msgType) {
            this.msgType = msgType;
        }

        public Integer getMsgType() {
            return msgType;
        }
    }
}
