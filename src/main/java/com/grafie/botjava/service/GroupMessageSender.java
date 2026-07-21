package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.common.KeyboardDto;
import com.grafie.botjava.entity.dto.common.MarkdownDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.MessageReferenceDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.minio.MinioUtil;
import com.grafie.botjava.util.HtmlToImageUtl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;
import java.util.List;

/**
 * 群消息发送器。
 * <p>
 * 负责将业务返回对象转换为 QQ 群消息结构，并调用 QQ 发送接口。
 *
 * @author grafie.chen
 */
@Slf4j
@Service
public class GroupMessageSender {

    private final QqGroupMessageClient messageClient;
    private final GroupActiveMessagePolicy activeMessagePolicy;
    @Autowired(required = false)
    private MinioUtil minioUtil;

    public GroupMessageSender(QqGroupMessageClient messageClient,
                              GroupActiveMessagePolicy activeMessagePolicy) {
        this.messageClient = messageClient;
        this.activeMessagePolicy = activeMessagePolicy;
    }

    /**
     * 兼容现有群指令链路，默认作为来源消息的第 1 条被动回复。
     */
    public void send(GroupAtMessageCreateDto messageCreateDto, BotResponse response) {
        requireText(messageCreateDto.getId(), "被动回复需要来源消息 id");
        sendInternal(messageCreateDto.getGroupOpenid(), response,
                DeliveryContext.messageReply(messageCreateDto.getId(), resolveMsgSeq(response)));
    }

    /**
     * 使用事件 ID 发送被动消息。
     */
    public void sendEventReply(String groupOpenId, String eventId, BotResponse response) {
        requireText(eventId, "事件被动回复需要 eventId");
        rejectMessageOnlyOptions(response, "事件被动回复");
        sendInternal(groupOpenId, response, DeliveryContext.eventReply(eventId));
    }

    /**
     * 发送群主动消息，不携带 event_id、msg_id 或 msg_seq。
     */
    public ActiveMessageResult sendActive(String groupOpenId, BotResponse response) {
        rejectMessageOnlyOptions(response, "主动消息");
        if (response == null) {
            return ActiveMessageResult.delivered();
        }
        GroupActiveMessagePolicy.Permit permit = activeMessagePolicy.acquire(groupOpenId);
        if (!permit.allowed()) {
            return ActiveMessageResult.rejected(permit.message(), permit.retryAfterSeconds());
        }
        try {
            sendInternal(groupOpenId, response, DeliveryContext.active());
            return ActiveMessageResult.delivered();
        } catch (RuntimeException e) {
            activeMessagePolicy.rollback(permit);
            throw e;
        }
    }

    public record ActiveMessageResult(boolean sent, String message, long retryAfterSeconds) {
        public static ActiveMessageResult delivered() {
            return new ActiveMessageResult(true, null, 0);
        }

        public static ActiveMessageResult rejected(String message, long retryAfterSeconds) {
            return new ActiveMessageResult(false, message, Math.max(0, retryAfterSeconds));
        }
    }

    private void sendInternal(String groupOpenId, BotResponse response, DeliveryContext deliveryContext) {
        requireText(groupOpenId, "群消息 groupOpenId 不能为空");
        TxMessageInfo messageInfo = buildMessageInfo(groupOpenId, response);
        if (messageInfo == null) {
            log.info("群消息业务返回为空，不发送消息");
            return;
        }
        applyDeliveryContext(messageInfo, response, deliveryContext);
        messageClient.send(groupOpenId, messageInfo);
    }

