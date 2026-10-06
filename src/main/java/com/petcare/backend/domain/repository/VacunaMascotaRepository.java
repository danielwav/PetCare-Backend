package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.VacunaMascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VacunaMascotaRepository extends JpaRepository<VacunaMascota, Long> {

	List<VacunaMascota> findByMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

	List<VacunaMascota> findByFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(LocalDate start, LocalDate end);

	List<VacunaMascota> findByMascotaClinicaIdAndFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(Long clinicaId, LocalDate start, LocalDate end);

	List<VacunaMascota> findByFechaProximaDosisLessThanEqualOrderByFechaProximaDosisAsc(LocalDate end);

	List<VacunaMascota> findByMascotaClinicaIdAndFechaProximaDosisLessThanEqualOrderByFechaProximaDosisAsc(Long clinicaId, LocalDate end);

	@Query("select vm from VacunaMascota vm where vm.mascota.duenio.id = :duenioId and vm.mascota.clinica.id = vm.mascota.duenio.clinica.id and vm.fechaProximaDosis <= :end order by vm.fechaProximaDosis asc")
	List<VacunaMascota> findByMascotaDuenioIdAndFechaProximaDosisLessThanEqualOrderByFechaProximaDosisAsc(@Param("duenioId") Long duenioId, @Param("end") LocalDate end);

	@Query("select vm from VacunaMascota vm where vm.mascota.duenio.id = :duenioId and vm.mascota.clinica.id = vm.mascota.duenio.clinica.id and vm.fechaProximaDosis between :start and :end order by vm.fechaProximaDosis asc")
	List<VacunaMascota> findByMascotaDuenioIdAndFechaProximaDosisBetweenOrderByFechaProximaDosisAsc(@Param("duenioId") Long duenioId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
