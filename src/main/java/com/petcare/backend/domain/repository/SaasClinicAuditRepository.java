package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.SaasClinicAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface SaasClinicAuditRepository extends Repository<SaasClinicAudit, Long> {
	SaasClinicAudit save(SaasClinicAudit audit);

	Page<SaasClinicAudit> findByClinicId(Long clinicId, Pageable pageable);
}
