package com.petcare.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionJwtConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtConfiguration.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    @Import(JwtService.class)
    static class JwtConfiguration {}

    @Test
    void productionRejectsAnEmptySigningSecretInsteadOfUsingDevelopmentFallback() {
        context.withPropertyValues("JWT_SECRET=").run(result -> assertThat(result).hasFailed());
    }

    @Test
    void productionUsesExplicitSigningSecret() {
        String secret = java.util.Base64.getEncoder().encodeToString(new byte[64]);
        context.withPropertyValues("JWT_SECRET=" + secret).run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result.getBean(JwtProperties.class).secret()).isEqualTo(secret);
        });
    }
}
