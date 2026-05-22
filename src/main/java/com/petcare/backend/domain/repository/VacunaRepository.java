package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VacunaRepository extends JpaRepository<Vacuna, Long> {

	Optional<Vacuna> findByNombreIgnoreCase(String nombre);

	@Query("""
			select v from Vacuna v
			where (:active is null or v.active = :active)
			and (
				:search is null
				or lower(v.nombre) like lower(concat('%', :search, '%'))
				or lower(v.descripcion) like lower(concat('%', :search, '%'))
			)
			order by v.nombre asc
			""")
	List<Vacuna> search(@Param("search") String search, @Param("active") Boolean active);
}
