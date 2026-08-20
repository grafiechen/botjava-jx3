package com.grafie.botjava.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:03
 */
public final class ObjectMapperUtil {
    private static final ObjectMapper objectMapper = configure(new ObjectMapper());

    private ObjectMapperUtil() {
    }

    /**
     * 统一外部 JSON 的宽松读写策略。字段缺失、null、未知字段和空对象均不应导致业务失败。
     */
    public static ObjectMapper configure(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES
        );
        mapper.enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY
        );
        mapper.disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS,
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );
        return mapper;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    public static <T> T readValue(String value, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(value, type);
    }

    /**
     * 用于转换外部来的数据，接口参数无法使用 String 当作 payload 中的 d 值。
     */
    public static <T> T readValue(Object value, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(writeValueAsString(value), type);
    }
}