package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

	Optional<Veterinario> findByEmail(String email);

	Optional<Veterinario> findByNumeroColegiatura(String numeroColegiatura);

	Optional<Veterinario> findByUsuarioId(Long usuarioId);

	@Query("""
			select distinct v from Veterinario v
			left join fetch v.horarios h
			where (:active is null or v.active = :active)
			and (
				:search is null
				or lower(v.nombres) like lower(concat('%', :search, '%'))
				or lower(v.apellidos) like lower(concat('%', :search, '%'))
				or lower(v.email) like lower(concat('%', :search, '%'))
				or lower(v.especialidad) like lower(concat('%', :search, '%'))
				or v.numeroColegiatura like concat('%', :search, '%')
			)
			order by v.apellidos asc, v.nombres asc
			""")
	List<Veterinario> search(@Param("search") String search, @Param("active") Boolean active);
}
