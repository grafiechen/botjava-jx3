package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.trade.WanbaolouData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.HtmlToImageUtl;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WanbaolouActionTest {

    @Test
    void shouldSendRoleIdAsGetQueryAndBuildImageResponse() {
        ApiProperties properties = new ApiProperties();
        properties.setApiV2Token("level-two-token");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(sampleData());
        when(requestUtil.doGetRequest(eq(MethodEnum.DATA_TRADE_WANBAOLOU), anyMap()))
                .thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, MethodEnum.DATA_TRADE_WANBAOLOU))
                .thenReturn(baseResult);
        WanbaolouAction action = new WanbaolouAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(),
                "编号搜索 1405435120446099456", REGEX.Wanbaolou);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doGetRequest(eq(MethodEnum.DATA_TRADE_WANBAOLOU), params.capture());
        assertEquals(Map.of("id", "1405435120446099456"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("万宝楼", response.getTemplateName());
        @SuppressWarnings("unchecked")
        Map<String, Object> view = (Map<String, Object>) response.getTemplateData();
        assertEquals("公示", view.get("tradeStatusText"));
        assertEquals("08/17 14:19", view.get("replyTimeText"));
        @SuppressWarnings("unchecked")
        List<WanbaolouAction.DetailRow> details =
                (List<WanbaolouAction.DetailRow>) view.get("details");
        assertEquals(4, details.size());
        assertEquals("成衣", details.get(3).label());
        assertTrue(details.get(3).content().contains("盒子："));
    }

    @Test
    void shouldRenderWanbaolouVueTemplate() throws Exception {
        WanbaolouData data = sampleData();
        Map<String, Object> view = Map.of(
                "data", data,
                "details", WanbaolouAction.parseDetails(data.getReplyContent()),
                "tradeStatusText", "公示",
                "replyTimeText", "08/17 14:19"
        );
        Path output = Path.of("target", "test-output", "html-image", "万宝楼.png");
        Files.createDirectories(output.getParent());

        HtmlToImageUtl.renderTemplateToImage("万宝楼", view, output.toString());

        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() >= 1180);
        assertTrue(image.getHeight() > 700);
        assertTrue(Files.size(output) > 20_000);
    }

    @Test
    void shouldDeserializeOptionalFieldsAndObjectPriceHistory() throws Exception {
        String json = """
                {
                  "id": "1397858208903151616",
                  "priceNum": 1488,
                  "updatePrices": [{
                    "accoSeq": "1397858208903151616",
                    "createtime": 1785343254000,
                    "updatePrice": 1888,
                    "updateTime": 1785141010000,
                    "zhanghaoId": "14953339700512",
                    "futureField": "ignored"
                  }],
                  "anotherFutureField": true
                }
                """;

        WanbaolouData data = ObjectMapperUtil.readValue(json, WanbaolouData.class);

        assertEquals("1397858208903151616", data.getId());
        assertEquals(1488, data.getPriceNum());
        assertEquals(1, data.getUpdatePrices().size());
        assertEquals(1888, data.getUpdatePrices().get(0).getUpdatePrice());
        assertEquals(1785141010000L, data.getUpdatePrices().get(0).getUpdateTime());
        assertEquals(null, data.getServerName());
    }
    @Test
    void shouldDeclareOfficialGetAndLevelTwoToken() {
        assertEquals("/trade/wanbaolou", MethodEnum.DATA_TRADE_WANBAOLOU.getMethodPath());
        assertEquals(org.springframework.http.HttpMethod.GET,
                MethodEnum.DATA_TRADE_WANBAOLOU.getHttpMethod());
        assertEquals(2, MethodEnum.DATA_TRADE_WANBAOLOU.getApiLevel());
        assertTrue(MethodEnum.DATA_TRADE_WANBAOLOU.isV2TokenRequired());
    }

    private WanbaolouData sampleData() {
        WanbaolouData data = new WanbaolouData();
        data.setId("1405435120446099456");
        data.setServerName("梦江南");
        data.setRoleName("心***");
        data.setRoleLevel(130);
        data.setFollowNum(0);
        data.setForceName("五毒");
        data.setBodyName("萝莉");
        data.setMeetingNum(43);
        data.setCampName("浩气盟");
        data.setEquipScore("771856");
        data.setSeniorityNum(76140);
        data.setPriceNum(4666);
        data.setTradeStatus(3);
        data.setReplyTitle("【电信双梦毒萝4K6】五红蓝葵蓝发蝶粉红寻香红发");
        data.setReplyContent("【区服】电信电信梦江南<br>【门派】五毒萝莉<br>"
                + "【阵营】浩气盟2阶<br>【成衣】总计58件<br>盒子：[金琼枝盒, 紫人间盒]");
        data.setReplyTime(1786947585L);
        return data;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}