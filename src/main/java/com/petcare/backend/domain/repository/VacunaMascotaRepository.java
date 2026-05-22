package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.VacunaMascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VacunaMascotaRepository extends JpaRepository<VacunaMascota, Long> {

	List<VacunaMascota> findByMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

	List<VacunaMascota> findByFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(LocalDate start, LocalDate end);

	List<VacunaMascota> findByFechaProximaDosisLessThanEqualOrderByFechaProximaDosisAsc(LocalDate end);
}
