package com.grafie.botjava;

import com.grafie.botjava.jx3.config.EnableJX3Api;
import com.grafie.botjava.jx3.config.EnableJX3ApiHttp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.grafie.botjava"})
@SpringBootApplication
@EnableJX3ApiHttp
public class BotjavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotjavaApplication.class, args);
    }

}
