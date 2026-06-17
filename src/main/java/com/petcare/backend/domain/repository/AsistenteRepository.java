package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Asistente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsistenteRepository extends JpaRepository<Asistente, Long> {

	Optional<Asistente> findByEmail(String email);

	Optional<Asistente> findByNumeroDocumento(String numeroDocumento);

	Optional<Asistente> findByUsuarioId(Long usuarioId);

	@Query(value = "select a.* from asistentes a where (:active is null or a.active = :active) and (:search is null or upper(a.nombres) like upper('%' || :search || '%') or upper(a.apellidos) like upper('%' || :search || '%') or upper(a.email) like upper('%' || :search || '%') or upper(a.funciones) like upper('%' || :search || '%') or a.numero_documento like ('%' || :search || '%')) order by a.apellidos asc, a.nombres asc", nativeQuery = true)
	List<Asistente> search(@Param("search") String search, @Param("active") Boolean active);
}
