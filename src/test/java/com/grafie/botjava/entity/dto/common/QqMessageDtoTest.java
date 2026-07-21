package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.core.type.TypeReference;
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
}
