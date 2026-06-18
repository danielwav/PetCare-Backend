package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.NotaSeguimientoRequest;
import com.petcare.backend.domain.dto.response.NotaSeguimientoResponse;
import com.petcare.backend.domain.repository.NotaSeguimientoRepository;
import com.petcare.backend.persistence.entity.NotaSeguimiento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotaSeguimientoService {

    private final NotaSeguimientoRepository repository;

    @Transactional
    public NotaSeguimientoResponse create(NotaSeguimientoRequest request, String usuario) {
        NotaSeguimiento nota = NotaSeguimiento.builder()
                .citaId(request.citaId())
                .observacion(request.observacion().trim())
                .registradoPor(usuario)
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(repository.save(nota));
    }

    @Transactional(readOnly = true)
    public List<NotaSeguimientoResponse> findByCitaId(Long citaId) {
        return repository.findByCitaIdOrderByCreatedAtDesc(citaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotaSeguimientoResponse toResponse(NotaSeguimiento n) {
        return new NotaSeguimientoResponse(n.getId(), n.getCitaId(), n.getObservacion(), n.getRegistradoPor(), n.getCreatedAt());
    }
}
