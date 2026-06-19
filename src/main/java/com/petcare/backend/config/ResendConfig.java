package com.petcare.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ResendConfig {

    @Value("${RESEND_API_KEY:}")
    private String apiKey;

    @Value("${RESEND_API_URL:https://api.resend.com/emails}")
    private String apiUrl;

    @Value("${RESEND_FROM_EMAIL:onboarding@resend.dev}")
    private String fromEmail;

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() { return apiKey; }
    public String getApiUrl() { return apiUrl; }
    public String getFromEmail() { return fromEmail; }
}
