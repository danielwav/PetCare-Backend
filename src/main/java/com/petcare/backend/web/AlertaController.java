package com.petcare.backend.web;

import com.petcare.backend.domain.dto.response.PanelAlertasDiaResponse;
import com.petcare.backend.domain.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlertaController {

    private static final Logger log = LoggerFactory.getLogger(AlertaController.class);

    private final AlertaService alertaService;

    @GetMapping("/api/alertas/dia")
    public ResponseEntity<PanelAlertasDiaResponse> getDailyPanel() {
        log.info("Solicitando alertas del día");
        try {
            PanelAlertasDiaResponse panel = alertaService.getDailyPanel(null, null);
            log.info("Alertas generadas: {} citas, {} vacunas, {} controles pendientes",
                    panel.totalCitasProgramadasHoy(),
                    panel.totalVacunasProximas(),
                    panel.totalControlesMensualesPendientes());
            return ResponseEntity.ok(panel);
        } catch (Exception e) {
            log.error("Error al generar alertas del día: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
