package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 鲜花价格
 * @author grafie.chen
 * @since 2025/1/22  17:16
 */
@Jx3Action
public class HomeFlowerAction extends Jx3BaseAction {
    public HomeFlowerAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("server", currentArguments().server(getDefaultServer()));
        if (currentArguments().get("name") != null) {
            params.put("name", currentArguments().get("name"));
        }
        return params;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected BotResponse.ResponseType getResponseType() {
        return BotResponse.ResponseType.IMAGE;
    }

    @Override
    protected String getTemplatePath() {
        return "家园鲜花";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().server(getDefaultServer()));
        template.put("name", currentArguments().get("name"));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<ServerFlowerView> toViews(Object value) {
        if (!(value instanceof OfficialQueryData.HomeFlower result)) {
            return List.of();
        }
        List<ServerFlowerView> servers = new ArrayList<>();
        result.getServers().forEach((server, flowers) -> servers.add(
                new ServerFlowerView(server, flowerViews(flowers))));
        return List.copyOf(servers);
    }

    private List<FlowerView> flowerViews(List<OfficialQueryData.Flower> flowers) {
        if (flowers == null) {
            return List.of();
        }
        List<FlowerView> views = new ArrayList<>();
        for (OfficialQueryData.Flower flower : flowers) {
            if (flower != null) {
                views.add(new FlowerView(flower.getName(), flower.getColor(), flower.getPrice(),
                        flower.getLine() == null ? List.of() : flower.getLine().stream()
                                .filter(line -> line != null && !line.isBlank()).map(String::trim).toList()));
            }
        }
        return List.copyOf(views);
    }

    public record ServerFlowerView(String server, List<FlowerView> flowers) {
    }

    public record FlowerView(String name, String color, Double price, List<String> lines) {
    }
}
