package com.petcare.backend.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class MailgunEmailSender {

    private static final Logger log = LoggerFactory.getLogger(MailgunEmailSender.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${MAILGUN_API_KEY:}")
    private String apiKey;

    @Value("${MAILGUN_DOMAIN:}")
    private String domain;

    @Value("${MAIL_FROM:}")
    private String mailFrom;

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank()
                && domain != null && !domain.isBlank()
                && mailFrom != null && !mailFrom.isBlank();
    }

    public void send(String to, String subject, String text) {
        if (!isAvailable()) {
            log.warn("Mailgun no configurado. MAILGUN_API_KEY={}, MAILGUN_DOMAIN={}, MAIL_FROM={}",
                    mask(apiKey), domain, mailFrom);
            return;
        }

        String url = "https://api.mailgun.net/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("api", apiKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("from", mailFrom);
        body.add("to", to);
        body.add("subject", subject);
        body.add("text", text);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Enviando email vía Mailgun a {} (dominio: {})", to, domain);
            String response = restTemplate.postForObject(url, request, String.class);
            log.info("Mailgun response: {}", response);
        } catch (Exception e) {
            log.error("Error Mailgun al enviar a {}: {} — {}", to, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String mask(String value) {
        return value != null && value.length() > 8
                ? value.substring(0, 4) + "****"
                : value;
    }
}
