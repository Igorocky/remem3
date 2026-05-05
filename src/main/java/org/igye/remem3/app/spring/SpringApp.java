package org.igye.remem3.app.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringApp {

    static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(SpringApp.class, args);
        ctx.start();
    }
}