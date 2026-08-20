package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.AppearanceNameAliasService;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.List;
import java.util.Map;

@Jx3Action
public class AppearanceNameAliasAction extends Jx3BaseAction {

    private final AppearanceNameAliasService aliasService;

    public AppearanceNameAliasAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                     GroupConfigurationService groupConfigurationService,
                                     AppearanceNameAliasService aliasService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.aliasService = aliasService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return switch (currentRegex()) {
            case AppearanceNameAliasAdd -> addAlias();
            case AppearanceNameAliasList -> listAliases();
            case AppearanceNameAliasQuery -> queryAlias();
            case AppearanceNameAliasPendingList -> pendingAliases();
            case AppearanceNameAliasApprove -> approveAlias();
            case AppearanceNameAliasDelete -> deleteAlias();
            default -> BotResponse.text("不支持的外观名称指令。");
        };
    }

    private BotResponse addAlias() {
        AppearanceNameAliasService.AddResult result = aliasService.add(
                currentMessage(), currentArguments().get("name"), currentArguments().get("text"), currentRegex());
        return switch (result.status()) {
            case ADDED -> {
                StringBuilder content = new StringBuilder("外观名称“")
                        .append(result.canonicalName())
                        .append(result.reviewPassed() ? "”已新增" : "”已提交审核");
                if (result.aliases().isEmpty()) {
                    content.append("，没有新增内容。");
                } else {
                    content.append("：").append(String.join("、", result.aliases())).append("。");
                }
                if (!result.unchangedAliases().isEmpty()) {
                    content.append(" 已存在：").append(String.join("、", result.unchangedAliases())).append("。");
                }
                yield BotResponse.text(content.toString());
            }
            case CONFLICT -> BotResponse.text("以下别名已映射到其他外观，请先删除后再追加："
                    + String.join("、", result.conflicts()));
            case PERMISSION_DENIED -> BotResponse.text("暂时无权维护外观名称配置。");
        };
    }

    private BotResponse listAliases() {
        List<AppearanceNameAliasService.AliasGroup> groups = aliasService.list(currentMessage().getGroupOpenid());
        if (groups.isEmpty()) {
            return BotResponse.text("暂无已审核外观名称别名。\n追加方式：外观名称追加 原始名 别名1 别名2");
        }
        return BotResponse.text(renderGroups("全局已审核外观名称列表：", groups));
    }

    private BotResponse pendingAliases() {
        AppearanceNameAliasService.PendingListResult result = aliasService.pendingList(currentMessage(), currentRegex());
        if (result.status() == AppearanceNameAliasService.PendingListResult.Status.PERMISSION_DENIED) {
            return BotResponse.text("暂时无权查看待审核外观名称。");
        }
        if (result.groups().isEmpty()) {
            return BotResponse.text("暂无待审核外观名称别名。");
        }
        return BotResponse.text(renderGroups("全局待审核外观名称列表：", result.groups()));
    }

    private BotResponse approveAlias() {
        AppearanceNameAliasService.ReviewResult result = aliasService.approve(
                currentMessage(), currentArguments().get("name"), currentArguments().get("text"), currentRegex());
        return switch (result.status()) {
            case APPROVED -> BotResponse.text("外观名称“" + result.canonicalName()
                    + "”已审核通过 " + result.approvedCount() + " 个别名。");
            case NOT_FOUND -> BotResponse.text("未找到待审核外观名称或别名“" + result.canonicalName() + "”。");
            case PERMISSION_DENIED -> BotResponse.text("暂时无权审核外观名称。");
        };
    }

    private BotResponse queryAlias() {
        String name = currentArguments().get("name");
        AppearanceNameAliasService.LookupResult result = aliasService.lookup(currentMessage().getGroupOpenid(), name);
        if (!result.found()) {
            return BotResponse.text("未找到已审核外观名称“" + result.queryName() + "”的映射。");
        }
        AppearanceNameAliasService.AliasGroup group = result.group();
        return BotResponse.text(group.canonicalName() + "："
                + (group.aliases().isEmpty() ? "暂无别名" : String.join("、", group.aliases())));
    }

    private BotResponse deleteAlias() {
        AppearanceNameAliasService.DeleteResult result = aliasService.delete(
                currentMessage(), currentArguments().get("name"), currentArguments().get("text"), currentRegex());
        return switch (result.status()) {
            case GROUP_DELETED -> BotResponse.text("外观名称“" + result.canonicalName()
                    + "”及其 " + result.deletedCount() + " 个别名已删除。");
            case ALIAS_DELETED -> BotResponse.text("外观名称“" + result.canonicalName()
                    + "”的别名“" + result.aliasName() + "”已删除。");
            case NOT_FOUND -> BotResponse.text("未找到外观名称或别名“" + result.canonicalName() + "”。");
            case PERMISSION_DENIED -> BotResponse.text("暂时无权维护外观名称配置。");
        };
    }

    private String renderGroups(String title, List<AppearanceNameAliasService.AliasGroup> groups) {
        StringBuilder content = new StringBuilder(title);
        for (int i = 0; i < groups.size(); i++) {
            AppearanceNameAliasService.AliasGroup group = groups.get(i);
            content.append("\n")
                    .append(i + 1).append(". ")
                    .append(group.canonicalName())
                    .append("：")
                    .append(group.aliases().isEmpty() ? "暂无别名" : String.join("、", group.aliases()));
        }
        return content.toString();
    }
}
