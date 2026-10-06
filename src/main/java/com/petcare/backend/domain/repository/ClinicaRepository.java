package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Clinica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClinicaRepository extends JpaRepository<Clinica, Long> {

	Optional<Clinica> findBySlug(String slug);

	boolean existsBySlug(String slug);

	@Query("""
			select c from Clinica c
			where c.estado = com.petcare.backend.persistence.enums.EstadoClinica.ACTIVA
			and (:pattern is null
				or translate(lower(c.nombre), '\u00e1\u00e9\u00ed\u00f3\u00fa\u00fc', 'aeiouu') like :pattern escape '!'
				or translate(lower(c.direccion), '\u00e1\u00e9\u00ed\u00f3\u00fa\u00fc', 'aeiouu') like :pattern escape '!'
				or exists (select s.id from Servicio s where s.clinica = c and s.active = true
					and translate(lower(s.nombre), '\u00e1\u00e9\u00ed\u00f3\u00fa\u00fc', 'aeiouu') like :pattern escape '!'))
			""")
	Page<Clinica> findDirectory(@Param("pattern") String pattern, Pageable pageable);
}