    private TxMessageInfo buildMessageInfo(String groupOpenId, BotResponse response) {
        if (response == null) {
            return null;
        }
        if (response.getResponseType() == null) {
            throw new IllegalArgumentException("业务返回类型不能为空");
        }
        if (response.getResponseType() == BotResponse.ResponseType.RAW_MESSAGE) {
            if (response.getRawMessage() == null) {
                throw new IllegalArgumentException("RAW_MESSAGE 的消息内容不能为空");
            }
            return response.getRawMessage();
        }

        TxMessageInfo messageInfo = new TxMessageInfo();
        messageInfo.setMsg_type(response.getResponseType().getMsgType());
        messageInfo.setContent(response.getContent());

        switch (response.getResponseType()) {
            case TEXT:
                requireText(response.getContent(), "文本消息 content 不能为空");
                return messageInfo;
            case IMAGE:
                requireText(response.getTemplateName(), "图片消息 templateName 不能为空");
                messageInfo.setContent(" ");
                messageInfo.setMedia(buildTemplateImageMedia(groupOpenId, response));
                return messageInfo;
            case IMAGE_URL:
                requireText(response.getImageUrl(), "图片消息 imageUrl 不能为空");
                messageInfo.setContent(" ");
                messageInfo.setMedia(uploadImageUrl(groupOpenId, response.getImageUrl()));
                return messageInfo;
            case AUDIO_URL:
                requireText(response.getAudioUrl(), "语音消息 audioUrl 不能为空");
                messageInfo.setContent(" ");
                messageInfo.setMedia(messageClient.uploadAudio(groupOpenId, response.getAudioUrl()));
                return messageInfo;
            case MEDIA:
                if (response.getMedia() == null || response.getMedia().getFile_info() == null
                        || response.getMedia().getFile_info().isBlank()) {
                    throw new IllegalArgumentException("媒体消息 file_info 不能为空");
                }
                messageInfo.setContent(response.getContent() == null ? " " : response.getContent());
                messageInfo.setMedia(response.getMedia());
                return messageInfo;
            case MARKDOWN:
                validateMarkdown(response.getMarkdown(), response.getKeyboard());
                messageInfo.setMarkdown(response.getMarkdown());
                messageInfo.setKeyboard(response.getKeyboard());
                return messageInfo;
            case ARK:
                if (response.getArk() == null || response.getArk().getTemplateId() == null
                        || response.getArk().getKv() == null || response.getArk().getKv().isEmpty()) {
                    throw new IllegalArgumentException("Ark 消息需要 template_id 和 kv");
                }
                messageInfo.setArk(response.getArk());
                return messageInfo;
            case EMBED:
                throw new UnsupportedOperationException("QQ 群聊当前不支持 Embed 消息");
            default:
                throw new IllegalArgumentException("不支持的返回类型：" + response.getResponseType());
        }
    }

    private MediaDto buildTemplateImageMedia(String groupOpenId, BotResponse response) {
        if (minioUtil == null) {
            throw new IllegalStateException("图片消息需要配置 MinioUtil");
        }
        try {
            Path outputPath = Path.of(HtmlToImageUtl.renderTemplateToImage(response.getTemplateName(), response.getTemplateData()));
            String imageUrl = minioUtil.uploadFile(outputPath.toFile(), UUID.randomUUID() + ".png");
            return messageClient.uploadImage(groupOpenId, imageUrl);
        } catch (Exception e) {
            throw new RuntimeException("生成图片消息失败", e);
        }
    }

    private MediaDto uploadImageUrl(String groupOpenId, String imageUrl) {
        return messageClient.uploadImage(groupOpenId, imageUrl);
    }

    private void applyDeliveryContext(TxMessageInfo messageInfo, BotResponse response,
                                      DeliveryContext deliveryContext) {
        // 平台传输字段统一由发送器管理，RAW_MESSAGE 也不能绕过该约束。
        messageInfo.setEvent_id(null);
        messageInfo.setMsg_id(null);
        messageInfo.setMsg_seq(null);
        messageInfo.setMessageReference(null);

        messageInfo.setEvent_id(deliveryContext.eventId());
        messageInfo.setMsg_id(deliveryContext.messageId());
        messageInfo.setMsg_seq(deliveryContext.msgSeq());
        if (response.isReferenceSourceMessage()) {
            messageInfo.setMessageReference(new MessageReferenceDto(
                    deliveryContext.messageId(), response.isIgnoreReferenceError()));
        }
    }

