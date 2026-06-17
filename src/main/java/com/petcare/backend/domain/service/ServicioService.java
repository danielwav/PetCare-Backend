package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CalculoCostoCitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.response.CalculoCostoCitaResponse;
import com.petcare.backend.domain.dto.response.DetalleCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.repository.ServicioRepository;
import com.petcare.backend.persistence.entity.Servicio;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioService {

	private static final int MONEY_SCALE = 2;

	private final ServicioRepository servicioRepository;

	@Transactional
	public ServicioResponse create(ServicioRequest request) {
		validateUniqueName(request.nombre(), null);

		LocalDateTime now = LocalDateTime.now();
		Servicio servicio = Servicio.builder()
				.nombre(normalizeText(request.nombre()))
				.descripcion(normalizeText(request.descripcion()))
				.costoBase(normalizeMoney(request.costoBase()))
				.active(true)
				.createdAt(now)
				.updatedAt(now)
				.build();

		return toResponse(servicioRepository.save(servicio));
	}

	@Transactional(readOnly = true)
	public List<ServicioResponse> findAll(String search, Boolean active) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		Boolean activeFilter = active == null ? true : active;
		return servicioRepository.search(normalizedSearch, activeFilter).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ServicioResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Transactional
	public ServicioResponse update(Long id, ServicioRequest request) {
		Servicio servicio = findEntityById(id);

		validateUniqueName(request.nombre(), id);

		servicio.setNombre(normalizeText(request.nombre()));
		servicio.setDescripcion(normalizeText(request.descripcion()));
		servicio.setCostoBase(normalizeMoney(request.costoBase()));
		servicio.setUpdatedAt(LocalDateTime.now());

		return toResponse(servicioRepository.save(servicio));
	}

	@Transactional
	public void deactivate(Long id) {
		Servicio servicio = findEntityById(id);
		servicio.setActive(false);
		servicio.setUpdatedAt(LocalDateTime.now());
		servicioRepository.save(servicio);
	}

	@Transactional
	public ServicioResponse activate(Long id) {
		Servicio servicio = findEntityById(id);
		servicio.setActive(true);
		servicio.setUpdatedAt(LocalDateTime.now());
		return toResponse(servicioRepository.save(servicio));
	}

	@Transactional(readOnly = true)
	public CalculoCostoCitaResponse calculateCost(CalculoCostoCitaRequest request) {
		List<DetalleCostoCitaResponse> detalles = request.servicios().stream()
				.map(this::calculateDetail)
				.toList();
		BigDecimal subtotal = detalles.stream()
				.map(DetalleCostoCitaResponse::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		BigDecimal descuento = normalizeMoney(request.descuento() == null ? BigDecimal.ZERO : request.descuento());

		if (descuento.compareTo(subtotal) > 0) {
			throw new IllegalArgumentException("El descuento no puede ser mayor al subtotal.");
		}

		BigDecimal total = subtotal.subtract(descuento).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		return new CalculoCostoCitaResponse(detalles, subtotal, descuento, total);
	}

	private DetalleCostoCitaResponse calculateDetail(CostoCitaServicioRequest request) {
		Servicio servicio = findEntityById(request.servicioId());
		if (!servicio.getActive()) {
			throw new IllegalArgumentException("El servicio " + servicio.getNombre() + " no esta activo.");
		}

		BigDecimal subtotal = servicio.getCostoBase()
				.multiply(BigDecimal.valueOf(request.cantidad()))
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

		return new DetalleCostoCitaResponse(
				servicio.getId(),
				servicio.getNombre(),
				servicio.getCostoBase(),
				request.cantidad(),
				subtotal
		);
	}

	private Servicio findEntityById(Long id) {
		return servicioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado."));
	}

	private void validateUniqueName(String nombre, Long currentId) {
		String normalizedName = normalizeText(nombre);
		servicioRepository.findByNombreIgnoreCase(normalizedName)
				.filter(servicio -> currentId == null || !servicio.getId().equals(currentId))
				.ifPresent(servicio -> {
					throw new IllegalArgumentException("El nombre del servicio ya esta registrado.");
				});
	}

	private ServicioResponse toResponse(Servicio servicio) {
		return new ServicioResponse(
				servicio.getId(),
				servicio.getNombre(),
				servicio.getDescripcion(),
				servicio.getCostoBase(),
				servicio.getActive(),
				servicio.getCreatedAt(),
				servicio.getUpdatedAt()
		);
	}

	private BigDecimal normalizeMoney(BigDecimal value) {
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String normalizeText(String value) {
		return value.trim();
	}
}
