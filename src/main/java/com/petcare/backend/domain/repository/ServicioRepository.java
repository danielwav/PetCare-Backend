package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

	Optional<Servicio> findByNombreIgnoreCase(String nombre);

	@Query("""
			select s from Servicio s
			where (:active is null or s.active = :active)
			and (
				:search is null
				or lower(s.nombre) like lower(concat('%', :search, '%'))
				or lower(s.descripcion) like lower(concat('%', :search, '%'))
			)
			order by s.nombre asc
			""")
	List<Servicio> search(@Param("search") String search, @Param("active") Boolean active);
}
