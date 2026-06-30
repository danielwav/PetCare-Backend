package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

	List<Mascota> findByDuenioIdOrderByNombreAsc(Long duenioId);

	@Query(value = "select m.* from mascotas m join duenios d on d.id = m.duenio_id where (:active is null or m.active = :active) and (:duenioId is null or d.id = :duenioId) and (:search is null or upper(m.nombre) like upper('%' || :search || '%') or upper(m.especie) like upper('%' || :search || '%') or upper(m.raza) like upper('%' || :search || '%') or upper(d.nombres) like upper('%' || :search || '%') or upper(d.apellidos) like upper('%' || :search || '%')) order by m.nombre asc", nativeQuery = true)
	List<Mascota> search(
			@Param("search") String search,
			@Param("duenioId") Long duenioId,
			@Param("active") Boolean active
	);

	@Query("""
			select m from Mascota m
			join fetch m.duenio d
			where m.active = true
			and not exists (
				select c from ControlMensualMascota c
				where c.mascota = m
				and c.anio = :anio
				and c.mes = :mes
			)
			order by m.nombre asc
			""")
	List<Mascota> findActivePetsWithoutMonthlyControl(
			@Param("anio") Integer anio,
			@Param("mes") Integer mes
	);
}
