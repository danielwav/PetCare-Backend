package com.petcare.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "atenciones_clinicas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtencionClinica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cita_id", nullable = false, unique = true)
	private Cita cita;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mascota_id", nullable = false)
	private Mascota mascota;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "veterinario_id", nullable = false)
	private Veterinario veterinario;

	@Column(nullable = false, length = 250)
	private String motivo;

	@Column(nullable = false, length = 1000)
	private String diagnostico;

	@Column(nullable = false, length = 1000)
	private String tratamiento;

	@Column(length = 1000)
	private String recomendaciones;

	@Column(length = 1000)
	private String observacionesClinicas;

	@Column(length = 1000)
	private String notasInternas;

	@Column(nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;
}
