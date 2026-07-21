package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.common.ArkDto;
import com.grafie.botjava.entity.dto.common.EmbedDto;
import com.grafie.botjava.entity.dto.common.KeyboardDto;
import com.grafie.botjava.entity.dto.common.MarkdownDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class GroupMessageSenderTest {

    @Test
    @DisplayName("QQ_GROUP_MESSAGE_TEXT_RESPONSE")
    void shouldBuildTextMessageAndDelegateToClient() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);

        sender.send(message("message-text", "group-text"), BotResponse.text("开服啦"));

        TxMessageInfo sent = captureMessage(client, "group-text");
        assertEquals(0, sent.getMsg_type());
        assertEquals("开服啦", sent.getContent());
        assertEquals("message-text", sent.getMsg_id());
        assertEquals(1, sent.getMsg_seq());
    }

    @Test
    @DisplayName("QQ_GROUP_MESSAGE_IMAGE_URL_RESPONSE")
    void shouldUploadImageUrlBeforeBuildingMediaMessage() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        MediaDto media = new MediaDto("FILE_INFO_001");
        when(client.uploadImage("group-image", "https://img.example.com/result.png")).thenReturn(media);
        GroupMessageSender sender = sender(client);

        sender.send(
                message("message-image", "group-image"),
                BotResponse.imageUrl("https://img.example.com/result.png")
        );

        verify(client).uploadImage("group-image", "https://img.example.com/result.png");
        TxMessageInfo sent = captureMessage(client, "group-image");
        assertEquals(7, sent.getMsg_type());
        assertEquals(" ", sent.getContent());
        assertEquals(media, sent.getMedia());
        assertEquals("message-image", sent.getMsg_id());
    }

    @Test
    void shouldUploadAudioUrlBeforeBuildingMediaMessage() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        MediaDto media = new MediaDto("AUDIO_FILE_INFO");
        when(client.uploadAudio("group-audio", "https://audio.example.com/result.mp3")).thenReturn(media);
        GroupMessageSender sender = sender(client);

        sender.send(message("message-audio", "group-audio"),
                BotResponse.audioUrl("https://audio.example.com/result.mp3"));

        verify(client).uploadAudio("group-audio", "https://audio.example.com/result.mp3");
        TxMessageInfo sent = captureMessage(client, "group-audio");
        assertEquals(7, sent.getMsg_type());
        assertEquals(media, sent.getMedia());
        assertEquals("message-audio", sent.getMsg_id());
    }

    @Test
    void shouldBuildMarkdownWithTemplateKeyboard() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);
        MarkdownDto markdown = MarkdownDto.content("# 开服状态");
        KeyboardDto keyboard = new KeyboardDto();
        keyboard.setId("keyboard-template-1");

        sender.send(message("message-markdown", "group-markdown"), BotResponse.markdown(markdown, keyboard));

        TxMessageInfo sent = captureMessage(client, "group-markdown");
        assertEquals(2, sent.getMsg_type());
        assertEquals(markdown, sent.getMarkdown());
        assertEquals(keyboard, sent.getKeyboard());
    }

    @Test
    void shouldBuildAndValidateInteractiveHelpKeyboard() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);
        BotResponse menu = new HelpMenuService(mock(GroupCommandPolicy.class)).interactiveMenu();

        sender.send(message("message-menu", "group-menu"), menu);

        TxMessageInfo sent = captureMessage(client, "group-menu");
        assertEquals(2, sent.getMsg_type());
        assertEquals("jx3:help:BASIC", sent.getKeyboard().getContent().getRows()
                .get(0).getButtons().get(0).getAction().getData());
    }

    @Test
    void shouldRejectInvalidCallbackButtonBeforeCallingQq() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);
        BotResponse menu = new HelpMenuService(mock(GroupCommandPolicy.class)).interactiveMenu();
        menu.getKeyboard().getContent().getRows().get(0).getButtons().get(0)
                .getAction().setData("x".repeat(129));

        assertThrows(IllegalArgumentException.class,
                () -> sender.send(message("message-menu", "group-menu"), menu));
        verify(client, never()).send(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldBuildArkMessage() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);
        ArkDto ark = new ArkDto();
        ark.setTemplateId(23);
        ark.setKv(List.of(new ArkDto.KeyValue("#DESC#", "开服状态", null)));

        sender.send(message("message-ark", "group-ark"), BotResponse.ark(ark));

        TxMessageInfo sent = captureMessage(client, "group-ark");
        assertEquals(3, sent.getMsg_type());
        assertEquals(ark, sent.getArk());
    }

    @Test
    void shouldRejectEmbedForGroupMessage() {
        GroupMessageSender sender = sender(mock(QqGroupMessageClient.class));
        EmbedDto embed = new EmbedDto();
        embed.setTitle("标题");

        assertThrows(
                UnsupportedOperationException.class,
                () -> sender.send(message("message-embed", "group-embed"), BotResponse.embed(embed))
        );
    }

    @Test
    void shouldApplyCustomReplySequenceAndSourceReference() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);

        sender.send(message("message-reference", "group-reference"),
                BotResponse.text("引用回复").withMsgSeq(2).referenceSourceMessage(false));

        TxMessageInfo sent = captureMessage(client, "group-reference");
        assertEquals("message-reference", sent.getMsg_id());
        assertEquals(2, sent.getMsg_seq());
        assertEquals("message-reference", sent.getMessageReference().getMessageId());
        assertEquals(false, sent.getMessageReference().getIgnoreGetMessageError());
        assertNull(sent.getEvent_id());
    }

    @Test
    void shouldSendActiveMessageWithoutPassiveReplyFields() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);

        GroupMessageSender.ActiveMessageResult result =
                sender.sendActive("group-active", BotResponse.text("主动通知"));

        assertTrue(result.sent());
        TxMessageInfo sent = captureMessage(client, "group-active");
        assertNull(sent.getMsg_id());
        assertNull(sent.getMsg_seq());
        assertNull(sent.getEvent_id());
        assertNull(sent.getMessageReference());
    }

    @Test
    void shouldRejectUnauthorizedActiveMessageAndRollbackFailedSend() {
        QqGroupMessageClient deniedClient = mock(QqGroupMessageClient.class);
        GroupActiveMessagePolicy deniedPolicy = mock(GroupActiveMessagePolicy.class);
        when(deniedPolicy.acquire("group-denied"))
                .thenReturn(GroupActiveMessagePolicy.Permit.denied("本群未授权机器人发送主动消息。", 0));
        GroupMessageSender deniedSender = new GroupMessageSender(deniedClient, deniedPolicy);

        GroupMessageSender.ActiveMessageResult denied =
                deniedSender.sendActive("group-denied", BotResponse.text("主动通知"));
        assertFalse(denied.sent());
        assertEquals("本群未授权机器人发送主动消息。", denied.message());
        assertEquals(0, denied.retryAfterSeconds());
        verify(deniedClient, never()).send(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());

        QqGroupMessageClient failedClient = mock(QqGroupMessageClient.class);
        GroupActiveMessagePolicy failedPolicy = mock(GroupActiveMessagePolicy.class);
        GroupActiveMessagePolicy.Permit permit = GroupActiveMessagePolicy.Permit.allowed(
                "group-failed", Instant.EPOCH, "reservation-1");
        when(failedPolicy.acquire("group-failed")).thenReturn(permit);
        doThrow(new IllegalStateException("send failed")).when(failedClient)
                .send(org.mockito.ArgumentMatchers.eq("group-failed"), org.mockito.ArgumentMatchers.any());
        GroupMessageSender failedSender = new GroupMessageSender(failedClient, failedPolicy);

        assertThrows(IllegalStateException.class,
                () -> failedSender.sendActive("group-failed", BotResponse.text("主动通知")));
        verify(failedPolicy).rollback(permit);
    }

    @Test
    void shouldSendEventReplyWithoutMessageReplyFields() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);

        sender.sendEventReply("group-event", "event-001", BotResponse.text("事件回复"));

        TxMessageInfo sent = captureMessage(client, "group-event");
        assertEquals("event-001", sent.getEvent_id());
        assertNull(sent.getMsg_id());
        assertNull(sent.getMsg_seq());
    }

    @Test
    void shouldRejectInvalidReplySequenceAndMessageOptionsForActiveSend() {
        GroupMessageSender sender = sender(mock(QqGroupMessageClient.class));

        assertThrows(IllegalArgumentException.class,
                () -> sender.send(message("message-invalid", "group-invalid"),
                        BotResponse.text("无效序号").withMsgSeq(6)));
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendActive("group-active",
                        BotResponse.text("错误主动消息").referenceSourceMessage()));
    }

    @Test
    void shouldReplaceTransportMetadataFromLegacyRawMessage() {
        QqGroupMessageClient client = mock(QqGroupMessageClient.class);
        GroupMessageSender sender = sender(client);
        TxMessageInfo rawMessage = new TxMessageInfo();
        rawMessage.setContent("旧消息结构");
        rawMessage.setMsg_type(0);
        rawMessage.setMsg_id("legacy-message");
        rawMessage.setMsg_seq(5);
        rawMessage.setEvent_id("legacy-event");

        sender.send(message("source-message", "group-raw"), BotResponse.message(rawMessage));

        TxMessageInfo sent = captureMessage(client, "group-raw");
        assertEquals("source-message", sent.getMsg_id());
        assertEquals(1, sent.getMsg_seq());
        assertNull(sent.getEvent_id());
    }

    private static TxMessageInfo captureMessage(QqGroupMessageClient client, String groupOpenId) {
        ArgumentCaptor<TxMessageInfo> captor = ArgumentCaptor.forClass(TxMessageInfo.class);
        verify(client).send(org.mockito.ArgumentMatchers.eq(groupOpenId), captor.capture());
        return captor.getValue();
    }

    private static GroupMessageSender sender(QqGroupMessageClient client) {
        GroupActiveMessagePolicy policy = mock(GroupActiveMessagePolicy.class);
        when(policy.acquire(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> GroupActiveMessagePolicy.Permit.allowed(
                        invocation.getArgument(0), Instant.EPOCH, "reservation-1"));
        return new GroupMessageSender(client, policy);
    }

    private static GroupAtMessageCreateDto message(String messageId, String groupOpenId) {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId(messageId);
        message.setGroupOpenid(groupOpenId);
        return message;
    }
}
