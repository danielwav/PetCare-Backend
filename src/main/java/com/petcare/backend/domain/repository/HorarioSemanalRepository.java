package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.HorarioSemanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HorarioSemanalRepository extends JpaRepository<HorarioSemanal, Long> {
    List<HorarioSemanal> findByFechaSemana(LocalDate fechaSemana);
    Optional<HorarioSemanal> findByUsuarioIdAndFechaSemana(Long usuarioId, LocalDate fechaSemana);
    void deleteByUsuarioIdAndFechaSemana(Long usuarioId, LocalDate fechaSemana);
}
