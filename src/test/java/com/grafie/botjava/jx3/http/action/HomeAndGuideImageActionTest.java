package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.home.HomeFurnitureData;
import com.grafie.botjava.jx3.http.data.home.HomeTravelData;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeAndGuideImageActionTest {

    @Test
    void shouldBuildSkillAndFoodViewsWithoutIdsOrUrls() {
        OfficialQueryData.SkillRework rework = new OfficialQueryData.SkillRework();
        rework.setId("1335074");
        rework.setTitle("山海源流资料片武学调整");
        rework.setUrl("https://example.invalid/rework");
        rework.setTime("2025-11-17 08:52:08");
        OfficialQueryData.SchoolFood food = new OfficialQueryData.SchoolFood();
        food.setId(1);
        food.setSchool("万花");
        food.setKungfu("花间游");
        food.setColor("紫");
        food.setCategory("增强食品");
        food.setName("风语·灌汤包");
        food.setBoost("内功");

        SkillReworkAction.ReworkView reworkView = firstView(
                execute(REGEX.SkillRework, "技改", List.of(rework)), SkillReworkAction.ReworkView.class);
        SchoolFoodsAction.FoodView foodView = firstView(
                execute(REGEX.SchoolFoods, "小药", List.of(food)), SchoolFoodsAction.FoodView.class);

        assertEquals("山海源流资料片武学调整", reworkView.title());
        assertRecordExcludes(reworkView, "id", "url");
        assertEquals("花间游", foodView.kungfu());
        assertRecordExcludes(foodView, "id");
    }

    @Test
    void shouldBuildHomeViewsWithoutInternalCodesOrExternalImages() {
        HomeFurnitureData furniture = new HomeFurnitureData();
        furniture.setId(5);
        furniture.setName("龙门香梦");
        furniture.setType(1);
        furniture.setColor(4);
        furniture.setArchitecture(0);
        furniture.setSource("大水南方令");
        furniture.setLimit(10);
        furniture.setQuality(1000);
        furniture.setView(7931);
        furniture.setPractical(3525);
        furniture.setHard(3525);
        furniture.setGeomantic(3525);
        furniture.setInteresting(3525);
        furniture.setImage("https://example.invalid/furniture.png");
        furniture.setTip("跳影如流泉，香梦过龙门。");
        HomeTravelData travel = new HomeTravelData();
        travel.setId(591);
        travel.setName("日晷");
        travel.setSource("宠物游历");
        travel.setProduce("万花");
        travel.setLimit(3);
        travel.setQuality(615);
        travel.setView(484);
        travel.setPractical(1743);
        travel.setHard(500);
        travel.setGeomantic(500);
        travel.setImage("https://example.invalid/travel.png");
        travel.setTip("产出地图：万花");

        HomeFurnitureAction.FurnitureView furnitureView = firstView(
                execute(REGEX.HomeFurniture, "家具 龙门香梦", List.of(furniture)),
                HomeFurnitureAction.FurnitureView.class);
        HomeTravelAction.TravelView travelView = firstView(
                execute(REGEX.HomeTravel, "器物 万花", List.of(travel)), HomeTravelAction.TravelView.class);

        assertEquals("龙门香梦", furnitureView.name());
        assertEquals("万花", travelView.produce());
        assertRecordExcludes(furnitureView, "id", "type", "color", "architecture", "image");
        assertRecordExcludes(travelView, "id", "type", "color", "architecture", "image");
    }

    private BotResponse execute(REGEX regex, String command, Object apiData) {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, regex.getMethodEnum())).thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3BaseAction action = switch (regex) {
            case SkillRework -> new SkillReworkAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case SchoolFoods -> new SchoolFoodsAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case HomeFurniture -> new HomeFurnitureAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case HomeTravel -> new HomeTravelAction(properties, requestUtil, mock(GroupConfigurationService.class));
            default -> throw new IllegalArgumentException("不支持的测试指令：" + regex);
        };

        BotResponse response = action.doRequest(message(), command, regex);
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        return response;
    }

    private <T> T firstView(BotResponse response, Class<T> viewType) {
        Map<?, ?> template = assertInstanceOf(Map.class, response.getTemplateData());
        return assertInstanceOf(viewType, ((List<?>) template.get("data")).get(0));
    }

    private void assertRecordExcludes(Object record, String... names) {
        List<String> excluded = List.of(names);
        assertFalse(Arrays.stream(record.getClass().getRecordComponents())
                .anyMatch(component -> excluded.contains(component.getName())));
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
