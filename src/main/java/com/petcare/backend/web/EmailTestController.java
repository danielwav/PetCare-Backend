package com.petcare.backend.web;

import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        log.info("--- Email Connectivity Test ---");
        log.info("Host: {}, Port: {}", mailHost, mailPort);

        // 1. DNS resolution
        boolean dnsOk = false;
        String resolvedIp = "unknown";
        try {
            resolvedIp = java.net.InetAddress.getByName(mailHost).getHostAddress();
            dnsOk = true;
            log.info("DNS OK: {} -> {}", mailHost, resolvedIp);
        } catch (Exception e) {
            log.error("DNS FAILED: {}", e.getMessage());
        }

        // 2. TCP socket connection
        boolean tcpOk = false;
        String tcpError = null;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(mailHost, mailPort), 15000);
            tcpOk = true;
            log.info("TCP OK: conectado a {}:{}", mailHost, mailPort);
        } catch (Exception e) {
            tcpError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("TCP FAILED: {}", tcpError);
        }

        // 3. JavaMail Transport check
        boolean transportOk = false;
        String transportError = null;
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", mailHost);
            props.put("mail.smtp.port", String.valueOf(mailPort));
            Session session = Session.getInstance(props);
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            transportOk = true;
            log.info("TRANSPORT OK: conexión SMTP establecida");
        } catch (NoSuchProviderException e) {
            transportError = "NoSuchProvider: " + e.getMessage();
            log.error("TRANSPORT FAILED: {}", transportError);
        } catch (Exception e) {
            transportError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("TRANSPORT FAILED: {}", transportError);
        }

        Map<String, Object> result = Map.of(
                "host", mailHost,
                "port", mailPort,
                "dnsResolution", Map.of("ok", dnsOk, "ip", resolvedIp),
                "tcpConnection", Map.of("ok", tcpOk, "error", tcpError),
                "smtpTransport", Map.of("ok", transportOk, "error", transportError)
        );

        log.info("--- Resultado: {} ---", result);
        return ResponseEntity.ok(result);
    }
}
