package com.petcare.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(length = 20)
	private String telefono;

	@Column(nullable = false)
	private Boolean active;

	@Builder.Default
	@Column(nullable = false)
	private Boolean forcePasswordChange = false;

	@Column(length = 255)
	private String activationToken;

	private LocalDateTime tokenExpiry;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder.Default
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "usuarios_roles",
			joinColumns = @JoinColumn(name = "usuario_id"),
			inverseJoinColumns = @JoinColumn(name = "rol_id")
	)
	private Set<Rol> roles = new HashSet<>();
}
