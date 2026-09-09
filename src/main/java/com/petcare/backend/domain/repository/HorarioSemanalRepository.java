package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.HorarioSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HorarioSemanalRepository extends JpaRepository<HorarioSemanal, Long> {
    List<HorarioSemanal> findByFechaSemana(LocalDate fechaSemana);
    Optional<HorarioSemanal> findByUsuarioIdAndFechaSemana(Long usuarioId, LocalDate fechaSemana);
    void deleteByUsuarioIdAndFechaSemana(Long usuarioId, LocalDate fechaSemana);

    @Query("select h from HorarioSemanal h where h.fechaSemana = :fechaSemana and h.usuarioId in (select u.id from Usuario u where u.clinica.id = :clinicaId)")
    List<HorarioSemanal> findScopedByFechaSemana(@Param("fechaSemana") LocalDate fechaSemana, @Param("clinicaId") Long clinicaId);
}
