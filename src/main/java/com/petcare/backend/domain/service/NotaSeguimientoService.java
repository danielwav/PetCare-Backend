package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.NotaSeguimientoRequest;
import com.petcare.backend.domain.dto.response.NotaSeguimientoResponse;
import com.petcare.backend.domain.repository.CitaRepository;
import com.petcare.backend.domain.repository.NotaSeguimientoRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.NotaSeguimiento;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotaSeguimientoService {

    private final NotaSeguimientoRepository repository;
    private final CitaRepository citaRepository;

    @Transactional
    public NotaSeguimientoResponse create(NotaSeguimientoRequest request, String usuario, Long clinicaId) {
        Cita cita = findCitaAndValidateClinic(request.citaId(), clinicaId);
        NotaSeguimiento nota = NotaSeguimiento.builder()
                .citaId(request.citaId())
                .observacion(request.observacion().trim())
                .registradoPor(usuario)
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(repository.save(nota));
    }

    @Transactional(readOnly = true)
    public List<NotaSeguimientoResponse> findByCitaId(Long citaId, Long clinicaId) {
        findCitaAndValidateClinic(citaId, clinicaId);
        return repository.findByCitaIdOrderByCreatedAtDesc(citaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Cita findCitaAndValidateClinic(Long citaId, Long clinicaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada."));
        if (cita.getClinica() == null || !cita.getClinica().getId().equals(clinicaId)) {
            throw new AccessDeniedException("La cita indicada no pertenece a tu clinica.");
        }
        return cita;
    }

    private NotaSeguimientoResponse toResponse(NotaSeguimiento n) {
        return new NotaSeguimientoResponse(n.getId(), n.getCitaId(), n.getObservacion(), n.getRegistradoPor(), n.getCreatedAt());
    }
}
