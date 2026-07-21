package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.Map;

@Jx3Action
public class MechCalculatorAction extends Jx3BaseAction {
    public MechCalculatorAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                                GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
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
        return "副本解密";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        OfficialQueryData.MechCalculator data = baseResult != null
                && baseResult.getData() instanceof OfficialQueryData.MechCalculator value ? value : null;
        if (data == null) {
            return Map.of("data", new MechView(null, null, null, null));
        }
        NodeView current = data.getCurr() == null
                ? new NodeView(data.getLegacyNowNode(), data.getLegacyNowResult()) : node(data.getCurr());
        NodeView next = data.getNext() == null
                ? new NodeView(data.getLegacyNextNode(), data.getLegacyNextResult()) : node(data.getNext());
        String time = data.getTime() == null ? data.getLegacyNowTime() : data.getTime();
        String condition = data.getCdtn() == null ? data.getLegacyIntervalTime() : data.getCdtn();
        return Map.of("data", new MechView(current, next, time, condition));
    }

    private NodeView node(OfficialQueryData.MechNode node) {
        return node == null ? null : new NodeView(node.getNode(), node.getData());
    }

    public record MechView(NodeView current, NodeView next, String time, String condition) {
    }

    public record NodeView(String node, String result) {
    }
}
