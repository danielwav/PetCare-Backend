package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.NotificacionResponse;
import com.petcare.backend.domain.service.CitaService;
import com.petcare.backend.domain.service.VacunaService;
import com.petcare.backend.persistence.enums.EstadoCita;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequiredArgsConstructor
public class NotificacionController {

    private final CitaService citaService;
    private final VacunaService vacunaService;

    @GetMapping("/api/notificaciones")
    public List<NotificacionResponse> getNotificaciones(Authentication authentication) {
        List<NotificacionResponse> notificaciones = new ArrayList<>();
        AtomicLong idGen = new AtomicLong(1);
        LocalDate today = LocalDate.now();
        String hoy = today.toString();
        String manana = today.plusDays(1).toString();

        if (authentication == null) return notificaciones;

        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        boolean isAsistente = hasRole(authentication, "ROLE_ASISTENTE");
        boolean isVet = hasRole(authentication, "ROLE_VETERINARIO");
        boolean isDuenio = hasRole(authentication, "ROLE_DUENIO");

        try {
            // Citas sin confirmar (ADMIN, ASISTENTE — módulo Alertas/Citas)
            if (isAdmin || isAsistente) {
                var citasSinConfirmar = citaService.findAll(EstadoCita.PROGRAMADA, null, null, null, null).stream()
                    .filter(c -> Boolean.TRUE.equals(c.requiereConfirmacion()) && !c.fecha().isBefore(today))
                    .toList();
                for (var c : citasSinConfirmar) {
                    notificaciones.add(new NotificacionResponse(
                        idGen.getAndIncrement(), "CITA_CONFIRMAR",
                        c.duenioNombreCompleto() + " — " + c.mascotaNombre() + " (" + c.fecha() + ")",
                        c.fecha().toString(), "/citas/" + c.id(), "notification_important", false
                    ));
                }
            }

            // Próximas citas (VET, ASISTENTE, DUENIO)
            if (isVet || isAsistente || isDuenio) {
                var citasHoy = citaService.findAll(EstadoCita.PROGRAMADA, today, null, null, null);
                for (var c : citasHoy) {
                    notificaciones.add(new NotificacionResponse(
                        idGen.getAndIncrement(), "CITA_PROXIMA",
                        c.mascotaNombre() + " — " + c.motivo() + " (" + c.horaInicio() + ")",
                        hoy, "/citas", "calendar_month", false
                    ));
                }
            }

            // Atención pendiente (VET, ASISTENTE — módulo Atención Clínica)
            if (isVet || isAsistente) {
                var citasConfirmadas = citaService.findAll(EstadoCita.CONFIRMADA, today, null, null, null);
                for (var c : citasConfirmadas) {
                    notificaciones.add(new NotificacionResponse(
                        idGen.getAndIncrement(), "ATENCION_PENDIENTE",
                        c.mascotaNombre() + " — " + c.motivo() + " (en espera)",
                        hoy, "/atencion-clinica?citaId=" + c.id(), "monitor_heart", false
                    ));
                }
            }

            // Recordatorio (solo DUENIO)
            if (isDuenio) {
                var citasManana = citaService.findAll(null, today.plusDays(1), null, null, null);
                for (var c : citasManana) {
                    notificaciones.add(new NotificacionResponse(
                        idGen.getAndIncrement(), "RECORDATORIO",
                        "Recordatorio: " + c.mascotaNombre() + " — " + c.motivo() + " mañana a las " + c.horaInicio(),
                        manana, "/citas", "alarm", false
                    ));
                }
            }
        } catch (Exception ignored) {}

        // Vacunas próximas (VET, ASISTENTE, DUENIO — módulo Vacunas)
        if (isVet || isAsistente || isDuenio) {
            try {
                var vacunas = vacunaService.findAlerts(30);
                for (var v : vacunas) {
                    notificaciones.add(new NotificacionResponse(
                        idGen.getAndIncrement(), "VACUNA",
                        v.vacunaNombre() + " — " + v.mascotaNombre(),
                        v.fechaProximaDosis() != null ? v.fechaProximaDosis().toString() : "",
                        "/vacunas", "vaccines", false
                    ));
                }
            } catch (Exception ignored) {}
        }
        notificaciones.sort(Comparator.comparing(NotificacionResponse::fecha).reversed());
        return notificaciones.size() > 20 ? notificaciones.subList(0, 20) : notificaciones;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
