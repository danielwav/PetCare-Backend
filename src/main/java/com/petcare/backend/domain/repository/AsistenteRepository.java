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

	@Query("""
			select a from Asistente a
			where (:active is null or a.active = :active)
			and (
				:search is null
				or lower(a.nombres) like lower(concat('%', :search, '%'))
				or lower(a.apellidos) like lower(concat('%', :search, '%'))
				or lower(a.email) like lower(concat('%', :search, '%'))
				or lower(a.funciones) like lower(concat('%', :search, '%'))
				or a.numeroDocumento like concat('%', :search, '%')
			)
			order by a.apellidos asc, a.nombres asc
			""")
	List<Asistente> search(@Param("search") String search, @Param("active") Boolean active);
}
