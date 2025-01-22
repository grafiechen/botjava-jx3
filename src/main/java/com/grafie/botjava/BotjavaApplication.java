package com.grafie.botjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.grafie.botjava")
@SpringBootApplication
public class BotjavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotjavaApplication.class, args);
    }

}
