package com.grafie.botjava;

import com.grafie.botjava.jx3.config.EnableJX3Api;
import com.grafie.botjava.jx3.config.EnableJX3ApiHttp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages = {"com.grafie.botjava"})
@SpringBootApplication
@EnableJX3ApiHttp
@EnableScheduling
public class BotjavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotjavaApplication.class, args);
    }

}
