package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Duenio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DuenioRepository extends JpaRepository<Duenio, Long> {

	boolean existsByEmail(String email);

	boolean existsByNumeroDocumento(String numeroDocumento);

	Optional<Duenio> findByEmail(String email);

	Optional<Duenio> findByNumeroDocumento(String numeroDocumento);

	Optional<Duenio> findByUsuarioId(Long usuarioId);

	@Query("""
			select d from Duenio d
			where (:active is null or d.active = :active)
			and (
				:search is null
				or lower(d.nombres) like lower(concat('%', :search, '%'))
				or lower(d.apellidos) like lower(concat('%', :search, '%'))
				or lower(d.email) like lower(concat('%', :search, '%'))
				or d.numeroDocumento like concat('%', :search, '%')
			)
			order by d.apellidos asc, d.nombres asc
			""")
	List<Duenio> search(@Param("search") String search, @Param("active") Boolean active);
}
