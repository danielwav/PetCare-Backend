package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.HorarioSemanalRequest;
import com.petcare.backend.domain.dto.response.HorarioSemanalResponse;
import com.petcare.backend.domain.repository.HorarioSemanalRepository;
import com.petcare.backend.persistence.entity.HorarioSemanal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioSemanalService {

    private final HorarioSemanalRepository repository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<HorarioSemanalResponse> findBySemana(LocalDate fechaSemana) {
        return repository.findByFechaSemana(fechaSemana).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<HorarioSemanalResponse> saveWeek(HorarioSemanalRequest request) {
        LocalDate semana = request.semana();
        for (var asig : request.asignaciones()) {
            var existing = repository.findByUsuarioIdAndFechaSemana(asig.usuarioId(), semana);
            existing.ifPresent(h -> {
                h.setLunes(asig.lunes());
                h.setMartes(asig.martes());
                h.setMiercoles(asig.miercoles());
                h.setJueves(asig.jueves());
                h.setViernes(asig.viernes());
                h.setSabado(asig.sabado());
                h.setDomingo(asig.domingo());
                h.setUpdatedAt(LocalDateTime.now());
                repository.save(h);
            });
            if (existing.isEmpty()) {
                repository.save(HorarioSemanal.builder()
                        .usuarioId(asig.usuarioId())
                        .fechaSemana(semana)
                        .lunes(asig.lunes()).martes(asig.martes())
                        .miercoles(asig.miercoles()).jueves(asig.jueves())
                        .viernes(asig.viernes()).sabado(asig.sabado())
                        .domingo(asig.domingo())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        }
        return findBySemana(semana);
    }

    @Transactional
    public void deleteByUsuarioAndSemana(Long usuarioId, LocalDate fechaSemana) {
        repository.deleteByUsuarioIdAndFechaSemana(usuarioId, fechaSemana);
    }

    private HorarioSemanalResponse toResponse(HorarioSemanal h) {
        var usuario = usuarioService.findUsuario(h.getUsuarioId());
        String nombre = usuario.getFullName();
        String rol = usuario.getRoles().stream().findFirst().map(r -> r.getName().name()).orElse("");
        return new HorarioSemanalResponse(
                h.getId(), h.getUsuarioId(), nombre, rol,
                h.getFechaSemana(),
                h.getLunes(), h.getMartes(), h.getMiercoles(),
                h.getJueves(), h.getViernes(), h.getSabado(), h.getDomingo()
        );
    }
}
