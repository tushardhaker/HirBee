package com.freelance.projectmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class ProjectManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectManagerApplication.class, args);
    }

    /**
     * Provides RestTemplate bean for making HTTP calls to Brevo API
     * (used in EmailService for sending emails via REST instead of SMTP)
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}