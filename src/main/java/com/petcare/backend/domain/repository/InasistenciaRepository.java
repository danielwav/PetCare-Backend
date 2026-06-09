package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Inasistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InasistenciaRepository extends JpaRepository<Inasistencia, Long> {

	boolean existsByCitaId(Long citaId);

	Optional<Inasistencia> findByCitaId(Long citaId);

	@Query("""
			select i from Inasistencia i
			join fetch i.cita c
			join fetch i.duenio d
			join fetch i.mascota m
			where (cast(:duenioId as long) is null or d.id = :duenioId)
			and (cast(:fechaInicio as timestamp) is null or i.fechaRegistro >= :fechaInicio)
			and (cast(:fechaFin as timestamp) is null or i.fechaRegistro <= :fechaFin)
			order by i.fechaRegistro desc
			""")
	List<Inasistencia> search(
			@Param("duenioId") Long duenioId,
			@Param("fechaInicio") LocalDateTime fechaInicio,
			@Param("fechaFin") LocalDateTime fechaFin
	);
}
