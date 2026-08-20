package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.GroupDailyPushField;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.service.GroupDailyPushFieldService;
import com.grafie.botjava.service.LuaRoleStatusStore;
import com.grafie.botjava.service.MongoDisplayValueFormatter;
import com.grafie.botjava.service.UserCommandPreferenceService;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "bot.mongodb", name = "enabled", havingValue = "true")
public class MongoDailyProgressPushTask implements ScheduledGroupPushTask {
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final PushTaskDefinition definition;
    private final ScheduledGroupPushRunner runner;
    private final UserCommandPreferenceService preferenceService;
    private final GroupDailyPushFieldService fieldService;
    private final LuaRoleStatusStore store;
    private final MongoDisplayValueFormatter formatter;
    private final ZoneId zoneId;

    public MongoDailyProgressPushTask(PushTaskRegistry registry,
                                      ScheduledGroupPushRunner runner,
                                      UserCommandPreferenceService preferenceService,
                                      GroupDailyPushFieldService fieldService,
                                      LuaRoleStatusStore store,
                                      MongoDisplayValueFormatter formatter,
                                      @Value("${bot.push.mongo-daily.zone:Asia/Tokyo}") String zone) {
        this.definition = registry.find(PushTaskRegistry.MONGO_DAILY_PROGRESS).orElseThrow();
        this.runner = runner;
        this.preferenceService = preferenceService;
        this.fieldService = fieldService;
        this.store = store;
        this.formatter = formatter;
        this.zoneId = ZoneId.of(zone);
    }

    @Scheduled(cron = "${bot.push.mongo-daily.cron:0 0 10 * * *}",
            zone = "${bot.push.mongo-daily.zone:Asia/Tokyo}")
    public void publishAtTen() {
        runner.run(this);
    }

    @Override
    public PushTaskDefinition definition() {
        return definition;
    }

    @Override
    public BotResponse buildResponse(String groupOpenId) {
        List<UserRoleBinding> bindings = preferenceService.findGroupBindings(groupOpenId);
        if (bindings.isEmpty()) {
            log.info("Mongo 日常进度跳过未绑定角色的已订阅群");
            return null;
        }
        List<GroupDailyPushField> fields = fieldService.enabledFields(groupOpenId);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, UserRoleBinding> distinctRoles = new LinkedHashMap<>();
        for (UserRoleBinding binding : bindings) {
            distinctRoles.putIfAbsent(binding.getServer() + "\u0000" + binding.getRoleName(), binding);
        }
        for (UserRoleBinding binding : distinctRoles.values()) {
            appendRoleRows(binding, fields, rows);
        }
        if (rows.isEmpty()) {
            log.info("Mongo 日常进度跳过没有脚本记录的已订阅群，bindingCount=>{}", bindings.size());
            return null;
        }
        List<Map<String, String>> columns = fields.stream()
                .map(field -> Map.of("field", field.getMongoFieldName(), "name", field.getDisplayName()))
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", GENERATED_AT.format(ZonedDateTime.now(zoneId)));
        data.put("columns", columns);
        data.put("rows", rows);
        data.put("roleCount", distinctRoles.size());
        data.put("recordCount", rows.size());
        return BotResponse.image("日常进度", data);
    }

    private void appendRoleRows(UserRoleBinding binding, List<GroupDailyPushField> fields,
                                List<Map<String, Object>> rows) {
        try {
            List<Document> documents = store.findByServerAndRoleName(binding.getServer(), binding.getRoleName());
            for (int index = 0; index < documents.size(); index++) {
                Document document = documents.get(index);
                List<String> values = fields.stream()
                        .map(field -> formatter.format(document.get(field.getMongoFieldName()), field.getValueType()))
                        .toList();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("server", binding.getServer());
                row.put("roleName", binding.getRoleName());
                row.put("recordIndex", documents.size() > 1 ? index + 1 : null);
                row.put("values", values);
                rows.add(row);
            }
        } catch (RuntimeException exception) {
            log.error("Mongo 日常进度读取单个绑定失败，reason=>{}",
                    SensitiveDataUtil.summarize(exception), exception);
        }
    }
}