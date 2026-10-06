package com.petcare.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vacunas", uniqueConstraints = @UniqueConstraint(
		name = "uk_vacunas_clinica_nombre", columnNames = {"clinica_id", "nombre"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vacuna {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "clinica_id", nullable = true)
	private Clinica clinica;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 500)
	private String descripcion;

	private Integer intervaloProximaDosisDias;

	@Column(nullable = false)
	private Boolean active;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
