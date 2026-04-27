package com.converse.disposition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DispositionEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(DispositionEngineApplication.class, args);
    }
}
