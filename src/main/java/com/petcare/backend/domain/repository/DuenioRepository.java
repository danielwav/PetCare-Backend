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

	Optional<Duenio> findByUsuarioEmail(String email);

	@Query(value = "select d.* from duenios d where (:active is null or d.active = :active) and (:search is null or upper(d.nombres) like upper('%' || :search || '%') or upper(d.apellidos) like upper('%' || :search || '%') or upper(d.email) like upper('%' || :search || '%') or d.numero_documento like ('%' || :search || '%')) order by d.apellidos asc, d.nombres asc", nativeQuery = true)
	List<Duenio> search(@Param("search") String search, @Param("active") Boolean active);
}
