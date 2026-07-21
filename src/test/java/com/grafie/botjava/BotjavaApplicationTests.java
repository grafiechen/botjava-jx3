package com.grafie.botjava;

import com.grafie.botjava.jx3.config.EnableJX3ApiHttp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BotjavaApplicationTests {

    @Test
    void shouldDeclareSpringBootEntryPoint() {
        assertTrue(BotjavaApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(BotjavaApplication.class.isAnnotationPresent(EnableJX3ApiHttp.class));
    }

}
