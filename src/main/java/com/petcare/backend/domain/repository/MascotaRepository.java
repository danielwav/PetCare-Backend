package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

	List<Mascota> findByDuenioIdOrderByNombreAsc(Long duenioId);

	@Query("""
			select m from Mascota m
			join fetch m.duenio d
			where (:active is null or m.active = :active)
			and (:duenioId is null or d.id = :duenioId)
			and (
				:search is null
				or lower(m.nombre) like lower(concat('%', :search, '%'))
				or lower(m.especie) like lower(concat('%', :search, '%'))
				or lower(m.raza) like lower(concat('%', :search, '%'))
				or lower(d.nombres) like lower(concat('%', :search, '%'))
				or lower(d.apellidos) like lower(concat('%', :search, '%'))
			)
			order by m.nombre asc
			""")
	List<Mascota> search(
			@Param("search") String search,
			@Param("duenioId") Long duenioId,
			@Param("active") Boolean active
	);
}
