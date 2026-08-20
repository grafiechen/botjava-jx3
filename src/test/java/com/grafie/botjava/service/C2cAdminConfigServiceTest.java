package com.grafie.botjava.service;

import com.grafie.botjava.config.QqAdminProperties;
import com.grafie.botjava.entity.BotRuntimeConfig;
import com.grafie.botjava.entity.dto.c2c.C2cMessageCreateDto;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class C2cAdminConfigServiceTest {

    @Test
    void shouldAllowMasterOpenidToSetRuntimeConfigAndAuditSuccess() {
        QqAdminProperties properties = new QqAdminProperties();
        properties.setMasterOpenids(List.of("master-openid"));
        BotRuntimeConfigService configService = mock(BotRuntimeConfigService.class);
        BotAdminAuditService auditService = mock(BotAdminAuditService.class);
        C2cMessageSender c2cMessageSender = mock(C2cMessageSender.class);
        BotRuntimeConfig config = new BotRuntimeConfig();
        config.setConfigKey("review.mode");
        when(configService.set(eq("review.mode"), eq("on"), eq("master-openid"))).thenReturn(config);
        C2cAdminConfigService service = new C2cAdminConfigService(
                properties, configService, auditService, c2cMessageSender, mock(ScriptStatusFieldService.class));

        service.handle(message("master-openid", "msg-1", "配置设置 review.mode on"));

        verify(configService).set(eq("review.mode"), eq("on"), eq("master-openid"));
        verify(auditService).record(eq("CONFIG"), eq("SET"), eq("review.mode"), eq("master-openid"),
                eq("C2C"), eq(true), eq(null));
        ArgumentCaptor<C2cMessageCreateDto> messageCaptor = ArgumentCaptor.forClass(C2cMessageCreateDto.class);
        verify(c2cMessageSender).replyText(messageCaptor.capture(), eq("已设置配置：review.mode"));
        assertEquals("msg-1", messageCaptor.getValue().getId());
    }

    @Test
    void shouldRejectNonMasterAndWriteAuditLog() {
        QqAdminProperties properties = new QqAdminProperties();
        properties.setMasterOpenids(List.of("master-openid"));
        BotRuntimeConfigService configService = mock(BotRuntimeConfigService.class);
        BotAdminAuditService auditService = mock(BotAdminAuditService.class);
        C2cMessageSender c2cMessageSender = mock(C2cMessageSender.class);
        C2cAdminConfigService service = new C2cAdminConfigService(
                properties, configService, auditService, c2cMessageSender, mock(ScriptStatusFieldService.class));

        service.handle(message("other-openid", "msg-1", "配置删除 review.mode"));

        verify(configService, never()).delete(any());
        verify(auditService).record(eq("CONFIG"), eq("配置删除"), eq("review.mode"), eq("other-openid"),
                eq("C2C"), eq(false), eq("非主号尝试执行配置管理"));
        verify(c2cMessageSender).replyText(any(C2cMessageCreateDto.class),
                eq("没有权限。只有配置的 QQ 主号可以执行私聊配置管理。"));
    }

    @Test
    void shouldListConfigKeysWithoutLeakingValues() {
        QqAdminProperties properties = new QqAdminProperties();
        properties.setMasterOpenids(List.of("master-openid"));
        BotRuntimeConfigService configService = mock(BotRuntimeConfigService.class);
        BotAdminAuditService auditService = mock(BotAdminAuditService.class);
        C2cMessageSender c2cMessageSender = mock(C2cMessageSender.class);
        BotRuntimeConfig config = new BotRuntimeConfig();
        config.setConfigKey("secret.key");
        config.setConfigValue("secret-value");
        when(configService.list()).thenReturn(List.of(config));
        C2cAdminConfigService service = new C2cAdminConfigService(
                properties, configService, auditService, c2cMessageSender, mock(ScriptStatusFieldService.class));

        service.handle(message("master-openid", "msg-1", "配置列表"));

        verify(c2cMessageSender).replyText(any(C2cMessageCreateDto.class), eq("运行时配置：\nsecret.key"));
    }

    @Test
    void shouldShowConfigValueOnlyForExplicitGetByMaster() {
        QqAdminProperties properties = new QqAdminProperties();
        properties.setMasterOpenids(List.of("master-openid"));
        BotRuntimeConfigService configService = mock(BotRuntimeConfigService.class);
        BotAdminAuditService auditService = mock(BotAdminAuditService.class);
        C2cMessageSender c2cMessageSender = mock(C2cMessageSender.class);
        BotRuntimeConfig config = new BotRuntimeConfig();
        config.setConfigKey("feature.flag");
        config.setConfigValue("enabled");
        when(configService.find("feature.flag")).thenReturn(Optional.of(config));
        C2cAdminConfigService service = new C2cAdminConfigService(
                properties, configService, auditService, c2cMessageSender, mock(ScriptStatusFieldService.class));

        service.handle(message("master-openid", "msg-1", "配置查看 feature.flag"));

        verify(c2cMessageSender).replyText(any(C2cMessageCreateDto.class), eq("feature.flag=enabled"));
        verify(auditService).record(eq("CONFIG"), eq("GET"), eq("feature.flag"), eq("master-openid"),
                eq("C2C"), eq(true), eq(null));
    }

    @Test
    void shouldMaskSensitiveConfigValueWhenExplicitGetByMaster() {
        QqAdminProperties properties = new QqAdminProperties();
        properties.setMasterOpenids(List.of("master-openid"));
        BotRuntimeConfigService configService = mock(BotRuntimeConfigService.class);
        BotAdminAuditService auditService = mock(BotAdminAuditService.class);
        C2cMessageSender c2cMessageSender = mock(C2cMessageSender.class);
        BotRuntimeConfig config = new BotRuntimeConfig();
        config.setConfigKey("qq.secret");
        config.setConfigValue("plain-secret-value");
        when(configService.find("qq.secret")).thenReturn(Optional.of(config));
        C2cAdminConfigService service = new C2cAdminConfigService(
                properties, configService, auditService, c2cMessageSender, mock(ScriptStatusFieldService.class));

        service.handle(message("master-openid", "msg-1", "配置查看 qq.secret"));

        verify(c2cMessageSender).replyText(any(C2cMessageCreateDto.class), eq("qq.secret=******"));
    }

    private C2cMessageCreateDto message(String userOpenid, String messageId, String content) {
        AuthorDto author = new AuthorDto();
        author.setUserOpenid(userOpenid);
        C2cMessageCreateDto dto = new C2cMessageCreateDto();
        dto.setId(messageId);
        dto.setContent(content);
        dto.setAuthor(author);
        return dto;
    }
}