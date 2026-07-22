package com.monagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonagentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonagentApplication.class, args);
    }
}
