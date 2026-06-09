package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.enums.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

	@Query("""
			select c from Cita c
			join fetch c.duenio d
			join fetch c.mascota m
			join fetch c.veterinario v
			where (cast(:estado as string) is null or c.estado = :estado)
			and (cast(:fecha as date) is null or c.fecha = :fecha)
			and (cast(:duenioId as long) is null or d.id = :duenioId)
			and (cast(:mascotaId as long) is null or m.id = :mascotaId)
			and (cast(:veterinarioId as long) is null or v.id = :veterinarioId)
			order by c.fecha asc, c.horaInicio asc
			""")
	List<Cita> search(
			@Param("estado") EstadoCita estado,
			@Param("fecha") LocalDate fecha,
			@Param("duenioId") Long duenioId,
			@Param("mascotaId") Long mascotaId,
			@Param("veterinarioId") Long veterinarioId
	);

	@Query("""
			select c from Cita c
			join fetch c.duenio d
			join fetch c.mascota m
			join fetch c.veterinario v
			where (cast(:estado as string) is null or c.estado = :estado)
			and (cast(:fechaInicio as date) is null or c.fecha >= :fechaInicio)
			and (cast(:fechaFin as date) is null or c.fecha <= :fechaFin)
			and (cast(:duenioId as long) is null or d.id = :duenioId)
			and (cast(:mascotaId as long) is null or m.id = :mascotaId)
			and (cast(:veterinarioId as long) is null or v.id = :veterinarioId)
			order by c.fecha asc, c.horaInicio asc
			""")
	List<Cita> searchByDateRange(
			@Param("estado") EstadoCita estado,
			@Param("fechaInicio") LocalDate fechaInicio,
			@Param("fechaFin") LocalDate fechaFin,
			@Param("duenioId") Long duenioId,
			@Param("mascotaId") Long mascotaId,
			@Param("veterinarioId") Long veterinarioId
	);

	@Query("""
			select c from Cita c
			where c.veterinario.id = :veterinarioId
			and c.fecha = :fecha
			and c.estado <> com.petcare.backend.persistence.enums.EstadoCita.CANCELADA
			and (cast(:currentCitaId as long) is null or c.id <> :currentCitaId)
			and c.horaInicio < :horaFin
			and c.horaFin > :horaInicio
			""")
	List<Cita> findOverlappingAppointments(
			@Param("veterinarioId") Long veterinarioId,
			@Param("fecha") LocalDate fecha,
			@Param("horaInicio") LocalTime horaInicio,
			@Param("horaFin") LocalTime horaFin,
			@Param("currentCitaId") Long currentCitaId
	);

	List<Cita> findByEstadoAndRequiereConfirmacionTrueOrderByFechaAscHoraInicioAsc(EstadoCita estado);

	List<Cita> findByMascotaIdAndFechaGreaterThanEqualAndEstadoIn(Long mascotaId, LocalDate fecha, List<EstadoCita> estados);
}
