package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.entity.dto.qq.QqGroupMessageSendResultDto;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QqMessageDtoTest {

    @Test
    void shouldSerializeMarkdownTemplateWithOfficialFieldNames() {
        MarkdownDto markdown = new MarkdownDto();
        markdown.setCustomTemplateId("template-1");
        markdown.setParams(List.of(new MarkdownDto.Param("title", List.of("开服状态"))));

        Map<String, Object> json = ObjectMapperUtil.getObjectMapper().convertValue(markdown, new TypeReference<>() {
        });

        assertEquals("template-1", json.get("custom_template_id"));
        assertEquals("title", ((Map<?, ?>) ((List<?>) json.get("params")).get(0)).get("key"));
    }

    @Test
    void shouldSerializeCustomKeyboardWithOfficialNestedFields() {
        KeyboardDto.RenderData renderData = new KeyboardDto.RenderData();
        renderData.setLabel("查询开服");
        renderData.setVisitedLabel("已查询");
        renderData.setStyle(1);
        KeyboardDto.Permission permission = new KeyboardDto.Permission();
        permission.setType(2);
        KeyboardDto.Action action = new KeyboardDto.Action();
        action.setType(2);
        action.setPermission(permission);
        action.setData("/开服");
        action.setUnsupportTips("请发送 /开服");
        KeyboardDto.Button button = new KeyboardDto.Button();
        button.setId("status");
        button.setRenderData(renderData);
        button.setAction(action);
        KeyboardDto keyboard = new KeyboardDto();
        keyboard.setContent(new KeyboardDto.Content(List.of(new KeyboardDto.Row(List.of(button)))));

        Map<String, Object> json = ObjectMapperUtil.getObjectMapper().convertValue(keyboard, new TypeReference<>() {
        });
        Map<?, ?> content = (Map<?, ?>) json.get("content");
        Map<?, ?> row = (Map<?, ?>) ((List<?>) content.get("rows")).get(0);
        Map<?, ?> serializedButton = (Map<?, ?>) ((List<?>) row.get("buttons")).get(0);

        assertEquals("已查询", ((Map<?, ?>) serializedButton.get("render_data")).get("visited_label"));
        assertEquals("请发送 /开服", ((Map<?, ?>) serializedButton.get("action")).get("unsupport_tips"));
    }

    @Test
    void shouldSerializeArkTemplateAndObjectList() {
        ArkDto ark = new ArkDto();
        ark.setTemplateId(23);
        ark.setKv(List.of(new ArkDto.KeyValue(
                "#META_LIST#",
                null,
                List.of(new ArkDto.Obj(List.of(new ArkDto.ObjKeyValue("name", "乾坤一掷"))))
        )));

        Map<String, Object> json = ObjectMapperUtil.getObjectMapper().convertValue(ark, new TypeReference<>() {
        });

        assertEquals(23, json.get("template_id"));
        Map<?, ?> kv = (Map<?, ?>) ((List<?>) json.get("kv")).get(0);
        Map<?, ?> obj = (Map<?, ?>) ((List<?>) kv.get("obj")).get(0);
        assertEquals("name", ((Map<?, ?>) ((List<?>) obj.get("obj_kv")).get(0)).get("key"));
    }

    @Test
    void shouldSerializeMessageReferenceWithOfficialFieldNames() {
        TxMessageInfo message = new TxMessageInfo();
        message.setContent("引用回复");
        message.setMsg_type(0);
        message.setMsg_id("source-message");
        message.setMsg_seq(2);
        message.setMessageReference(new MessageReferenceDto("source-message", true));

        Map<String, Object> json = ObjectMapperUtil.getObjectMapper().convertValue(message, new TypeReference<>() {
        });
        Map<?, ?> reference = (Map<?, ?>) json.get("message_reference");

        assertEquals("source-message", json.get("msg_id"));
        assertEquals(2, json.get("msg_seq"));
        assertEquals("source-message", reference.get("message_id"));
        assertEquals(true, reference.get("ignore_get_message_error"));
    }

    @Test
    void shouldSerializeWakeupMessageWithOfficialFieldName() {
        TxMessageInfo message = new TxMessageInfo();
        message.setContent("召回消息");
        message.setMsg_type(0);
        message.setIsWakeup(true);

        Map<String, Object> json = ObjectMapperUtil.getObjectMapper().convertValue(message, new TypeReference<>() {
        });

        assertEquals(true, json.get("is_wakeup"));
    }

    @Test
    void shouldDeserializeOfficialGroupMessageEventAdditions() throws Exception {
        String json = """
                {
                  "id": "message-1",
                  "content": "看看图片",
                  "group_openid": "group-1",
                  "message_type": 103,
                  "author": {
                    "id": "member-1",
                    "username": "加菲",
                    "member_role": "admin",
                    "union_user_account": "union-account-1"
                  },
                  "attachments": [{
                    "content_type": "image/png",
                    "filename": "demo.png",
                    "url": "https://example.com/demo.png",
                    "width": 320,
                    "height": 240
                  }],
                  "mentions": [{
                    "id": "member-2",
                    "username": "小明",
                    "member_openid": "member-2"
                  }],
                  "ark_data": {
                    "prompt": "卡片",
                    "ark_type": "tuwen",
                    "fields": {
                      "title": "标题"
                    }
                  },
                  "msg_elements": [{
                    "msg_idx": "REFIDX_1",
                    "message_type": 0,
                    "content": "被引用消息"
                  }]
                }
                """;

        GroupAtMessageCreateDto message = ObjectMapperUtil.getObjectMapper()
                .readValue(json, GroupAtMessageCreateDto.class);

        assertEquals("group-1", message.getGroupOpenid());
        assertEquals("admin", message.getAuthor().getMemberRole());
        assertEquals("union-account-1", message.getAuthor().getUnionUserAccount());
        assertEquals("image/png", message.getAttachments().get(0).getContentType());
        assertEquals("member-2", message.getMentions().get(0).getMemberOpenid());
        assertEquals("tuwen", message.getArkData().getArkType());
        assertEquals("被引用消息", message.getMsgElements().get(0).getContent());
    }

    @Test
    void shouldDeserializeGroupMessageSendResult() throws Exception {
        String json = """
                {
                  "id": "ROBOT1.0_result",
                  "timestamp": "2026-07-21T10:00:00+08:00",
                  "ext_info": {
                    "ref_idx": "REFIDX_xxx=="
                  }
                }
                """;

        QqGroupMessageSendResultDto result = ObjectMapperUtil.getObjectMapper()
                .readValue(json, QqGroupMessageSendResultDto.class);

        assertEquals("ROBOT1.0_result", result.getId());
        assertEquals("REFIDX_xxx==", result.getExtInfo().getRefIdx());
    }
}
