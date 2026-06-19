package com.petcare.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ResendConfig {

    private static final Logger log = LoggerFactory.getLogger(ResendConfig.class);

    @Value("${RESEND_API_KEY:}")
    private String apiKey;

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Value("${RESEND_API_URL:https://api.resend.com/emails}")
    private String apiUrl;

    @Value("${RESEND_FROM_EMAIL:onboarding@resend.dev}")
    private String fromEmail;

    @PostConstruct
    public void validate() {
        if (apiKey.isBlank()) {
            log.error("RESEND_API_KEY no configurada. Los correos de activacion no se enviaran.");
        }
        if (frontendUrl.isBlank()) {
            log.error("FRONTEND_URL no configurada. Los enlaces de activacion seran invalidos.");
        } else {
            log.info("FRONTEND_URL configurada: {}", frontendUrl);
        }
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() {
        if (apiKey.isBlank()) throw new IllegalStateException("RESEND_API_KEY no configurada");
        return apiKey;
    }

    public String getFrontendUrl() {
        if (frontendUrl.isBlank()) throw new IllegalStateException("FRONTEND_URL no configurada");
        return frontendUrl.replaceAll("/+$", "");
    }

    public String getApiUrl() { return apiUrl; }
    public String getFromEmail() { return fromEmail; }
}
