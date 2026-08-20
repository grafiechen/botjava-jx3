package com.grafie.botjava.jx3.http.action.base;


import com.fasterxml.jackson.databind.JsonNode;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.jx3.http.command.CommandArgumentException;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import java.util.Iterator;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 基础类
 *
 * @author grafie.chen
 * @since 2025/1/22  16:19
 */

@Slf4j
public abstract class Jx3BaseAction {

    private static final int MAX_TEXT_FIELDS = 12;
    private static final Set<String> HIDDEN_TEXT_FIELDS = Set.of(
            "id", "token", "url", "icon", "avatar", "avatarUrl", "personAvatar",
            "roleId", "globalRoleId", "forceId", "bodyId", "tongId", "campId", "personId"
    );
    private static final Map<String, String> TEXT_FIELD_NAMES = Map.ofEntries(
            Map.entry("zone", "区域"), Map.entry("zoneName", "区域"),
            Map.entry("server", "服务器"), Map.entry("serverName", "服务器"),
            Map.entry("name", "名称"), Map.entry("roleName", "角色"),
            Map.entry("title", "标题"), Map.entry("text", "内容"),
            Map.entry("event", "奇遇"), Map.entry("level", "等级"),
            Map.entry("status", "状态"), Map.entry("date", "日期"),
            Map.entry("time", "时间"), Map.entry("start", "开始时间"),
            Map.entry("end", "结束时间"), Map.entry("update", "更新时间"),
            Map.entry("map", "地图"), Map.entry("mapName", "地图"),
            Map.entry("sender", "发送者"), Map.entry("receive", "接收者"),
            Map.entry("recipient", "接收者"), Map.entry("content", "内容")
    );

    protected ApiProperties apiProperties;
    protected Jx3RequestUtil jx3RequestUtil;
    private final GroupConfigurationService groupConfigurationService;
    private final ThreadLocal<ActionContext> actionContext = new ThreadLocal<>();

