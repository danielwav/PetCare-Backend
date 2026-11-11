package com.petcare.backend.domain.repository;

import com.petcare.backend.persistence.entity.MonthlyPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface MonthlyPaymentRepository extends Repository<MonthlyPayment, Long> {
	MonthlyPayment saveAndFlush(MonthlyPayment payment);

	Optional<MonthlyPayment> findByPaymentReference(String paymentReference);

	Optional<MonthlyPayment> findByAuditId(Long auditId);

	Page<MonthlyPayment> findByClinicId(Long clinicId, Pageable pageable);
}
