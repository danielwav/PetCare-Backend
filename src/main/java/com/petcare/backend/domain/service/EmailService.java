package com.petcare.backend.domain.service;

import com.petcare.backend.config.ResendConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestTemplate restTemplate;
    private final ResendConfig resendConfig;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public void sendActivationEmail(String to, String fullName, String token) {
        if (!mailEnabled) {
            log.warn("Correo no enviado a {}: mail.enabled=false. Token: {}", to, token);
            return;
        }

        String link = baseUrl + "/auth/activate/" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif">
            <table style="max-width:600px;margin:20px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)">
              <tr><td style="background:#4FC3F7;padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:24px">🐾 PetCare</h1>
                <p style="color:#e3f2fd;margin:4px 0 0">Sistema de Gesti\u00f3n Veterinaria</p>
              </td></tr>
              <tr><td style="padding:32px">
                <h2 style="color:#333;margin:0 0 8px">\u00a1Hola, %s!</h2>
                <p style="color:#666;line-height:1.6">Se ha creado una cuenta para ti en <strong>PetCare</strong> con el rol asignado por tu administrador.</p>
                <p style="color:#666;line-height:1.6">Para activarla y crear tu contrase\u00f1a, haz clic en el bot\u00f3n:</p>
                <div style="text-align:center;margin:28px 0">
                  <a href="%s" style="background:#4FC3F7;color:#fff;padding:14px 36px;border-radius:12px;text-decoration:none;font-size:16px;font-weight:bold;display:inline-block">Activar Cuenta</a>
                </div>
                <p style="color:#999;font-size:13px">Si el bot\u00f3n no funciona, copia este enlace en tu navegador:</p>
                <p style="color:#999;font-size:12px;word-break:break-all">%s</p>
                <p style="color:#999;font-size:13px;margin-top:20px">Este enlace expirar\u00e1 en <strong>7 d\u00edas</strong>.</p>
                <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                <p style="color:#aaa;font-size:12px;text-align:center">Si no esperabas este correo, ign\u00f3ralo.<br>\u00a9 2026 PetCare</p>
              </td></tr>
            </table>
            </body>
            </html>
            """.formatted(fullName, link, link);

        try {
            String from = resendConfig.getFromEmail();
            var body = Map.of(
                "from", from,
                "to", java.util.List.of(to),
                "subject", "PetCare - Activa tu cuenta",
                "html", html
            );

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendConfig.getApiKey());

            var request = new HttpEntity<>(body, headers);
            var response = restTemplate.postForEntity(resendConfig.getApiUrl(), request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo de activación enviado a {} vía Resend", to);
            } else {
                log.error("Resend respondió con {}: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Error al enviar correo vía Resend");
            }
        } catch (Exception e) {
            log.error("Error al enviar correo de activación a {}: {}", to, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo de activación a " + to, e);
        }
    }
}
