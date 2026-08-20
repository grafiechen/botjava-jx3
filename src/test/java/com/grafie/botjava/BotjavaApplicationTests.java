package com.grafie.botjava;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BotjavaApplicationTests {

    @Test
    void shouldDeclareSpringBootEntryPoint() {
        assertTrue(BotjavaApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

}
