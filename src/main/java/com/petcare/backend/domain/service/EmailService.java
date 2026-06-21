package com.petcare.backend.domain.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Async
    public void sendActivationEmail(String to, String fullName, String token) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("No se puede enviar el correo: FRONTEND_URL no configurada");
            return;
        }

        String link = frontendUrl.replaceAll("/+$", "") + "/activate-account?token=" + token;
        String text = buildText(fullName, link);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("PetCare - Activa tu cuenta");
            message.setText(text);
            mailSender.send(message);
            log.info("Correo de activación enviado a {}", to);
        } catch (Exception e) {
            log.error("Error al enviar correo de activación a {}: {}", to, e.getMessage());
        }
    }

    private String buildText(String fullName, String link) {
        return """
Hola %s,

Se ha creado una cuenta para ti en PetCare.

Para activarla y crear tu contraseña, haz clic en el siguiente enlace:

%s

Este enlace expirará en 7 días.

Si no esperabas este correo, ignóralo.

© 2026 PetCare - Sistema de Gestión Veterinaria
""".formatted(fullName, link);
    }
}
