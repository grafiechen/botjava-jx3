package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupRequirementService;

import java.util.List;
import java.util.Map;

@Jx3Action
public class GroupRequirementAction extends Jx3BaseAction {

    private final GroupRequirementService requirementService;

    public GroupRequirementAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                  GroupConfigurationService groupConfigurationService,
                                  GroupRequirementService requirementService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.requirementService = requirementService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return switch (currentRegex()) {
            case GroupRequirementList -> listRequirements();
            case GroupRequirementCreate -> createRequirement();
            case GroupRequirementRename -> renameRequirement();
            case GroupRequirementStatus -> updateRequirementStatus();
            case GroupRequirementAppend -> appendRequirement();
            case GroupRequirementCancel -> cancelRequirement();
            case GroupRequirementDelete -> deleteRequirement();
            case GroupRequirementDetail -> requirementDetail();
            default -> BotResponse.text("不支持的需求指令。");
        };
    }

    private BotResponse listRequirements() {
        List<GroupRequirementService.RequirementSummary> summaries = requirementService.list(
                currentMessage().getGroupOpenid());
        if (summaries.isEmpty()) {
            return BotResponse.text("本群暂无需求列表。\n创建方式：创建需求 需求名称");
        }
        StringBuilder content = new StringBuilder("本群需求列表：");
        for (int i = 0; i < summaries.size(); i++) {
            GroupRequirementService.RequirementSummary summary = summaries.get(i);
            content.append("\n")
                    .append(i + 1).append(". ")
                    .append(summary.title())
                    .append("（").append(summary.itemCount()).append("条）");
        }
        return BotResponse.text(content.toString());
    }

    private BotResponse createRequirement() {
        String title = currentArguments().get("name");
        GroupRequirementService.CreateResult result = requirementService.create(currentMessage(), title);
        if (!result.created()) {
            return BotResponse.text("需求“" + result.title() + "”已存在。");
        }
        return BotResponse.text("需求“" + result.title() + "”创建成功。");
    }

    private BotResponse renameRequirement() {
        String oldTitle = currentArguments().get("name");
        String newTitle = currentArguments().get("name1");
        GroupRequirementService.RenameResult result = requirementService.rename(
                currentMessage(), oldTitle, newTitle);
        return switch (result.status()) {
            case RENAMED -> BotResponse.text("需求“" + result.oldTitle()
                    + "”已修改为“" + result.newTitle() + "”。");
            case NOT_FOUND -> BotResponse.text("未找到需求“" + result.oldTitle() + "”。");
            case NOT_OWNER -> BotResponse.text("只有群主、群管理或需求创建者可以修改需求。");
            case DUPLICATED -> BotResponse.text("需求“" + result.newTitle() + "”已存在。");
        };
    }

    private BotResponse updateRequirementStatus() {
        String title = currentArguments().get("name");
        String itemContent = currentArguments().get("item");
        String status = currentArguments().get("status");
        GroupRequirementService.StatusResult result = requirementService.updateStatus(
                currentMessage(), title, itemContent, status);
        return switch (result.status()) {
            case UPDATED -> BotResponse.text("需求“" + result.title()
                    + "”下“" + result.itemContent() + "”的状态已更新为：“" + result.itemStatus() + "”。");
            case NOT_FOUND -> BotResponse.text("未找到需求“" + result.title() + "”。");
            case ITEM_NOT_FOUND -> BotResponse.text("需求“" + result.title()
                    + "”下未找到追加内容“" + result.itemContent() + "”。");
            case DUPLICATED -> BotResponse.text("需求“" + result.title()
                    + "”下有多条追加内容都叫“" + result.itemContent() + "”，暂时无法判断要修改哪一条。");
            case NOT_OWNER -> BotResponse.text("只有群主、群管理或需求创建者可以修改追加内容状态。");
        };
    }

    private BotResponse appendRequirement() {
        String title = currentArguments().get("name");
        String description = currentArguments().get("text");
        GroupRequirementService.AppendResult result = requirementService.append(
                currentMessage(), title, description);
        if (!result.appended()) {
            return BotResponse.text("未找到需求“" + result.title() + "”，请先让群主或管理员创建。");
        }
        return BotResponse.text("已追加到需求“" + result.title() + "”，当前共 "
                + result.itemCount() + " 条。");
    }

    private BotResponse cancelRequirement() {
        String title = currentArguments().get("name");
        int itemIndex = currentArguments().integer("num", 1, Integer.MAX_VALUE).orElseThrow();
        GroupRequirementService.CancelResult result = requirementService.cancelOwnItem(
                currentMessage(), title, itemIndex);
        return switch (result.status()) {
            case CANCELED -> BotResponse.text("已取消需求“" + result.title()
                    + "”下第 " + result.itemIndex() + " 条追加内容。");
            case REQUIREMENT_NOT_FOUND -> BotResponse.text("未找到需求“" + result.title() + "”。");
            case ITEM_NOT_FOUND -> BotResponse.text("需求“" + result.title()
                    + "”下没有第 " + result.itemIndex() + " 条追加内容。");
            case NOT_OWNER -> BotResponse.text("只有群主、群管理、需求创建者或追加内容本人可以取消。");
        };
    }

    private BotResponse deleteRequirement() {
        String title = currentArguments().get("name");
        GroupRequirementService.DeleteResult result = requirementService.deleteOwnRequirement(
                currentMessage(), title);
        return switch (result.status()) {
            case DELETED -> BotResponse.text("需求“" + result.title() + "”已删除。");
            case NOT_FOUND -> BotResponse.text("未找到需求“" + result.title() + "”。");
            case NOT_OWNER -> BotResponse.text("只有群主、群管理或需求创建者可以删除需求。");
        };
    }

    private BotResponse requirementDetail() {
        String title = currentArguments().get("name");
        GroupRequirementService.RequirementDetail detail = requirementService.detail(
                currentMessage().getGroupOpenid(), title);
        if (detail == null) {
            return BotResponse.text("未找到需求“" + title + "”。");
        }
        StringBuilder content = new StringBuilder("需求详情：")
                .append(detail.title());
        if (detail.items().isEmpty()) {
            return BotResponse.text(content.append("\n暂无追加内容。").toString());
        }
        for (int i = 0; i < detail.items().size(); i++) {
            GroupRequirementService.RequirementItemView item = detail.items().get(i);
            content.append("\n")
                    .append(i + 1).append(". ")
                    .append(item.memberName() == null || item.memberName().isBlank()
                            ? "未知群员" : item.memberName().trim())
                    .append("：")
                    .append(item.content() == null || item.content().isBlank()
                            ? "无描述" : item.content().trim())
                    .append("（状态：")
                    .append(displayStatus(item.status()))
                    .append("）");
        }
        return BotResponse.text(content.toString());
    }

    private String displayStatus(String status) {
        return status == null || status.isBlank() ? "未标记" : status.trim();
    }
}
