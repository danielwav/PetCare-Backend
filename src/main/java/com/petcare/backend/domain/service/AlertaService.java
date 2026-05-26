package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.response.AlertaCitaResponse;
import com.petcare.backend.domain.dto.response.AlertaVacunaResponse;
import com.petcare.backend.domain.dto.response.ControlMensualPendienteResponse;
import com.petcare.backend.domain.dto.response.PanelAlertasDiaResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.domain.repository.VacunaMascotaRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.VacunaMascota;
import com.petcare.backend.persistence.enums.EstadoCita;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaService {

	private static final int DEFAULT_VACCINE_ALERT_DAYS = 30;

	private final CitaRepository citaRepository;
	private final VacunaMascotaRepository vacunaMascotaRepository;
	private final MascotaRepository mascotaRepository;

	@Transactional(readOnly = true)
	public PanelAlertasDiaResponse getDailyPanel(LocalDate fecha, Integer diasVacunas) {
		LocalDate panelDate = fecha == null ? LocalDate.now() : fecha;
		int vaccineAlertDays = diasVacunas == null ? DEFAULT_VACCINE_ALERT_DAYS : diasVacunas;
		if (vaccineAlertDays <= 0) {
			throw new IllegalArgumentException("La ventana de alertas de vacunas debe ser mayor a cero.");
		}

		List<Cita> citasDelDia = citaRepository.search(null, panelDate, null, null, null);
		List<AlertaCitaResponse> citasProgramadasHoy = citasDelDia.stream()
				.filter(cita -> cita.getEstado() == EstadoCita.PROGRAMADA || cita.getEstado() == EstadoCita.CONFIRMADA)
				.map(this::toCitaAlert)
				.toList();
		List<AlertaCitaResponse> citasSinConfirmar = citasDelDia.stream()
				.filter(cita -> cita.getEstado() == EstadoCita.PROGRAMADA)
				.filter(cita -> Boolean.TRUE.equals(cita.getRequiereConfirmacion()))
				.map(this::toCitaAlert)
				.toList();
		List<AlertaCitaResponse> citasConfirmadasPendientesAtencion = citasDelDia.stream()
				.filter(cita -> cita.getEstado() == EstadoCita.CONFIRMADA)
				.map(this::toCitaAlert)
				.toList();
		List<AlertaCitaResponse> citasNoAsistidasHoy = citasDelDia.stream()
				.filter(cita -> cita.getEstado() == EstadoCita.NO_ASISTIO)
				.map(this::toCitaAlert)
				.toList();

		LocalDate today = LocalDate.now();
		List<VacunaMascota> vaccineAlerts = vacunaMascotaRepository
				.findByFechaProximaDosisLessThanEqualOrderByFechaProximaDosisAsc(today.plusDays(vaccineAlertDays));
		List<AlertaVacunaResponse> vacunasProximas = vaccineAlerts.stream()
				.filter(vacunaMascota -> vacunaMascota.getFechaProximaDosis() != null)
				.filter(vacunaMascota -> !vacunaMascota.getFechaProximaDosis().isBefore(today))
				.map(this::toVacunaAlert)
				.toList();
		List<AlertaVacunaResponse> vacunasVencidas = vaccineAlerts.stream()
				.filter(vacunaMascota -> vacunaMascota.getFechaProximaDosis() != null)
				.filter(vacunaMascota -> vacunaMascota.getFechaProximaDosis().isBefore(today))
				.map(this::toVacunaAlert)
				.toList();

		List<ControlMensualPendienteResponse> controlesPendientes = mascotaRepository
				.findActivePetsWithoutMonthlyControl(panelDate.getYear(), panelDate.getMonthValue())
				.stream()
				.map(mascota -> toControlPending(mascota, panelDate))
				.toList();

		return new PanelAlertasDiaResponse(
				panelDate,
				citasProgramadasHoy.size(),
				citasSinConfirmar.size(),
				citasConfirmadasPendientesAtencion.size(),
				citasNoAsistidasHoy.size(),
				vacunasProximas.size(),
				vacunasVencidas.size(),
				controlesPendientes.size(),
				citasProgramadasHoy,
				citasSinConfirmar,
				citasConfirmadasPendientesAtencion,
				citasNoAsistidasHoy,
				vacunasProximas,
				vacunasVencidas,
				controlesPendientes
		);
	}

	private AlertaCitaResponse toCitaAlert(Cita cita) {
		return new AlertaCitaResponse(
				cita.getId(),
				cita.getDuenio().getId(),
				fullName(cita.getDuenio().getNombres(), cita.getDuenio().getApellidos()),
				cita.getMascota().getId(),
				cita.getMascota().getNombre(),
				cita.getVeterinario().getId(),
				fullName(cita.getVeterinario().getNombres(), cita.getVeterinario().getApellidos()),
				cita.getFecha(),
				cita.getHoraInicio(),
				cita.getEstado(),
				cita.getMotivo()
		);
	}

	private AlertaVacunaResponse toVacunaAlert(VacunaMascota vacunaMascota) {
		return new AlertaVacunaResponse(
				vacunaMascota.getId(),
				vacunaMascota.getMascota().getId(),
				vacunaMascota.getMascota().getNombre(),
				vacunaMascota.getVacuna().getId(),
				vacunaMascota.getVacuna().getNombre(),
				vacunaMascota.getFechaProximaDosis(),
				resolveVaccineStatus(vacunaMascota.getFechaProximaDosis())
		);
	}

	private ControlMensualPendienteResponse toControlPending(Mascota mascota, LocalDate panelDate) {
		return new ControlMensualPendienteResponse(
				mascota.getId(),
				mascota.getNombre(),
				mascota.getDuenio().getId(),
				fullName(mascota.getDuenio().getNombres(), mascota.getDuenio().getApellidos()),
				panelDate.getYear(),
				panelDate.getMonthValue()
		);
	}

	private String resolveVaccineStatus(LocalDate nextDoseDate) {
		if (nextDoseDate.isBefore(LocalDate.now())) {
			return "VENCIDA";
		}
		return "PROXIMA";
	}

	private String fullName(String nombres, String apellidos) {
		return nombres + " " + apellidos;
	}
}
