package com.petcare.backend.web;

import com.petcare.backend.domain.service.MailgunEmailSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);
    private static final int TIMEOUT_SECONDS = 10;

    private final MailgunEmailSender mailgunSender;

    @Value("${MAILGUN_API_KEY:}")
    private String mailgunApiKey;

    @Value("${MAILGUN_DOMAIN:}")
    private String mailgunDomain;

    @Value("${MAIL_FROM:}")
    private String mailFrom;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        log.info("--- Email Connectivity Test ---");

        boolean mailgunConfigured = mailgunSender.isAvailable();
        String mailgunDomainVal = mailgunDomain;

        // Test DNS resolution for api.mailgun.net
        boolean dnsOk = false;
        String resolvedIp = "unknown";
        try {
            resolvedIp = java.net.InetAddress.getByName("api.mailgun.net").getHostAddress();
            dnsOk = true;
            log.info("DNS OK: api.mailgun.net -> {}", resolvedIp);
        } catch (Exception e) {
            log.error("DNS FAILED: {}", e.getMessage());
        }

        // Test TCP socket to api.mailgun.net:443
        boolean tcpOk = false;
        String tcpError = null;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("api.mailgun.net", 443), TIMEOUT_SECONDS * 1000);
            tcpOk = true;
            log.info("TCP OK: conectado a api.mailgun.net:443");
        } catch (Exception e) {
            tcpError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("TCP FAILED: {}", tcpError);
        }

        // Test HTTP GET to Mailgun API
        boolean httpOk = false;
        String httpError = null;
        int httpStatus = 0;
        if (mailgunConfigured) {
            try {
                String auth = Base64.getEncoder().encodeToString(("api:" + mailgunApiKey).getBytes());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mailgun.net/v3/" + mailgunDomain + "/messages"))
                        .header("Authorization", "Basic " + auth)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .GET()
                        .build();
                HttpResponse<String> response = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .build()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                httpStatus = response.statusCode();
                httpOk = true;
                log.info("HTTP OK: status {}", httpStatus);
            } catch (Exception e) {
                httpError = e.getClass().getSimpleName() + ": " + e.getMessage();
                log.error("HTTP FAILED: {}", httpError);
            }
        } else {
            httpError = "Mailgun no configurado (faltan MAILGUN_API_KEY, MAILGUN_DOMAIN y/o MAIL_FROM)";
            log.warn(httpError);
        }

        Map<String, Object> result = Map.of(
                "mailgunConfigured", mailgunConfigured,
                "mailgunDomain", mailgunDomainVal != null ? mailgunDomainVal : "no configurado",
                "mailFrom", mailFrom != null ? mailFrom : "no configurado",
                "dnsResolution", Map.of("host", "api.mailgun.net", "ok", dnsOk, "ip", resolvedIp),
                "tcpConnection", Map.of("host", "api.mailgun.net:443", "ok", tcpOk, "error", tcpError),
                "httpApi", Map.of("ok", httpOk, "status", httpStatus, "error", httpError)
        );

        log.info("--- Resultado: {} ---", result);
        return ResponseEntity.ok(result);
    }
}
