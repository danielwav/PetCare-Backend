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

    @Value("${MAIL_FROM:}")
    private String mailFrom;

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Async
    public void sendActivationEmail(String to, String fullName, String token) {
        if (mailFrom == null || mailFrom.isBlank()) {
            log.error("No se puede enviar el correo: MAIL_FROM no configurado");
            return;
        }
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("No se puede enviar el correo: FRONTEND_URL no configurada");
            return;
        }
        String link = frontendUrl.replaceAll("/+$", "") + "/activate-account?token=" + token;
        String text = buildActivationText(fullName, link);
        log.info("Enviando correo de activación a {} desde {}", to, mailFrom);
        send(to, "PetCare - Activa tu cuenta", text);
    }

    @Async
    public void sendPasswordRecoveryEmail(String to, String fullName, String token) {
        if (mailFrom == null || mailFrom.isBlank()) {
            log.error("No se puede enviar el correo: MAIL_FROM no configurado");
            return;
        }
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("No se puede enviar el correo: FRONTEND_URL no configurada");
            return;
        }
        String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        String text = buildRecoveryText(fullName, link);
        log.info("Enviando correo de recuperación a {} desde {}", to, mailFrom);
        send(to, "PetCare - Recuperación de contraseña", text);
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Correo enviado a {} — asunto: {}", to, subject);
        } catch (Exception e) {
            log.error("Error SMTP al enviar a {}: {} — {}", to, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String buildActivationText(String fullName, String link) {
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

    private String buildRecoveryText(String fullName, String link) {
        return """
Hola %s,

Recibimos una solicitud para restablecer tu contraseña en PetCare.

Haz clic en el siguiente enlace para crear una nueva contraseña:

%s

Este enlace expirará en 1 hora.

Si no solicitaste este cambio, ignora este correo.

© 2026 PetCare - Sistema de Gestión Veterinaria
""".formatted(fullName, link);
    }
}