    public Jx3BaseAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                         GroupConfigurationService groupConfigurationService) {
        this.apiProperties = apiProperties;
        this.jx3RequestUtil = jx3RequestUtil;
        this.groupConfigurationService = groupConfigurationService;
    }

    /**
     * 需要按照不同调用方法的顺序进行传参
     *
     * @param requestRegex 请求参数
     * @param regex        方法枚举
     * @return 返回结果
     */
    public BotResponse doRequest(GroupAtMessageCreateDto atMessageCreateDto, String requestRegex, REGEX regex) {
        return doRequest(atMessageCreateDto, requestRegex, regex,
                CommandArguments.of(regex.handleEncounter(requestRegex)));
    }

    public BotResponse doRequest(GroupAtMessageCreateDto atMessageCreateDto, String requestRegex, REGEX regex,
                                 CommandArguments arguments) {
        actionContext.set(new ActionContext(atMessageCreateDto, requestRegex, regex, arguments));
        try {
            return doAction(requestRegex, regex);
        } finally {
            actionContext.remove();
        }
    }

    public BotResponse doAction(String requestRegex, REGEX regex) {
        // 当method为空时，说明不需要调用外部接口。直接在具体实现类里面进行处理即可
        if (regex.getMethodEnum() == null) {
            return buildResponse(null);
        }
        String requestId = RequestTraceContext.currentId()
                .orElseGet(() -> UUID.randomUUID().toString().substring(0, 8));
        long startNanos = System.nanoTime();
        log.info("开始调用 JX3API，requestId=>{}，command=>{}，path=>{}",
                requestId, regex.name(), regex.getMethodEnum().getMethodPath());
        try {
            Map<String, Object> requestParam = getRequestParam(requestRegex, regex);
            if (requestParam == null) {
                requestParam = Map.of();
            }
            RequestResult requestResult = HttpMethod.GET.equals(regex.getMethodEnum().getHttpMethod())
                    ? jx3RequestUtil.doGetRequest(regex.getMethodEnum(), requestParam)
                    : jx3RequestUtil.doPostRequest(regex.getMethodEnum().getMethodPath(), requestParam);
            BaseResult baseResult = jx3RequestUtil.getResultRealData(requestResult, regex.getMethodEnum());
            if (baseResult == null) {
                return Jx3ApiFailureMapper.fromApiResult(null, null, requestId);
            }
            if (baseResult.getCode() != null && baseResult.getCode() != 200) {
                return Jx3ApiFailureMapper.fromApiResult(baseResult.getCode(), baseResult.getMsg(), requestId);
            }
            if (isEmptyData(baseResult.getData())) {
                return Jx3ApiFailureMapper.empty(requestId);
            }
            return buildResponse(baseResult);
        } catch (CommandArgumentException e) {
            log.info("JX3 指令参数无效，requestId=>{}，command=>{}，reason=>{}",
                    requestId, regex.name(), SensitiveDataUtil.redactText(e.getMessage()));
            return BotResponse.text("指令参数有误：" + e.getMessage());
        } catch (Exception e) {
            log.error("调用 JX3API 失败，requestId=>{}，command=>{}，reason=>{}",
                    requestId, regex.name(), SensitiveDataUtil.summarize(e), e);
            return Jx3ApiFailureMapper.fromException(e, requestId);
        } finally {
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("结束调用 JX3API，requestId=>{}，command=>{}，elapsedMs=>{}",
                    requestId, regex.name(), elapsedMillis);
        }
    }

    private boolean isEmptyData(Object data) {
        if (data == null) {
            return true;
        }
        if (data instanceof CharSequence sequence) {
            return sequence.toString().isBlank();
        }
        if (data instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (data instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return data.getClass().isArray() && Array.getLength(data) == 0;
    }

    private BotResponse buildResponse(BaseResult baseResult) {
        BotResponse response = dealAfterJx3ApiRequest(baseResult);
        return response == null
                ? BotResponse.text("查询成功，但暂时没有可展示的数据。")
                : response;
    }

    /**
     * 交给各个子类处理的生成请求参数的基类
     *
     * @param requestRegex qq发过来的表达式
     * @param regex        识别到的枚举
     * @return
     */
    protected abstract Map<String, Object> getRequestParam(String requestRegex, REGEX regex);

    /**
     * 请求jx3Api之后，过来的
     *
     * @param baseResult 需要拼装成真正返回值的内容
     * @return
     */
    protected abstract BotResponse dealAfterJx3ApiRequest(BaseResult baseResult);

    protected BotResponse buildMessageByTemplate(BaseResult baseResult) {
        if (getResponseType() == BotResponse.ResponseType.IMAGE) {
            return BotResponse.image(getTemplatePath(), buildTemplateData(baseResult));
        }
        return BotResponse.text(buildTextContent(baseResult));
    }

    protected BotResponse.ResponseType getResponseType() {
        return BotResponse.ResponseType.TEXT;
    }

    protected String buildTextContent(BaseResult baseResult) {
        throw new UnsupportedOperationException("请在子类中实现文本内容拼装");
    }

    protected String getTemplatePath() {
        throw new UnsupportedOperationException("请在子类中指定 HTML 模板路径");
    }

    protected Object buildTemplateData(BaseResult baseResult) {
        throw new UnsupportedOperationException("请在子类中实现 Vue 模板数据拼装");
    }


    /**
     * 物价类查询的物品别名解析入口。
     * 当前未接入配置，直接返回传入名称；后续可在这里接数据库或群配置别名。
     */
    protected String resolveTradeItemNameAlias(String itemName) {
        return itemName;
    }

    protected BotResponse buildTextMessage(String content) {
        return BotResponse.text(content);
    }

    protected BotResponse buildReadableText(String title, BaseResult baseResult) {
        if (baseResult == null || baseResult.getData() == null) {
            return BotResponse.text(title + "：暂无数据。");
        }
        JsonNode root = ObjectMapperUtil.getObjectMapper().valueToTree(baseResult.getData());
        Set<String> lines = new LinkedHashSet<>();
        collectReadableFields(root, null, lines);
        if (lines.isEmpty()) {
            return BotResponse.text(title + "：暂无可展示数据。");
        }
        return BotResponse.text(title + "\n" + String.join("\n", lines));
    }

    protected Map<String, Object> buildStandardTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", currentArguments().roleName());
        data.put("data", baseResult == null ? null : baseResult.getData());
        return data;
    }

    protected String resolveServer(Map<String, String> arguments) {
        return firstNotBlank(arguments.get("server"), arguments.get("server1"), getDefaultServer());
    }

    protected String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void collectReadableFields(JsonNode node, String fieldName, Set<String> lines) {
        if (node == null || node.isNull() || lines.size() >= MAX_TEXT_FIELDS) {
            return;
        }
        if (node.isValueNode()) {
            if (fieldName == null || HIDDEN_TEXT_FIELDS.contains(fieldName)) {
                return;
            }
            String value = node.asText().trim();
            if (!value.isEmpty() && !value.startsWith("http://") && !value.startsWith("https://")) {
                if (value.length() > 120) {
                    value = value.substring(0, 117) + "...";
                }
                lines.add(TEXT_FIELD_NAMES.getOrDefault(fieldName, fieldName) + "：" + value);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectReadableFields(child, fieldName, lines);
                if (lines.size() >= MAX_TEXT_FIELDS) {
                    return;
                }
            }
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext() && lines.size() < MAX_TEXT_FIELDS) {
            Map.Entry<String, JsonNode> field = fields.next();
            collectReadableFields(field.getValue(), field.getKey(), lines);
        }
    }

    /**
     * 设置区服
     */

    public String getDefaultServer() {
        return groupConfigurationService.findServer(currentMessage().getGroupOpenid())
                .orElse(apiProperties.getDefaultServer());
    }

    protected GroupAtMessageCreateDto currentMessage() {
        return currentContext().message();
    }

    protected String currentRequestRegex() {
        return currentContext().requestRegex();
    }

    protected REGEX currentRegex() {
        return currentContext().regex();
    }

    protected CommandArguments currentArguments() {
        return currentContext().arguments();
    }

    private ActionContext currentContext() {
        ActionContext context = actionContext.get();
        if (context == null) {
            throw new IllegalStateException("当前线程不存在 JX3 指令上下文");
        }
        return context;
    }

    private record ActionContext(GroupAtMessageCreateDto message, String requestRegex, REGEX regex,
                                 CommandArguments arguments) {
    }
}
