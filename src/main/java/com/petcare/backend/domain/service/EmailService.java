package com.petcare.backend.domain.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public void sendActivationEmail(String to, String fullName, String token) {
        if (!mailEnabled) {
            log.warn("Correo no enviado a {}: mail.enabled=false. Token: {}", to, token);
            return;
        }

        String link = baseUrl + "/auth/activate/" + token;

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail.isBlank() ? "noreply@petcare.com" : fromEmail);
            helper.setTo(to);
            helper.setSubject("PetCare - Activa tu cuenta");

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif">
                <table style="max-width:600px;margin:20px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)">
                  <tr><td style="background:#4FC3F7;padding:32px;text-align:center">
                    <h1 style="color:#fff;margin:0;font-size:24px">🐾 PetCare</h1>
                    <p style="color:#e3f2fd;margin:4px 0 0">Sistema de Gestión Veterinaria</p>
                  </td></tr>
                  <tr><td style="padding:32px">
                    <h2 style="color:#333;margin:0 0 8px">¡Hola, %s!</h2>
                    <p style="color:#666;line-height:1.6">Se ha creado una cuenta para ti en <strong>PetCare</strong> con el rol asignado por tu administrador.</p>
                    <p style="color:#666;line-height:1.6">Para activarla y crear tu contraseña, haz clic en el botón:</p>
                    <div style="text-align:center;margin:28px 0">
                      <a href="%s" style="background:#4FC3F7;color:#fff;padding:14px 36px;border-radius:12px;text-decoration:none;font-size:16px;font-weight:bold;display:inline-block">Activar Cuenta</a>
                    </div>
                    <p style="color:#999;font-size:13px">Si el botón no funciona, copia este enlace en tu navegador:</p>
                    <p style="color:#999;font-size:12px;word-break:break-all">%s</p>
                    <p style="color:#999;font-size:13px;margin-top:20px">Este enlace expirará en <strong>7 días</strong>.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                    <p style="color:#aaa;font-size:12px;text-align:center">Si no esperabas este correo, ignóralo.<br>© 2026 PetCare</p>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(fullName, link, link);

            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Correo de activación enviado a {}", to);

        } catch (Exception e) {
            log.error("Error al enviar correo de activación a {}: {}", to, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo de activación a " + to, e);
        }
    }
}
