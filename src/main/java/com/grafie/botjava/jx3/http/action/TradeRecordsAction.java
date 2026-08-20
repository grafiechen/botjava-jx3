package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.AppearanceNameAliasService;
import com.grafie.botjava.service.GroupConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 黑市物价查询与图片数据拼装。
 */
@Jx3Action
public class TradeRecordsAction extends Jx3BaseAction {

    private static final Logger log = LoggerFactory.getLogger(TradeRecordsAction.class);

    private final AppearanceNameAliasService appearanceNameAliasService;

    public TradeRecordsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                              GroupConfigurationService groupConfigurationService) {
        this(apiProperties, requestUtil, groupConfigurationService, null);
    }

    @Autowired
    public TradeRecordsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                              GroupConfigurationService groupConfigurationService,
                              AppearanceNameAliasService appearanceNameAliasService) {
        super(apiProperties, requestUtil, groupConfigurationService);
        this.appearanceNameAliasService = appearanceNameAliasService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().server(getDefaultServer()),
                "name", resolveTradeItemNameAlias(currentArguments().roleName())
        );
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
        data.put("mode", "黑市物价");
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", resolveTradeItemNameAlias(currentArguments().roleName()));
        Object resultData = baseResult == null ? null : baseResult.getData();
        data.put("data", resultData);
        if (resultData instanceof OfficialQueryData.TradeRecords tradeRecords) {
            data.put("previewImageDataUri", image(tradeRecords.getView()));
        }
        return data;
    }

    private void importTrustedAliases(BaseResult baseResult) {
        if (appearanceNameAliasService == null || baseResult == null
                || !(baseResult.getData() instanceof OfficialQueryData.TradeRecords tradeRecords)
                || StringUtils.isBlank(tradeRecords.getName())
                || StringUtils.isBlank(tradeRecords.getAlias())) {
            return;
        }
        try {
            AppearanceNameAliasService.TrustedImportResult result =
                    appearanceNameAliasService.importTrustedAliases(
                            tradeRecords.getName(), tradeRecords.getAlias());
            if (!result.createdAliases().isEmpty() || !result.approvedAliases().isEmpty()
                    || !result.conflicts().isEmpty()) {
                log.info("JX3API 外观别名入库，canonicalName=>{}，created=>{}，approved=>{}，conflicts=>{}",
                        result.canonicalName(), result.createdAliases(), result.approvedAliases(), result.conflicts());
            }
        } catch (RuntimeException exception) {
            log.warn("JX3API 外观别名入库失败，canonicalName=>{}，alias=>{}",
                    tradeRecords.getName(), tradeRecords.getAlias(), exception);
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
