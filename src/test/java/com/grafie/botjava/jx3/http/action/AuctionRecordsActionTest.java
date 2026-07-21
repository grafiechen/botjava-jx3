package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.acution.AcutionRecordsData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuctionRecordsActionTest {

    @Test
    void shouldDeserializeCurrentOfficialFieldsAndLegacyAliases() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        AcutionRecordsData current = mapper.readValue("""
                {"map_name":"25人普通会战弓月城","item_name":"昆玉玄晶","item_amount":"1"}
                """, AcutionRecordsData.class);
        AcutionRecordsData legacy = mapper.readValue("""
                {"name":"沉沙玄晶","amount":"2"}
                """, AcutionRecordsData.class);

        assertEquals("25人普通会战弓月城", current.getMapName());
        assertEquals("昆玉玄晶", current.getItemName());
        assertEquals("1", current.getItemAmount());
        assertEquals("沉沙玄晶", legacy.getItemName());
        assertEquals("2", legacy.getItemAmount());
    }

    @Test
    void shouldBuildAuctionImageWithReadableOfficialFields() {
        AcutionRecordsData record = new AcutionRecordsData();
        record.setId(1594L);
        record.setZone("电信区");
        record.setServer("唯我独尊");
        record.setMapName("25人普通会战弓月城");
        record.setRoleName("醉卧青苔");
        record.setItemName("昆玉玄晶");
        record.setItemAmount("1");
        record.setTime(1764385748L);

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(record));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.AuctionRecords.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        AuctionRecordsAction action = new AuctionRecordsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(
                message(), "拍卖 唯我独尊 玄晶", REGEX.AuctionRecords);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.AuctionRecords.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "唯我独尊", "name", "玄晶", "limit", 20), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("阵营拍卖", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("唯我独尊", templateData.get("server"));
        assertEquals("玄晶", templateData.get("name"));
        AuctionRecordsAction.AuctionRecordView view =
                (AuctionRecordsAction.AuctionRecordView) ((List<?>) templateData.get("data")).get(0);
        assertEquals("25人普通会战弓月城", view.mapName());
        assertEquals("昆玉玄晶", view.itemName());
        assertEquals("1", view.itemAmount());
        assertEquals("醉卧青苔", view.roleName());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
