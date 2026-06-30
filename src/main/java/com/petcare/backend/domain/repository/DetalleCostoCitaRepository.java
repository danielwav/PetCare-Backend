package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.DetalleCostoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DetalleCostoCitaRepository extends JpaRepository<DetalleCostoCita, Long> {

	List<DetalleCostoCita> findByCitaId(Long citaId);

	@Query("""
			select d.nombreServicio, sum(d.cantidad), sum(d.total)
			from DetalleCostoCita d
			join d.cita c
			where (cast(:fechaInicio as date) is null or c.fecha >= :fechaInicio)
			and (cast(:fechaFin as date) is null or c.fecha <= :fechaFin)
			group by d.nombreServicio
			order by sum(d.cantidad) desc, sum(d.total) desc
			""")
	List<Object[]> findMostRequestedServices(
			@Param("fechaInicio") LocalDate fechaInicio,
			@Param("fechaFin") LocalDate fechaFin
	);
}