    private int resolveMsgSeq(BotResponse response) {
        int msgSeq = response == null || response.getMsgSeq() == null ? 1 : response.getMsgSeq();
        if (msgSeq < 1 || msgSeq > 5) {
            throw new IllegalArgumentException("msgSeq 必须在 1 到 5 之间");
        }
        return msgSeq;
    }

    private void rejectMessageOnlyOptions(BotResponse response, String deliveryName) {
        if (response == null) {
            return;
        }
        if (response.getMsgSeq() != null || response.isReferenceSourceMessage()) {
            throw new IllegalArgumentException(deliveryName + "不能设置 msgSeq 或引用来源消息");
        }
    }

    private void validateMarkdown(MarkdownDto markdown, KeyboardDto keyboard) {
        if (markdown == null) {
            throw new IllegalArgumentException("Markdown 消息结构不能为空");
        }
        boolean hasContent = markdown.getContent() != null && !markdown.getContent().isBlank();
        boolean hasTemplate = markdown.getCustomTemplateId() != null && !markdown.getCustomTemplateId().isBlank();
        if (!hasContent && !hasTemplate) {
            throw new IllegalArgumentException("Markdown 需要 content 或 custom_template_id");
        }
        if (hasTemplate && (markdown.getParams() == null || markdown.getParams().isEmpty())) {
            throw new IllegalArgumentException("模板 Markdown 需要 params");
        }
        validateKeyboard(keyboard);
    }

    private void validateKeyboard(KeyboardDto keyboard) {
        if (keyboard == null || keyboard.getId() != null) {
            return;
        }
        if (keyboard.getContent() == null || keyboard.getContent().getRows() == null) {
            throw new IllegalArgumentException("自定义 Keyboard 需要 content.rows");
        }
        List<KeyboardDto.Row> rows = keyboard.getContent().getRows();
        if (rows.isEmpty() || rows.size() > 5) {
            throw new IllegalArgumentException("Keyboard 行数必须在 1 到 5 之间");
        }
        for (KeyboardDto.Row row : rows) {
            if (row.getButtons() == null || row.getButtons().isEmpty() || row.getButtons().size() > 5) {
                throw new IllegalArgumentException("Keyboard 每行按钮数必须在 1 到 5 之间");
            }
            for (KeyboardDto.Button button : row.getButtons()) {
                validateKeyboardButton(button);
            }
        }
    }

    private void validateKeyboardButton(KeyboardDto.Button button) {
        if (button == null || button.getRenderData() == null
                || button.getRenderData().getLabel() == null
                || button.getRenderData().getLabel().isBlank()) {
            throw new IllegalArgumentException("Keyboard 按钮需要 render_data.label");
        }
        KeyboardDto.Action action = button.getAction();
        if (action == null || action.getType() == null || action.getType() < 0 || action.getType() > 4) {
            throw new IllegalArgumentException("Keyboard 按钮需要合法的 action.type");
        }
        KeyboardDto.Permission permission = action.getPermission();
        if (permission == null || permission.getType() == null
                || permission.getType() < 0 || permission.getType() > 3) {
            throw new IllegalArgumentException("Keyboard 按钮需要合法的 action.permission.type");
        }
        if (action.getType() == 1) {
            if (action.getData() == null || action.getData().isBlank()) {
                throw new IllegalArgumentException("Keyboard 回调按钮需要 action.data");
            }
            if (action.getData().length() > 128) {
                throw new IllegalArgumentException("Keyboard 回调按钮 action.data 不能超过 128 个字符");
            }
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private record DeliveryContext(String messageId, String eventId, Integer msgSeq) {

        private static DeliveryContext messageReply(String messageId, int msgSeq) {
            return new DeliveryContext(messageId, null, msgSeq);
        }

        private static DeliveryContext eventReply(String eventId) {
            return new DeliveryContext(null, eventId, null);
        }

        private static DeliveryContext active() {
            return new DeliveryContext(null, null, null);
        }
    }
}
