package com.grafie.botjava.service;

import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Component
public class MongoDisplayValueFormatter {
    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LUA_TEXT = DateTimeFormatter.ofPattern("yyyy-M-d-H-m-s");
    private final ZoneId zoneId;

    public MongoDisplayValueFormatter(@Value("${bot.push.mongo-daily.zone:Asia/Tokyo}") String zone) {
        this.zoneId = ZoneId.of(zone);
    }

    public String format(Object value, ScriptStatusFieldDefinition.ValueType type) {
        if (value == null) {
            return "-";
        }
        return switch (type == null ? ScriptStatusFieldDefinition.ValueType.TEXT : type) {
            case DATETIME -> formatDateTime(value);
            case BOOLEAN -> formatBoolean(value);
            case INTEGER -> formatInteger(value);
            case DECIMAL -> formatDecimal(value);
            case TEXT -> cleanText(value);
        };
    }

    private String formatDateTime(Object value) {
        if ((value instanceof Number number && number.longValue() <= 0)
                || "0".equals(String.valueOf(value).trim())) {
            return "-";
        }
        Instant instant = toInstant(value);
        if (instant != null) {
            return OUTPUT.format(instant.atZone(zoneId));
        }
        String text = cleanText(value);
        try {
            return OUTPUT.format(LocalDateTime.parse(text, LUA_TEXT));
        } catch (DateTimeParseException ignored) {
            return text;
        }
    }

    private Instant toInstant(Object value) {
        if (value instanceof Date date) return date.toInstant();
        if (value instanceof Instant instant) return instant;
        if (value instanceof LocalDateTime dateTime) return dateTime.atZone(zoneId).toInstant();
        if (value instanceof Number number) return epoch(number.longValue());
        String text = String.valueOf(value).trim();
        if (text.matches("-?\\d{1,16}")) {
            try { return epoch(Long.parseLong(text)); } catch (NumberFormatException ignored) { return null; }
        }
        try { return Instant.parse(text); } catch (DateTimeParseException ignored) { return null; }
    }

    private Instant epoch(long value) {
        if (value <= 0) return null;
        return Math.abs(value) < 100_000_000_000L ? Instant.ofEpochSecond(value) : Instant.ofEpochMilli(value);
    }

    private String formatBoolean(Object value) {
        if (value instanceof Boolean bool) return bool ? "是" : "否";
        String text = cleanText(value);
        return switch (text.toLowerCase()) {
            case "1", "true", "是", "完成", "开启" -> "是";
            case "0", "false", "否", "未完成", "关闭" -> "否";
            default -> text;
        };
    }

    private String formatInteger(Object value) {
        if (value instanceof Number number) return String.valueOf(number.longValue());
        return cleanText(value);
    }

    private String formatDecimal(Object value) {
        if (value instanceof Number number) return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        return cleanText(value);
    }

    private String cleanText(Object value) {
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text.length() <= 120 ? text : text.substring(0, 117) + "...";
    }
}