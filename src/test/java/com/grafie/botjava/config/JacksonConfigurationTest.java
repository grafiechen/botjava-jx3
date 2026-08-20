package com.grafie.botjava.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class JacksonConfigurationTest {

    @Test
    void shouldApplyLenientExternalJsonRulesToSpringMapper() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfiguration().externalJsonCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();

        OptionalPayload payload = mapper.readValue(
                "{\"items\":1,\"unknown\":true}", OptionalPayload.class);

        assertEquals(List.of(1), payload.getItems());
        assertNull(payload.getName());
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES));
        assertEquals("{\"items\":[1]}", mapper.writeValueAsString(payload));
    }

    @Test
    void shouldApplySameRulesToStaticMapper() throws Exception {
        ObjectMapper mapper = ObjectMapperUtil.getObjectMapper();
        OptionalPayload payload = mapper.readValue("{\"items\":[]}", OptionalPayload.class);

        assertEquals(List.of(), payload.getItems());
        assertNull(payload.getName());
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES));
        assertEquals("{\"items\":[]}", mapper.writeValueAsString(payload));
    }

    public static class OptionalPayload {
        private String name;
        private List<Integer> items;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Integer> getItems() {
            return items;
        }

        public void setItems(List<Integer> items) {
            this.items = items;
        }
    }
}