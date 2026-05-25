package com.petcare.backend.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendActivationEmail(String to, String fullName, String token) {
        String link = baseUrl + "/auth/activate/" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail.isBlank() ? "noreply@petcare.com" : fromEmail);
        message.setTo(to);
        message.setSubject("PetCare - Activa tu cuenta");
        message.setText("""
                Hola %s,

                Se ha creado una cuenta para ti en PetCare. Para activarla, haz clic en el siguiente enlace:

                %s

                Este enlace expirará en 7 días.

                Si no esperabas este correo, ignóralo.

                Saludos,
                El equipo de PetCare
                """.formatted(fullName, link));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar correo a " + to + ": " + e.getMessage());
        }
    }
}
