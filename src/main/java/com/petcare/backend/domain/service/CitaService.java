package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DetalleCostoCitaResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.domain.repository.HorarioVeterinarioRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.domain.repository.ServicioRepository;
import com.petcare.backend.domain.repository.VeterinarioRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.DetalleCostoCita;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.HorarioVeterinario;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.Servicio;
import com.petcare.backend.persistence.entity.Veterinario;
import com.petcare.backend.persistence.enums.EstadoCita;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaService {

	private static final int MONEY_SCALE = 2;

	private final CitaRepository citaRepository;
	private final DuenioRepository duenioRepository;
	private final MascotaRepository mascotaRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final HorarioVeterinarioRepository horarioVeterinarioRepository;
	private final ServicioRepository servicioRepository;

	@Transactional
	public CitaResponse create(CitaRequest request) {
		Duenio duenio = findDuenio(request.duenioId());
		Mascota mascota = findMascota(request.mascotaId());
		Veterinario veterinario = findVeterinario(request.veterinarioId());
		LocalTime horaFin = request.horaInicio().plusMinutes(request.duracionMinutos());

		validateBaseData(duenio, mascota, veterinario);
		validateMascotaBelongsToDuenio(mascota, duenio);
		validateSchedule(request.fecha(), request.horaInicio(), horaFin, request.duracionMinutos(), veterinario.getId(), null);

		LocalDateTime now = LocalDateTime.now();
		Cita cita = Cita.builder()
				.duenio(duenio)
				.mascota(mascota)
				.veterinario(veterinario)
				.fecha(request.fecha())
				.horaInicio(request.horaInicio())
				.horaFin(horaFin)
				.duracionMinutos(request.duracionMinutos())
				.motivo(normalizeText(request.motivo()))
				.estado(EstadoCita.PROGRAMADA)
				.subtotal(BigDecimal.ZERO)
				.descuento(BigDecimal.ZERO)
				.total(BigDecimal.ZERO)
				.requiereConfirmacion(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		replaceCostDetails(cita, request.servicios(), request.descuento());
		return toResponse(citaRepository.save(cita));
	}

	@Transactional(readOnly = true)
	public List<CitaResponse> findAll(
			EstadoCita estado,
			LocalDate fecha,
			Long duenioId,
			Long mascotaId,
			Long veterinarioId
	) {
		return citaRepository.search(estado, fecha, duenioId, mascotaId, veterinarioId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CitaResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Transactional
	public CitaResponse update(Long id, CitaRequest request) {
		Cita cita = findEntityById(id);
		if (cita.getEstado() == EstadoCita.CANCELADA) {
			throw new IllegalArgumentException("No se puede modificar una cita cancelada.");
		}

		Duenio duenio = findDuenio(request.duenioId());
		Mascota mascota = findMascota(request.mascotaId());
		Veterinario veterinario = findVeterinario(request.veterinarioId());
		LocalTime horaFin = request.horaInicio().plusMinutes(request.duracionMinutos());

		validateBaseData(duenio, mascota, veterinario);
		validateMascotaBelongsToDuenio(mascota, duenio);
		validateSchedule(request.fecha(), request.horaInicio(), horaFin, request.duracionMinutos(), veterinario.getId(), id);

		cita.setDuenio(duenio);
		cita.setMascota(mascota);
		cita.setVeterinario(veterinario);
		cita.setFecha(request.fecha());
		cita.setHoraInicio(request.horaInicio());
		cita.setHoraFin(horaFin);
		cita.setDuracionMinutos(request.duracionMinutos());
		cita.setMotivo(normalizeText(request.motivo()));
		cita.setEstado(EstadoCita.PROGRAMADA);
		cita.setRequiereConfirmacion(true);
		cita.setFechaConfirmacion(null);
		cita.setConfirmadaPor(null);
		cita.setUpdatedAt(LocalDateTime.now());
		replaceCostDetails(cita, request.servicios(), request.descuento());

		return toResponse(citaRepository.save(cita));
	}

	@Transactional
	public CitaResponse cancel(Long id) {
		Cita cita = findEntityById(id);
		if (cita.getEstado() == EstadoCita.CANCELADA) {
			return toResponse(cita);
		}

		cita.setEstado(EstadoCita.CANCELADA);
		cita.setUpdatedAt(LocalDateTime.now());
		return toResponse(citaRepository.save(cita));
	}

	@Transactional
	public CitaResponse confirm(Long id, String confirmadaPor) {
		Cita cita = findEntityById(id);
		if (cita.getEstado() == EstadoCita.CANCELADA) {
			throw new IllegalArgumentException("No se puede confirmar una cita cancelada.");
		}
		if (cita.getEstado() == EstadoCita.ATENDIDA || cita.getEstado() == EstadoCita.INASISTENCIA) {
			throw new IllegalArgumentException("No se puede confirmar una cita cerrada.");
		}
		if (!LocalDateTime.of(cita.getFecha(), cita.getHoraInicio()).isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("No se puede confirmar una cita vencida.");
		}

		cita.setEstado(EstadoCita.CONFIRMADA);
		cita.setRequiereConfirmacion(false);
		cita.setFechaConfirmacion(LocalDateTime.now());
		cita.setConfirmadaPor(normalizeText(confirmadaPor));
		cita.setUpdatedAt(LocalDateTime.now());
		return toResponse(citaRepository.save(cita));
	}

	@Transactional(readOnly = true)
	public List<CitaResponse> findConfirmationAlerts(Integer horas) {
		int hoursWindow = horas == null ? 24 : horas;
		if (hoursWindow <= 0) {
			throw new IllegalArgumentException("La ventana de alertas debe ser mayor a cero.");
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime limit = now.plusHours(hoursWindow);

		return citaRepository.findByEstadoAndRequiereConfirmacionTrueOrderByFechaAscHoraInicioAsc(EstadoCita.PROGRAMADA)
				.stream()
				.filter(cita -> {
					LocalDateTime scheduledAt = LocalDateTime.of(cita.getFecha(), cita.getHoraInicio());
					return !scheduledAt.isBefore(now) && !scheduledAt.isAfter(limit);
				})
				.map(this::toResponse)
				.toList();
	}

	private void replaceCostDetails(
			Cita cita,
			List<CostoCitaServicioRequest> servicios,
			BigDecimal descuentoRequest
	) {
		cita.getDetallesCosto().clear();

		List<DetalleCostoCita> details = servicios.stream()
				.map(request -> buildCostDetail(cita, request))
				.toList();
		BigDecimal subtotal = details.stream()
				.map(DetalleCostoCita::getSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		BigDecimal descuento = normalizeMoney(descuentoRequest == null ? BigDecimal.ZERO : descuentoRequest);

		if (descuento.compareTo(subtotal) > 0) {
			throw new IllegalArgumentException("El descuento no puede ser mayor al subtotal.");
		}

		cita.setSubtotal(subtotal);
		cita.setDescuento(descuento);
		cita.setTotal(subtotal.subtract(descuento).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
		cita.getDetallesCosto().addAll(details);
	}

	private DetalleCostoCita buildCostDetail(Cita cita, CostoCitaServicioRequest request) {
		Servicio servicio = servicioRepository.findById(request.servicioId())
				.orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado."));
		if (!servicio.getActive()) {
			throw new IllegalArgumentException("El servicio " + servicio.getNombre() + " no esta activo.");
		}

		BigDecimal subtotal = servicio.getCostoBase()
				.multiply(BigDecimal.valueOf(request.cantidad()))
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

		return DetalleCostoCita.builder()
				.cita(cita)
				.servicio(servicio)
				.nombreServicio(servicio.getNombre())
				.costoUnitario(servicio.getCostoBase())
				.cantidad(request.cantidad())
				.subtotal(subtotal)
				.descuento(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
				.total(subtotal)
				.createdAt(LocalDateTime.now())
				.build();
	}

	private void validateSchedule(
			LocalDate fecha,
			LocalTime horaInicio,
			LocalTime horaFin,
			Integer duracionMinutos,
			Long veterinarioId,
			Long currentCitaId
	) {
		if (!LocalDateTime.of(fecha, horaInicio).isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("La cita debe programarse en una fecha y hora futura.");
		}
		if (duracionMinutos <= 0) {
			throw new IllegalArgumentException("La duracion debe ser mayor a cero.");
		}

		List<HorarioVeterinario> horarios = horarioVeterinarioRepository
				.findByVeterinarioIdAndDiaSemanaAndActiveTrueOrderByHoraInicioAsc(veterinarioId, fecha.getDayOfWeek());
		boolean fitsSchedule = horarios.stream()
				.anyMatch(horario -> !horaInicio.isBefore(horario.getHoraInicio()) && !horaFin.isAfter(horario.getHoraFin()));
		if (!fitsSchedule) {
			throw new IllegalArgumentException("El veterinario no tiene disponibilidad en el horario solicitado.");
		}

		boolean hasOverlap = !citaRepository.findOverlappingAppointments(
				veterinarioId,
				fecha,
				horaInicio,
				horaFin,
				currentCitaId
		).isEmpty();
		if (hasOverlap) {
			throw new IllegalArgumentException("El veterinario ya tiene una cita programada en ese horario.");
		}
	}

	private void validateBaseData(Duenio duenio, Mascota mascota, Veterinario veterinario) {
		if (!duenio.getActive()) {
			throw new IllegalArgumentException("El duenio no esta activo.");
		}
		if (!mascota.getActive()) {
			throw new IllegalArgumentException("La mascota no esta activa.");
		}
		if (!veterinario.getActive()) {
			throw new IllegalArgumentException("El veterinario no esta activo.");
		}
	}

	private void validateMascotaBelongsToDuenio(Mascota mascota, Duenio duenio) {
		if (!mascota.getDuenio().getId().equals(duenio.getId())) {
			throw new IllegalArgumentException("La mascota no pertenece al duenio indicado.");
		}
	}

	private Cita findEntityById(Long id) {
		return citaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Cita no encontrada."));
	}

	private Duenio findDuenio(Long id) {
		return duenioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Duenio no encontrado."));
	}

	private Mascota findMascota(Long id) {
		return mascotaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada."));
	}

	private Veterinario findVeterinario(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Veterinario no encontrado."));
	}

	private CitaResponse toResponse(Cita cita) {
		return new CitaResponse(
				cita.getId(),
				cita.getDuenio().getId(),
				fullName(cita.getDuenio().getNombres(), cita.getDuenio().getApellidos()),
				cita.getMascota().getId(),
				cita.getMascota().getNombre(),
				cita.getVeterinario().getId(),
				fullName(cita.getVeterinario().getNombres(), cita.getVeterinario().getApellidos()),
				cita.getFecha(),
				cita.getHoraInicio(),
				cita.getHoraFin(),
				cita.getDuracionMinutos(),
				cita.getMotivo(),
				cita.getEstado(),
				cita.getDetallesCosto().stream().map(this::toDetailResponse).toList(),
				cita.getSubtotal(),
				cita.getDescuento(),
				cita.getTotal(),
				cita.getRequiereConfirmacion(),
				cita.getFechaConfirmacion(),
				cita.getConfirmadaPor(),
				cita.getCreatedAt(),
				cita.getUpdatedAt()
		);
	}

	private DetalleCostoCitaResponse toDetailResponse(DetalleCostoCita detail) {
		return new DetalleCostoCitaResponse(
				detail.getServicio().getId(),
				detail.getNombreServicio(),
				detail.getCostoUnitario(),
				detail.getCantidad(),
				detail.getSubtotal()
		);
	}

	private BigDecimal normalizeMoney(BigDecimal value) {
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String normalizeText(String value) {
		return value.trim();
	}

	private String fullName(String nombres, String apellidos) {
		return nombres + " " + apellidos;
	}
}
