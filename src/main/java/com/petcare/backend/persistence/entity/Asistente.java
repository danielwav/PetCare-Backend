package com.petcare.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "asistentes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asistente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", unique = true)
	private Usuario usuario;

	@Column(nullable = false, length = 80)
	private String nombres;

	@Column(nullable = false, length = 80)
	private String apellidos;

	@Column(nullable = false, length = 30)
	private String tipoDocumento;

	@Column(nullable = false, unique = true, length = 20)
	private String numeroDocumento;

	@Column(nullable = false, length = 9)
	private String telefono;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(nullable = false, length = 500)
	private String funciones;

	@Column(nullable = false)
	private Boolean active;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
