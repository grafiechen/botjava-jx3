package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.trade.record.TradeRecordData;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.AppearanceNameAliasService;
import com.grafie.botjava.service.GroupConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Jx3Action

/**
 * 物品价格
 * @author grafie.chen
 * @since 2025/1/22  17:24
 */
public class TradeRecordAction extends Jx3BaseAction {

    private static final Logger log = LoggerFactory.getLogger(TradeRecordAction.class);

    private final AppearanceNameAliasService appearanceNameAliasService;

    public TradeRecordAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                             GroupConfigurationService groupConfigurationService) {
        this(apiProperties, jx3RequestUtil, groupConfigurationService, null);
    }

    @Autowired
    public TradeRecordAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                             GroupConfigurationService groupConfigurationService,
                             AppearanceNameAliasService appearanceNameAliasService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.appearanceNameAliasService = appearanceNameAliasService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> requestMap = new HashMap<>();
        String server = currentArguments().server(getDefaultServer());
        if (StringUtils.isNotBlank(server)) {
            requestMap.put("server", server);
        }
        requestMap.put("name", resolveTradeItemNameAlias(currentArguments().value()));
        return requestMap;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        importTrustedAliases(baseResult);
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected BotResponse.ResponseType getResponseType() {
        return BotResponse.ResponseType.IMAGE;
    }

    @Override
    protected String getTemplatePath() {
        return "物品价格";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", "物品价格");
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", resolveTradeItemNameAlias(currentArguments().value()));
        Object resultData = baseResult == null ? null : baseResult.getData();
        data.put("data", resultData);
        if (resultData instanceof TradeRecordData tradeRecordData) {
            data.put("previewImageDataUri", image(tradeRecordData.getView()));
        }
        return data;
    }

    private void importTrustedAliases(BaseResult baseResult) {
        if (appearanceNameAliasService == null || baseResult == null
                || !(baseResult.getData() instanceof TradeRecordData tradeRecordData)
                || StringUtils.isBlank(tradeRecordData.getName())
                || StringUtils.isBlank(tradeRecordData.getAlias())) {
            return;
        }
        try {
            AppearanceNameAliasService.TrustedImportResult result =
                    appearanceNameAliasService.importTrustedAliases(
                            tradeRecordData.getName(), tradeRecordData.getAlias());
            if (!result.createdAliases().isEmpty() || !result.approvedAliases().isEmpty()
                    || !result.conflicts().isEmpty()) {
                log.info("JX3API 外观别名入库，canonicalName=>{}，created=>{}，approved=>{}，conflicts=>{}",
                        result.canonicalName(), result.createdAliases(), result.approvedAliases(), result.conflicts());
            }
        } catch (RuntimeException exception) {
            log.warn("JX3API 外观别名入库失败，canonicalName=>{}，alias=>{}",
                    tradeRecordData.getName(), tradeRecordData.getAlias(), exception);
        }
    }

    private String image(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return jx3RequestUtil.loadRemoteImageDataUri(url).orElse(null);
    }
    @Override
    protected String resolveTradeItemNameAlias(String itemName) {
        if (appearanceNameAliasService == null) {
            return super.resolveTradeItemNameAlias(itemName);
        }
        return appearanceNameAliasService.resolve(currentMessage().getGroupOpenid(), itemName);
    }
}
