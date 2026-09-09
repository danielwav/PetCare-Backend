package com.petcare.backend.persistence.entity;

import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinicas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clinica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, unique = true, length = 80)
	private String slug;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanClinica plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoClinica estado;

	@Column(length = 250)
	private String direccion;

	@Column(length = 20)
	private String telefono;

	@Column(length = 250)
	private String horarioAtencion;

	@Column(length = 1000)
	private String descripcion;

	@Column(length = 500)
	private String logoUrl;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;
}