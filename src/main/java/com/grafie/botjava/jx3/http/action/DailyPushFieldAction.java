package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.GroupDailyPushField;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.BotAdminAuditService;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupDailyPushFieldService;

import java.util.List;
import java.util.Map;

@Jx3Action
public class DailyPushFieldAction extends Jx3BaseAction {
    private final GroupDailyPushFieldService fieldService;
    private final BotAdminAuditService auditService;

    public DailyPushFieldAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                GroupConfigurationService groupConfigurationService,
                                GroupDailyPushFieldService fieldService,
                                BotAdminAuditService auditService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.fieldService = fieldService;
        this.auditService = auditService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return switch (currentRegex()) {
            case DailyPushFieldList -> list();
            case DailyPushFieldAdd -> add();
            case DailyPushFieldDelete -> delete();
            default -> BotResponse.text("不支持的日常推送字段操作。");
        };
    }

    private BotResponse list() {
        String groupOpenId = currentMessage().getGroupOpenid();
        List<GroupDailyPushField> fields = fieldService.enabledFields(groupOpenId);
        StringBuilder text = new StringBuilder(fieldService.hasCustomConfiguration(groupOpenId)
                ? "本群日常推送字段：" : "本群日常推送字段（系统默认）：");
        if (fields.isEmpty()) {
            return BotResponse.text(text.append("\n当前未配置任何动态字段，图片只显示区服和角色名。").toString());
        }
        for (GroupDailyPushField field : fields) {
            text.append("\n- ").append(field.getDisplayName()).append("：")
                    .append(field.getMongoFieldName()).append(" [")
                    .append(field.getValueType()).append("]");
        }
        return BotResponse.text(text.toString());
    }

    private BotResponse add() {
        String groupOpenId = currentMessage().getGroupOpenid();
        String actorOpenId = memberOpenId();
        String type = currentArguments().get("type");
        int sortOrder = currentArguments().integer("num", 0, 10000).orElse(100);
        GroupDailyPushField saved = fieldService.save(groupOpenId,
                currentArguments().get("name"), currentArguments().get("name1"),
                type == null ? "TEXT" : type, sortOrder, actorOpenId);
        auditService.record("DAILY_PUSH_FIELD", "UPSERT", groupOpenId + ":" + saved.getMongoFieldName(),
                actorOpenId, "GROUP", true, null);
        return BotResponse.text("已保存日常推送字段：" + saved.getDisplayName() + "（"
                + saved.getMongoFieldName() + "，" + saved.getValueType() + "）");
    }

    private BotResponse delete() {
        String groupOpenId = currentMessage().getGroupOpenid();
        String actorOpenId = memberOpenId();
        String field = currentArguments().get("name");
        boolean removed = fieldService.disable(groupOpenId, field, actorOpenId);
        auditService.record("DAILY_PUSH_FIELD", "DISABLE", groupOpenId + ":" + field,
                actorOpenId, "GROUP", removed, removed ? null : "字段不存在");
        return BotResponse.text(removed ? "已从日常推送中移除字段：" + field : "未找到该日常推送字段。");
    }

    private String memberOpenId() {
        if (currentMessage().getAuthor() == null) {
            return null;
        }
        String memberOpenId = currentMessage().getAuthor().getMemberOpenid();
        return memberOpenId == null || memberOpenId.isBlank()
                ? currentMessage().getAuthor().getId() : memberOpenId;
    }
}