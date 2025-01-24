package com.grafie.botjava.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:03
 */
public class ObjectMapperUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());

        // 解决序列化问题
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    }

    /**
     * -- GETTER --
     * 返回统一的objectMapper，用于无法自动注入的地方
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper(){
        return objectMapper;
    }

    public static String writeValueAsString(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }

    public static <T> T readValue(String value, Class<T> c) throws JsonProcessingException {
        return objectMapper.readValue(value, c);
    }

    /**
     * 用于转换外部来的数据，接口参数无法使用String 当作payload中的d值
     *
     * @param o payload中的原始D值
     * @param c 需要转换出来的实体类class
     * @return c
     * @throws JsonProcessingException
     */
    public static <T> T readValue(Object o, Class<T> c) throws JsonProcessingException {
        return objectMapper.readValue(writeValueAsString(o), c);
    }

}
