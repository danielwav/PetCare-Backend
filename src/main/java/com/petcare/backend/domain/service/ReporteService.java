package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.response.DetalleCostoCitaResponse;
import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
import com.petcare.backend.domain.dto.response.InasistenciaResponse;
import com.petcare.backend.domain.dto.response.ReporteCitaResponse;
import com.petcare.backend.domain.dto.response.ReporteCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ReporteServicioResponse;
import com.petcare.backend.domain.dto.response.ServicioSolicitadoResponse;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.DetalleCostoCitaRepository;
import com.petcare.backend.domain.repository.InasistenciaRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.domain.repository.ServicioRepository;
import com.petcare.backend.domain.repository.VacunaMascotaRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.DetalleCostoCita;
import com.petcare.backend.persistence.entity.Inasistencia;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.Servicio;
import com.petcare.backend.persistence.entity.VacunaMascota;
import com.petcare.backend.persistence.entity.Veterinario;
import com.petcare.backend.persistence.enums.EstadoCita;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

	private static final int DEFAULT_UPCOMING_VACCINE_DAYS = 30;

	private final CitaRepository citaRepository;
	private final DetalleCostoCitaRepository detalleCostoCitaRepository;
	private final InasistenciaRepository inasistenciaRepository;
	private final VacunaMascotaRepository vacunaMascotaRepository;
	private final ServicioRepository servicioRepository;
	private final MascotaRepository mascotaRepository;
	private final AtencionClinicaService atencionClinicaService;

	@Transactional(readOnly = true)
	public List<ReporteCitaResponse> findCitas(
			EstadoCita estado,
			LocalDate fechaInicio,
			LocalDate fechaFin,
			Long veterinarioId,
			Long mascotaId,
			Long duenioId,
			Long clinicaId
	) {
		validateDateRange(fechaInicio, fechaFin);
		return citaRepository.searchByDateRange(estado, fechaInicio, fechaFin, clinicaId, duenioId, mascotaId, veterinarioId)
				.stream()
				.map(this::toCitaReport)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<InasistenciaResponse> findInasistencias(Long duenioId, LocalDate fechaInicio, LocalDate fechaFin, Long clinicaId) {
		validateDateRange(fechaInicio, fechaFin);
		LocalDateTime start = fechaInicio == null ? null : fechaInicio.atStartOfDay();
		LocalDateTime end = fechaFin == null ? null : fechaFin.atTime(LocalTime.MAX);

		return inasistenciaRepository.search(clinicaId, duenioId, start, end).stream()
				.map(this::toInasistenciaResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<VacunaMascotaResponse> findVacunasProximas(LocalDate fechaInicio, LocalDate fechaFin, Long clinicaId) {
		LocalDate start = fechaInicio == null ? LocalDate.now() : fechaInicio;
		LocalDate end = fechaFin == null ? start.plusDays(DEFAULT_UPCOMING_VACCINE_DAYS) : fechaFin;
		validateDateRange(start, end);

		return vacunaMascotaRepository.findByMascotaClinicaIdAndFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(clinicaId, start, end).stream()
				.map(this::toVacunaMascotaResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ReporteCostoCitaResponse findCostoCita(Long citaId, Long clinicaId) {
		Cita cita = citaRepository.findById(citaId)
				.orElseThrow(() -> new EntityNotFoundException("Cita no encontrada."));
		if (cita.getClinica() == null || !cita.getClinica().getId().equals(clinicaId)) {
			throw new AccessDeniedException("No tienes permiso para acceder a esta cita.");
		}
		List<DetalleCostoCitaResponse> detalles = cita.getDetallesCosto().stream()
				.map(this::toDetalleCostoResponse)
				.toList();

		return new ReporteCostoCitaResponse(
				cita.getId(),
				cita.getSubtotal(),
				cita.getDescuento(),
				cita.getTotal(),
				detalles
		);
	}

	@Transactional(readOnly = true)
	public List<ServicioSolicitadoResponse> findServiciosMasSolicitados(LocalDate fechaInicio, LocalDate fechaFin, Long clinicaId) {
		validateDateRange(fechaInicio, fechaFin);
		return detalleCostoCitaRepository.findMostRequestedServices(clinicaId, fechaInicio, fechaFin).stream()
				.map(row -> new ServicioSolicitadoResponse(
						(String) row[0],
						((Number) row[1]).longValue(),
						(BigDecimal) row[2]
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public ReporteServicioResponse findReporteServicios(Long clinicaId) {
		List<Servicio> todos = servicioRepository.findAllByClinicaIdOrderByNombreAsc(clinicaId);
		long activos = todos.stream().filter(Servicio::getActive).count();
		long inactivos = todos.size() - activos;

		LocalDate now = LocalDate.now();
		LocalDate inicioMes = now.withDayOfMonth(1);
		LocalDate inicioAnio = now.withDayOfMonth(1).withMonth(1);

		List<Object[]> detalleMes = detalleCostoCitaRepository.findMostRequestedServices(clinicaId, inicioMes, now);
		List<Object[]> detalleAnio = detalleCostoCitaRepository.findMostRequestedServices(clinicaId, inicioAnio, now);

		BigDecimal ingresosMes = detalleMes.stream().map(r -> (BigDecimal) r[2]).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal ingresosAnioValue = detalleAnio.stream().map(r -> (BigDecimal) r[2]).reduce(BigDecimal.ZERO, BigDecimal::add);

		String servicioMasSolicitado = "N/A";

		Map<String, Long> categorias = detectarCategorias(todos);
		String catMasUsada = categorias.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");

		List<ReporteServicioResponse.CategoriaCount> catList = categorias.entrySet().stream()
				.map(e -> new ReporteServicioResponse.CategoriaCount(e.getKey(), e.getValue().intValue()))
				.toList();

		List<ServicioSolicitadoResponse> top = findServiciosMasSolicitados(inicioAnio, now, clinicaId);
		if (!top.isEmpty()) servicioMasSolicitado = top.getFirst().nombreServicio();

		List<ReporteServicioResponse.IngresoPorServicio> ingresos = top.stream()
				.map(s -> {
					Servicio serv = servicioRepository.findByClinicaIdAndNombreIgnoreCase(clinicaId, s.nombreServicio()).orElse(null);
					return new ReporteServicioResponse.IngresoPorServicio(
							s.nombreServicio(), serv != null ? serv.getCostoBase() : BigDecimal.ZERO,
							s.cantidadSolicitada(), s.totalGenerado()
					);
				})
				.toList();

		List<ReporteServicioResponse.TendenciaMensual> tendencia = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			LocalDate start = inicioAnio.plusMonths(i);
			LocalDate end = start.plusMonths(1).minusDays(1);
			LocalDate fin = end.isAfter(now) ? now : end;
			List<Object[]> datos = detalleCostoCitaRepository.findMostRequestedServices(clinicaId, start, fin);
			long cant = datos.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
			String mes = start.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es"));
			tendencia.add(new ReporteServicioResponse.TendenciaMensual(mes, start.getYear(), cant));
		}

		return new ReporteServicioResponse(
				todos.size(), (int) activos, (int) inactivos,
				catMasUsada, servicioMasSolicitado,
				ingresosMes, ingresosAnioValue,
				catList, top, ingresos, tendencia
		);
	}

	@Transactional(readOnly = true)
	public HistoriaClinicaResponse findHistoriaClinica(Long mascotaId, Long clinicaId) {
		Mascota mascota = mascotaRepository.findById(mascotaId)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
		if (mascota.getClinica() == null || !mascota.getClinica().getId().equals(clinicaId)) {
			throw new AccessDeniedException("No tienes permiso para acceder a esta mascota.");
		}
		return atencionClinicaService.findHistoriaClinicaByMascota(mascotaId);
	}

	private Map<String, Long> detectarCategorias(List<Servicio> servicios) {
		Map<String, Long> map = new HashMap<>();
		for (var s : servicios) {
			var cat = detectCategory(s.getNombre());
			map.merge(cat, 1L, Long::sum);
		}
		return map;
	}

	private String detectCategory(String nombre) {
		String n = nombre.toLowerCase();
		if (n.contains("consulta")) return "Consulta";
		if (n.contains("vacu") || n.contains("vacuna")) return "Vacunación";
		if (n.contains("cirug") || n.contains("ciruj") || n.contains("esterilizacion")) return "Cirugía";
		if (n.contains("labor") || n.contains("analisis") || n.contains("examen")) return "Laboratorio";
		if (n.contains("estét") || n.contains("pelu") || n.contains("belleza")) return "Estética";
		return "Consulta";
	}

	private void validateDateRange(LocalDate start, LocalDate end) {
		if (start != null && end != null && end.isBefore(start)) {
			throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
		}
	}

	private ReporteCitaResponse toCitaReport(Cita cita) {
		Veterinario veterinario = cita.getVeterinario();
		return new ReporteCitaResponse(
				cita.getId(),
				cita.getFecha(),
				cita.getHoraInicio(),
				cita.getEstado(),
				cita.getDuenio().getId(),
				fullName(cita.getDuenio().getNombres(), cita.getDuenio().getApellidos()),
				cita.getMascota().getId(),
				cita.getMascota().getNombre(),
				veterinario.getId(),
				fullName(veterinario.getNombres(), veterinario.getApellidos()),
				cita.getMotivo(),
				cita.getTotal()
		);
	}

	private InasistenciaResponse toInasistenciaResponse(Inasistencia inasistencia) {
		Cita cita = inasistencia.getCita();
		return new InasistenciaResponse(
				inasistencia.getId(),
				cita.getId(),
				inasistencia.getDuenio().getId(),
				fullName(inasistencia.getDuenio().getNombres(), inasistencia.getDuenio().getApellidos()),
				inasistencia.getMascota().getId(),
				inasistencia.getMascota().getNombre(),
				cita.getFecha(),
				cita.getHoraInicio(),
				inasistencia.getObservacion(),
				inasistencia.getRegistradoPor(),
				inasistencia.getFechaRegistro()
		);
	}

	private VacunaMascotaResponse toVacunaMascotaResponse(VacunaMascota vacunaMascota) {
		Veterinario veterinario = vacunaMascota.getVeterinario();
		Cita cita = vacunaMascota.getCita();
		return new VacunaMascotaResponse(
				vacunaMascota.getId(),
				vacunaMascota.getMascota().getId(),
				vacunaMascota.getMascota().getNombre(),
				vacunaMascota.getVacuna().getId(),
				vacunaMascota.getVacuna().getNombre(),
				veterinario.getId(),
				fullName(veterinario.getNombres(), veterinario.getApellidos()),
				cita == null ? null : cita.getId(),
				vacunaMascota.getFechaAplicacion(),
				vacunaMascota.getLote(),
				vacunaMascota.getFechaProximaDosis(),
				vacunaMascota.getObservaciones(),
				resolveVaccineStatus(vacunaMascota.getFechaProximaDosis()),
				vacunaMascota.getCreatedAt()
		);
	}

	private DetalleCostoCitaResponse toDetalleCostoResponse(DetalleCostoCita detail) {
		return new DetalleCostoCitaResponse(
				detail.getServicio().getId(),
				detail.getNombreServicio(),
				detail.getCostoUnitario(),
				detail.getCantidad(),
				detail.getSubtotal()
		);
	}

	private String resolveVaccineStatus(LocalDate nextDoseDate) {
		if (nextDoseDate == null) {
			return "SIN_PROXIMA_DOSIS";
		}
		if (nextDoseDate.isBefore(LocalDate.now())) {
			return "VENCIDA";
		}
		if (!nextDoseDate.isAfter(LocalDate.now().plusDays(DEFAULT_UPCOMING_VACCINE_DAYS))) {
			return "PROXIMA";
		}
		return "PROGRAMADA";
	}

	private String fullName(String nombres, String apellidos) {
		return nombres + " " + apellidos;
	}
}
