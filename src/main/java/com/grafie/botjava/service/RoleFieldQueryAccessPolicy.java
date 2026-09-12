package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Component
public class RoleFieldQueryAccessPolicy {

    private static final List<String> SENSITIVE_FRAGMENTS = List.of(
            "账号", "账户", "用户名", "用户id", "全局id", "角色id", "openid", "userid",
            "memberid", "unionid", "手机号", "电话号码", "身份证", "邮箱", "qq号", "qqid",
            "account", "username", "password", "passwd", "pwd", "secret", "token", "credential",
            "authorization", "cookie", "ticket", "accesskey", "privatekey"
    );

    private final BotMongoProperties properties;

    public RoleFieldQueryAccessPolicy(BotMongoProperties properties) {
        this.properties = properties;
    }

    public String requireQueryableField(String value) {
        String field = clean(value);
        if (field == null) {
            throw new IllegalArgumentException("查询字段不能为空。");
        }
        if (field.length() > 100) {
            throw new IllegalArgumentException("查询字段不能超过 100 个字符。");
        }
        if (field.contains(".") || field.startsWith("$") || field.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("只能查询 MongoDB 顶层普通字段。");
        }

        String normalized = normalize(field);
        BotMongoProperties.RoleFieldQuery query = properties.getRoleFieldQuery();
        boolean configuredDenied = query.getDeniedFields().stream()
                .map(this::normalize)
                .anyMatch(normalized::equals);
        boolean builtInDenied = normalized.equals("id")
                || SENSITIVE_FRAGMENTS.stream().anyMatch(normalized::contains);
        if (configuredDenied || builtInDenied) {
            throw new IllegalArgumentException("该字段属于敏感信息，不允许查询。");
        }

        if (query.isWhitelistEnabled()) {
            boolean allowed = query.getAllowedFields().stream()
                    .map(this::normalize)
                    .anyMatch(normalized::equals);
            if (!allowed) {
                throw new IllegalArgumentException("该字段未加入查询白名单，不允许查询。");
            }
        }
        return field;
    }

    public int maxItems() {
        return properties.getRoleFieldQuery().getMaxItems();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
