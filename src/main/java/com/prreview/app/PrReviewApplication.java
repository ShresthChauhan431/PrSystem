package com.prreview.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
public class PrReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrReviewApplication.class, args);
    }
}
