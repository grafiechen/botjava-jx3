package com.grafie.botjava.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:03
 */
public class ObjectMapperUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 返回统一的objectMapper，用于无法自动注入的地方
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static String writeValueAsString(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }

    public static <T> T readValue(String value, Class<T> c) throws JsonProcessingException {
        return objectMapper.readValue(value, c);
    }
}
