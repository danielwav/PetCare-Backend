# Guia de desarrollo backend - PetCare

## 1. Objetivo del backend

El backend de PetCare tiene como objetivo centralizar la gestion clinica y operativa de una veterinaria. El sistema debe permitir administrar usuarios, roles, duenios, mascotas, citas, historias clinicas, vacunas, servicios, costos, alertas e inasistencias, evitando el uso disperso de papel, Excel y coordinaciones manuales.

La aplicacion se desarrolla como una API REST usando Java 21, Spring Boot, Spring Security con JWT, Spring Data JPA, MySQL, Maven y Lombok.

## 2. Alcance funcional

El backend cubre los siguientes modulos:

- Autenticacion y seguridad por roles.
- Gestion de duenios de mascotas.
- Gestion de mascotas.
- Gestion de veterinarios.
- Gestion de citas veterinarias.
- Confirmacion, reprogramacion, cancelacion e inasistencia de citas.
- Consulta de disponibilidad del veterinario.
- Registro de diagnosticos, tratamientos y observaciones clinicas.
- Historia clinica por mascota.
- Control mensual de la mascota.
- Gestion de vacunas, proxima dosis y alertas de vencimiento.
- Gestion de servicios veterinarios y costos.
- Calculo de costo total por cita.
- Panel de alertas del dia.

## 3. Stack tecnico

| Tecnologia | Uso en el backend |
| --- | --- |
| Java 21 | Lenguaje principal y version LTS del proyecto. |
| Spring Boot | Framework base para construir la API REST. |
| Spring Security | Autenticacion, autorizacion y proteccion de endpoints. |
| JWT | Manejo de sesiones stateless mediante tokens. |
| Spring Data JPA | Persistencia y consultas hacia la base de datos. |
| MySQL | Base de datos relacional del sistema. |
| Maven | Gestion de dependencias y ciclo de build. |
| Lombok | Reduccion de codigo repetitivo en entidades, DTO y servicios. |
| Swagger / OpenAPI | Documentacion y prueba de endpoints REST desde navegador. |

## 4. Arquitectura propuesta

El backend se organiza por capas para separar responsabilidades y facilitar mantenimiento.

```text
src/main/java/com/petcare/backend
+-- config
|   +-- CorsConfig.java
|   +-- OpenApiConfig.java
|   +-- SecurityConfig.java
+-- security
|   +-- JwtAuthenticationFilter.java
|   +-- JwtProperties.java
|   +-- JwtService.java
|   +-- CustomUserDetailsService.java
+-- domain
|   +-- dto
|   |   +-- request
|   |   +-- response
|   +-- service
|   +-- repository
+-- persistence
|   +-- entity
|   +-- enums
+-- web
|   +-- AuthController.java
|   +-- UsuarioController.java
|   +-- DuenioController.java
|   +-- MascotaController.java
|   +-- VeterinarioController.java
|   +-- ServicioController.java
|   +-- CitaController.java
|   +-- HistoriaClinicaController.java
|   +-- VacunaController.java
|   +-- AlertaController.java
+-- PetCareBackendApplication.java
```

### Responsabilidad por capa

| Capa | Responsabilidad |
| --- | --- |
| `web` | Recibir peticiones HTTP y devolver respuestas REST. |
| `domain.service` | Ejecutar reglas de negocio: citas, vacunas, costos, alertas. |
| `domain.repository` | Definir operaciones de acceso a datos mediante Spring Data JPA. |
| `persistence.entity` | Mapear tablas de MySQL como entidades JPA. |
| `domain.dto` | Transportar datos sin exponer directamente las entidades. |
| `security` | Validar JWT, autenticar usuarios y aplicar roles. |
| `config` | Configurar seguridad, CORS, codificador de contrasenas y beans. |

### Estructura concreta por modulo

Para que el desarrollo sea mas claro, cada modulo debe tener entidad, repositorio, servicio, DTO y controlador. Ejemplo con `duenios`:

```text
persistence/entity/Duenio.java
domain/repository/DuenioRepository.java
domain/service/DuenioService.java
domain/dto/request/DuenioRequest.java
domain/dto/response/DuenioResponse.java
web/DuenioController.java
```

La misma idea se repite para `Mascota`, `Veterinario`, `Servicio`, `Cita`, `Vacuna`, `HistoriaClinica`, `ControlMensualMascota`, `Inasistencia` y `Alerta`.

### Paquetes que se deben crear primero

```text
src/main/java/com/petcare/backend/config
src/main/java/com/petcare/backend/security
src/main/java/com/petcare/backend/persistence/entity
src/main/java/com/petcare/backend/persistence/enums
src/main/java/com/petcare/backend/domain/repository
src/main/java/com/petcare/backend/domain/service
src/main/java/com/petcare/backend/domain/dto/request
src/main/java/com/petcare/backend/domain/dto/response
src/main/java/com/petcare/backend/web
```

## 5. Roles del sistema

Los roles deben quedar definidos desde los requerimientos funcionales porque condicionan que acciones puede ejecutar cada usuario.

| Rol | Permisos principales |
| --- | --- |
| `ADMIN` | Gestionar usuarios, roles, servicios, veterinarios, costos y configuraciones generales. |
| `VETERINARIO` | Consultar agenda, atender citas, registrar diagnosticos, tratamientos, observaciones clinicas, vacunas y controles mensuales. |
| `ASISTENTE` | Registrar duenios, mascotas, programar citas, confirmar citas, cancelar citas y consultar alertas del dia. |
| `DUENIO` | Consultar sus mascotas, citas, vacunas pendientes y estado de atenciones permitidas. |

### Roles que se crearan en base de datos

En el backend se manejaran con el prefijo usado por Spring Security:

```text
ROLE_ADMIN
ROLE_VETERINARIO
ROLE_ASISTENTE
ROLE_DUENIO
```

### Como se implementaran los roles

Se creara un enum para evitar escribir nombres de roles como texto suelto:

```java
public enum RoleName {
	ROLE_ADMIN,
	ROLE_VETERINARIO,
	ROLE_ASISTENTE,
	ROLE_DUENIO
}
```

Luego se creara la entidad `Rol`:

```text
persistence/entity/Rol.java
```

Campos recomendados:

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador autogenerado. |
| `name` | `RoleName` | Nombre del rol. Debe ser unico. |
| `description` | `String` | Descripcion del rol. |
| `active` | `Boolean` | Permite activar o desactivar el rol. |

### Tablas esperadas para roles y usuarios

Hibernate creara estas tablas cuando existan las entidades:

```text
roles
usuarios
usuarios_roles
```

La relacion recomendada es:

- Un usuario puede tener uno o varios roles.
- Un rol puede pertenecer a varios usuarios.
- La tabla intermedia sera `usuarios_roles`.

### Datos iniciales de roles

Al iniciar la aplicacion por primera vez se debe insertar la data base de roles. Esto se puede hacer con un `CommandLineRunner` o `ApplicationRunner`.

Archivo sugerido:

```text
config/DataInitializer.java
```

Responsabilidad:

- Verificar si cada rol existe.
- Si no existe, crearlo.
- No duplicar roles si la aplicacion se reinicia.

Datos a insertar:

| Nombre | Descripcion |
| --- | --- |
| `ROLE_ADMIN` | Administrador general del sistema. |
| `ROLE_VETERINARIO` | Personal medico veterinario. |
| `ROLE_ASISTENTE` | Personal operativo de recepcion y agenda. |
| `ROLE_DUENIO` | Cliente o propietario de mascota. |

### Permisos por endpoint

| Modulo | ADMIN | VETERINARIO | ASISTENTE | DUENIO |
| --- | --- | --- | --- | --- |
| Usuarios y roles | Si | No | No | No |
| Duenios | Si | Consulta | Si | Solo su perfil |
| Mascotas | Si | Consulta | Si | Solo sus mascotas |
| Veterinarios | Si | Consulta propia | Consulta | No |
| Servicios y costos | Si | Consulta | Consulta | Consulta |
| Citas | Si | Agenda propia | Si | Sus citas |
| Historia clinica | Si | Si | Consulta limitada | Consulta limitada |
| Vacunas | Si | Si | Consulta | Sus mascotas |
| Alertas del dia | Si | Si | Si | No |

## 6. Modelo de datos base

Entidades principales recomendadas:

| Entidad | Proposito |
| --- | --- |
| `Usuario` | Datos de acceso al sistema: correo, contrasena y estado. |
| `Rol` | Define permisos y perfiles del sistema. |
| `Duenio` | Informacion personal del propietario de una o varias mascotas. |
| `Mascota` | Datos de la mascota: nombre, especie, raza, sexo, fecha de nacimiento y duenio. |
| `Veterinario` | Datos profesionales del veterinario y horarios disponibles. |
| `Servicio` | Servicios veterinarios con costo base: consulta, vacuna, control, tratamiento, etc. |
| `Cita` | Programacion de atencion entre mascota, duenio, veterinario y servicio. |
| `DetalleCostoCita` | Servicios y costos asociados a una cita. |
| `HistoriaClinica` | Registro clinico general asociado a la mascota. |
| `AtencionClinica` | Diagnostico, tratamiento y observaciones de una cita atendida. |
| `ControlMensualMascota` | Seguimiento mensual de peso, talla, condicion y observaciones. |
| `Vacuna` | Catalogo o registro de vacunas aplicadas. |
| `VacunaMascota` | Aplicacion de vacuna, fecha, proxima dosis y estado de alerta. |
| `Inasistencia` | Registro de citas no asistidas y motivo si corresponde. |
| `Alerta` | Alertas del dia: citas, vacunas por vencer, dosis proximas y confirmaciones pendientes. |

### Relaciones principales

```text
Usuario *---* Rol
Duenio 1---1 Usuario
Duenio 1---* Mascota
Mascota 1---* Cita
Veterinario 1---* Cita
Cita 1---* DetalleCostoCita
Servicio 1---* DetalleCostoCita
Mascota 1---1 HistoriaClinica
HistoriaClinica 1---* AtencionClinica
Mascota 1---* VacunaMascota
Mascota 1---* ControlMensualMascota
Cita 1---0..1 Inasistencia
```

### Creacion automatica de tablas

La base de datos `petcare` ya debe existir en MySQL. Las tablas se crearan automaticamente cuando se implementen las entidades JPA, porque el proyecto usa:

```properties
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
```

En desarrollo se usara `update` para que Hibernate cree o actualice tablas. En produccion se recomienda usar migraciones con Flyway o Liquibase.

## 7. Estados importantes

### Estados de cita

```text
PROGRAMADA
CONFIRMADA
ATENDIDA
CANCELADA
REPROGRAMADA
NO_ASISTIO
```

### Estados de vacuna

```text
APLICADA
PROXIMA
VENCIDA
CANCELADA
```

### Estados de alerta

```text
PENDIENTE
VISTA
RESUELTA
```

## 8. Guia por pasos de desarrollo

### Paso 1: Preparar la base del proyecto

**Que se esta haciendo:** se crea la estructura inicial del backend con Spring Boot, Maven y Java 21.

**Actividades:**

- Crear el proyecto Spring Boot.
- Configurar `pom.xml` con dependencias de Web, Security, JPA, MySQL, Lombok y JWT.
- Configurar `application.properties`.
- Definir conexion a MySQL mediante variables de entorno.
- Crear endpoint de salud `GET /api/health`.
- Preparar Swagger/OpenAPI para documentar la API.

**Archivos creados o esperados:**

```text
pom.xml
src/main/resources/application.properties
src/main/java/com/petcare/backend/config/OpenApiConfig.java
src/main/java/com/petcare/backend/health/HealthController.java
src/test/java/com/petcare/backend/health/HealthControllerTest.java
```

**Configuracion esperada en `application.properties`:**

```properties
server.port=${SERVER_PORT:8080}

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/petcare?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
spring.jpa.show-sql=${JPA_SHOW_SQL:false}
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.format_sql=true

springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
```

**Variables locales para MySQL:**

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="admin"
```

**Resultado esperado:**

- El backend compila correctamente.
- El endpoint de salud responde.
- La aplicacion puede conectarse a MySQL.
- Swagger queda disponible en `http://localhost:8080/swagger-ui.html`.

### Paso 2: Configurar seguridad y JWT

**Que se esta haciendo:** se protege la API para que solo usuarios autenticados puedan acceder a los modulos internos.

**Actividades:**

- Crear entidad `Usuario`.
- Crear entidad `Rol`.
- Crear enum `RoleName`.
- Crear repositorios `UsuarioRepository` y `RolRepository`.
- Crear carga inicial de roles con `DataInitializer`.
- Implementar registro e inicio de sesion.
- Encriptar contrasenas con `BCryptPasswordEncoder`.
- Generar JWT al iniciar sesion.
- Validar JWT en cada request.
- Configurar rutas publicas y privadas.
- Aplicar autorizacion por roles.
- Permitir rutas publicas de Swagger.

**Archivos a crear en este paso:**

```text
persistence/enums/RoleName.java
persistence/entity/Rol.java
persistence/entity/Usuario.java
domain/repository/RolRepository.java
domain/repository/UsuarioRepository.java
domain/dto/request/LoginRequest.java
domain/dto/request/RegisterRequest.java
domain/dto/response/AuthResponse.java
domain/service/AuthService.java
security/CustomUserDetailsService.java
web/AuthController.java
config/DataInitializer.java
```

**Rutas publicas que debe permitir seguridad:**

```text
/api/auth/**
/api/health
/swagger-ui/**
/v3/api-docs/**
```

**Tablas que deben aparecer al ejecutar el proyecto:**

```text
roles
usuarios
usuarios_roles
```

**Roles iniciales que debe insertar el backend:**

```text
ROLE_ADMIN
ROLE_VETERINARIO
ROLE_ASISTENTE
ROLE_DUENIO
```

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Registra usuario inicial o usuario duenio. |
| `POST` | `/api/auth/login` | Autentica y devuelve JWT. |
| `GET` | `/api/auth/me` | Devuelve el usuario autenticado. |

**Resultado esperado:**

- El sistema permite iniciar sesion.
- Cada request privado requiere token JWT.
- Los usuarios solo acceden a modulos permitidos por su rol.

### Paso 3: Gestionar duenios

**Que se esta haciendo:** se digitaliza el registro de propietarios de mascotas.

**Actividades:**

- Crear entidad `Duenio`.
- Relacionar `Duenio` con `Usuario`.
- Crear DTO de registro y actualizacion.
- Implementar CRUD.
- Validar datos personales, telefono, correo y documento.

**Archivos creados en este paso:**

```text
persistence/entity/Duenio.java
domain/repository/DuenioRepository.java
domain/dto/request/DuenioRequest.java
domain/dto/response/DuenioResponse.java
domain/service/DuenioService.java
web/DuenioController.java
```

**Tabla que creara Hibernate:**

```text
duenios
```

**Campos principales de `duenios`:**

| Campo | Descripcion |
| --- | --- |
| `id` | Identificador autogenerado. |
| `usuario_id` | Relacion opcional 1 a 1 con `usuarios`. |
| `nombres` | Nombres del propietario. |
| `apellidos` | Apellidos del propietario. |
| `tipo_documento` | Tipo de documento, por ejemplo DNI o CE. |
| `numero_documento` | Numero de documento unico. |
| `telefono` | Telefono de contacto. |
| `email` | Correo unico del propietario. |
| `direccion` | Direccion opcional. |
| `active` | Estado logico del registro. |
| `created_at` | Fecha de creacion. |
| `updated_at` | Fecha de ultima actualizacion. |

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/duenios` | Crear duenio. |
| `GET` | `/api/duenios` | Listar duenios con filtros. |
| `GET` | `/api/duenios/{id}` | Obtener detalle de duenio. |
| `PUT` | `/api/duenios/{id}` | Actualizar duenio. |
| `DELETE` | `/api/duenios/{id}` | Desactivar duenio. |

**Permisos definidos:**

| Endpoint | Roles |
| --- | --- |
| `POST /api/duenios` | `ADMIN`, `ASISTENTE` |
| `GET /api/duenios` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/duenios/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/duenios/{id}` | `ADMIN`, `ASISTENTE` |
| `DELETE /api/duenios/{id}` | `ADMIN`, `ASISTENTE` |

**Resultado esperado:**

- La veterinaria puede registrar, buscar y editar duenios.
- Cada duenio puede tener una o varias mascotas.

### Paso 4: Gestionar mascotas

**Que se esta haciendo:** se registra la informacion clinica base de cada mascota asociada a un duenio.

**Actividades:**

- Crear entidad `Mascota`.
- Relacionar `Mascota` con `Duenio`.
- Registrar especie, raza, sexo, fecha de nacimiento y datos basicos.
- Listar mascotas por duenio.
- Preparar relacion con historia clinica, citas, vacunas y controles mensuales.

**Archivos creados en este paso:**

```text
persistence/entity/Mascota.java
persistence/enums/SexoMascota.java
domain/repository/MascotaRepository.java
domain/dto/request/MascotaRequest.java
domain/dto/response/MascotaResponse.java
domain/service/MascotaService.java
web/MascotaController.java
```

**Tabla que creara Hibernate:**

```text
mascotas
```

**Campos principales de `mascotas`:**

| Campo | Descripcion |
| --- | --- |
| `id` | Identificador autogenerado. |
| `duenio_id` | Relacion obligatoria con `duenios`. |
| `nombre` | Nombre de la mascota. |
| `especie` | Especie, por ejemplo perro o gato. |
| `raza` | Raza de la mascota. |
| `sexo` | `MACHO` o `HEMBRA`. |
| `fecha_nacimiento` | Fecha de nacimiento aproximada o real. |
| `color` | Color o descripcion visual opcional. |
| `peso_kg` | Peso actual opcional en kilogramos. |
| `observaciones` | Observaciones generales opcionales. |
| `active` | Estado logico del registro. |
| `created_at` | Fecha de creacion. |
| `updated_at` | Fecha de ultima actualizacion. |

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/mascotas` | Crear mascota. |
| `GET` | `/api/mascotas` | Listar mascotas. |
| `GET` | `/api/duenios/{id}/mascotas` | Listar mascotas por duenio. |
| `GET` | `/api/mascotas/{id}` | Obtener detalle de mascota. |
| `PUT` | `/api/mascotas/{id}` | Actualizar mascota. |
| `DELETE` | `/api/mascotas/{id}` | Desactivar mascota. |

**Permisos definidos:**

| Endpoint | Roles |
| --- | --- |
| `POST /api/mascotas` | `ADMIN`, `ASISTENTE` |
| `GET /api/mascotas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/duenios/{id}/mascotas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/mascotas/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/mascotas/{id}` | `ADMIN`, `ASISTENTE` |
| `DELETE /api/mascotas/{id}` | `ADMIN`, `ASISTENTE` |

**Resultado esperado:**

- Cada mascota queda asociada a su propietario.
- La informacion queda disponible para citas, vacunas e historia clinica.

### Paso 5: Gestionar veterinarios y disponibilidad

**Que se esta haciendo:** se registra al personal veterinario y sus horarios disponibles para programar citas sin cruces.

**Actividades:**

- Crear entidad `Veterinario`.
- Definir horarios de atencion por dia.
- Crear reglas de disponibilidad.
- Validar que no existan citas cruzadas para el mismo veterinario.
- Permitir consultar disponibilidad por fecha, veterinario y servicio.

**Archivos creados en este paso:**

```text
persistence/entity/Veterinario.java
persistence/entity/HorarioVeterinario.java
domain/repository/VeterinarioRepository.java
domain/repository/HorarioVeterinarioRepository.java
domain/dto/request/VeterinarioRequest.java
domain/dto/request/HorarioVeterinarioRequest.java
domain/dto/response/VeterinarioResponse.java
domain/dto/response/HorarioVeterinarioResponse.java
domain/dto/response/DisponibilidadVeterinarioResponse.java
domain/service/VeterinarioService.java
web/VeterinarioController.java
```

**Tablas que creara Hibernate:**

```text
veterinarios
horarios_veterinarios
```

**Campos principales de `veterinarios`:**

| Campo | Descripcion |
| --- | --- |
| `id` | Identificador autogenerado. |
| `usuario_id` | Relacion opcional 1 a 1 con `usuarios`. |
| `nombres` | Nombres del veterinario. |
| `apellidos` | Apellidos del veterinario. |
| `numero_colegiatura` | Numero de colegiatura unico. |
| `especialidad` | Especialidad o area principal. |
| `telefono` | Telefono de contacto. |
| `email` | Correo unico del veterinario. |
| `active` | Estado logico del registro. |
| `created_at` | Fecha de creacion. |
| `updated_at` | Fecha de ultima actualizacion. |

**Campos principales de `horarios_veterinarios`:**

| Campo | Descripcion |
| --- | --- |
| `id` | Identificador autogenerado. |
| `veterinario_id` | Relacion obligatoria con `veterinarios`. |
| `dia_semana` | Dia de atencion, por ejemplo `MONDAY`. |
| `hora_inicio` | Hora de inicio de atencion. |
| `hora_fin` | Hora de fin de atencion. |
| `duracion_bloque_minutos` | Intervalo usado para generar bloques disponibles. |
| `active` | Estado logico del horario. |

**Nota sobre cruces de citas:**

En este paso la disponibilidad se calcula desde horarios activos del veterinario. Desde el Paso 7, la programacion de citas tambien valida cruces contra citas ya registradas.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/veterinarios` | Crear veterinario. |
| `GET` | `/api/veterinarios` | Listar veterinarios. |
| `GET` | `/api/veterinarios/{id}` | Obtener detalle de veterinario. |
| `GET` | `/api/veterinarios/{id}/disponibilidad` | Consultar disponibilidad. |
| `PUT` | `/api/veterinarios/{id}` | Actualizar veterinario. |
| `DELETE` | `/api/veterinarios/{id}` | Desactivar veterinario. |

**Permisos definidos:**

| Endpoint | Roles |
| --- | --- |
| `POST /api/veterinarios` | `ADMIN` |
| `GET /api/veterinarios` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/veterinarios/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/veterinarios/{id}/disponibilidad` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/veterinarios/{id}` | `ADMIN` |
| `DELETE /api/veterinarios/{id}` | `ADMIN` |

**Resultado esperado:**

- El frontend puede mostrar horarios disponibles.
- El backend evita programar citas en horarios ocupados.

### Paso 6: Gestionar servicios y costos

**Que se esta haciendo:** se define el catalogo de servicios veterinarios y sus costos para calcular el costo total de cada cita.

**Archivos principales:**

```text
persistence/entity/Servicio.java
persistence/entity/DetalleCostoCita.java
domain/repository/ServicioRepository.java
domain/repository/DetalleCostoCitaRepository.java
domain/dto/request/ServicioRequest.java
domain/dto/request/CalculoCostoCitaRequest.java
domain/dto/request/CostoCitaServicioRequest.java
domain/dto/response/ServicioResponse.java
domain/dto/response/CalculoCostoCitaResponse.java
domain/dto/response/DetalleCostoCitaResponse.java
domain/service/ServicioService.java
web/ServicioController.java
```

**Entidades creadas:**

#### `Servicio`

Representa el catalogo de servicios que ofrece la veterinaria.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador del servicio. |
| `nombre` | `String` | Nombre unico del servicio. |
| `descripcion` | `String` | Descripcion visible para administracion y agenda. |
| `costoBase` | `BigDecimal` | Precio base del servicio. |
| `active` | `Boolean` | Permite activar o desactivar el servicio sin eliminarlo. |
| `createdAt` | `LocalDateTime` | Fecha de creacion. |
| `updatedAt` | `LocalDateTime` | Fecha de ultima actualizacion. |

#### `DetalleCostoCita`

Representa el detalle economico de los servicios asociados a una cita.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador del detalle. |
| `cita` | `Cita` | Cita a la que pertenece el detalle de costo. |
| `servicio` | `Servicio` | Servicio usado en la cita. |
| `nombreServicio` | `String` | Copia del nombre del servicio al momento de calcular. |
| `costoUnitario` | `BigDecimal` | Costo unitario aplicado. |
| `cantidad` | `Integer` | Cantidad del servicio. |
| `subtotal` | `BigDecimal` | Costo antes de descuento. |
| `descuento` | `BigDecimal` | Descuento aplicado al detalle o cita. |
| `total` | `BigDecimal` | Total final. |
| `createdAt` | `LocalDateTime` | Fecha de creacion del detalle. |

**Actividades implementadas:**

- Crear entidad `Servicio`.
- Registrar nombre, descripcion, costo base y estado.
- Permitir activar o desactivar servicios.
- Crear entidad `DetalleCostoCita`.
- Preparar la asociacion de uno o varios servicios a una cita mediante `DetalleCostoCita`.
- Calcular subtotal, descuento y costo total a partir de servicios activos.
- Validar que no existan nombres de servicios duplicados.
- Evitar calcular costos con servicios inactivos.

**Reglas de negocio:**

- Solo `ADMIN` puede crear, editar, activar o desactivar servicios.
- `ADMIN`, `ASISTENTE` y `VETERINARIO` pueden consultar servicios.
- Por defecto `GET /api/servicios` devuelve solo servicios activos.
- El descuento no puede ser mayor al subtotal calculado.
- El costo se maneja con `BigDecimal` y dos decimales.
- La relacion directa con `Cita` se completa en el Paso 7 mediante `@ManyToOne`.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/servicios` | Crear servicio. |
| `GET` | `/api/servicios` | Listar servicios activos por defecto. Permite `search` y `active`. |
| `GET` | `/api/servicios/{id}` | Obtener detalle de un servicio. |
| `PUT` | `/api/servicios/{id}` | Actualizar servicio y costo. |
| `PATCH` | `/api/servicios/{id}/activar` | Reactivar servicio desactivado. |
| `DELETE` | `/api/servicios/{id}` | Desactivar servicio. |
| `POST` | `/api/servicios/calcular-costo` | Calcular subtotal, descuento y total para una cita. |

**Ejemplo de request para crear servicio:**

```json
{
  "nombre": "Consulta general",
  "descripcion": "Evaluacion clinica basica de la mascota.",
  "costoBase": 50.00
}
```

**Ejemplo de request para calcular costo:**

```json
{
  "servicios": [
    {
      "servicioId": 1,
      "cantidad": 1
    },
    {
      "servicioId": 2,
      "cantidad": 2
    }
  ],
  "descuento": 10.00
}
```

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `POST /api/servicios` | `ADMIN` |
| `GET /api/servicios` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/servicios/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/servicios/{id}` | `ADMIN` |
| `PATCH /api/servicios/{id}/activar` | `ADMIN` |
| `DELETE /api/servicios/{id}` | `ADMIN` |
| `POST /api/servicios/calcular-costo` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Resultado esperado:**

- Los costos quedan centralizados.
- Cada cita puede calcular su costo total automaticamente.
- El modulo de citas reutiliza `Servicio` y `DetalleCostoCita` sin duplicar la logica de costos.

### Paso 7: Programar citas veterinarias

**Que se esta haciendo:** se implementa el flujo de agendamiento de citas entre mascota, duenio, veterinario y servicio.

**Archivos principales:**

```text
persistence/enums/EstadoCita.java
persistence/entity/Cita.java
persistence/entity/DetalleCostoCita.java
domain/repository/CitaRepository.java
domain/dto/request/CitaRequest.java
domain/dto/response/CitaResponse.java
domain/service/CitaService.java
web/CitaController.java
```

**Entidades creadas o actualizadas:**

#### `Cita`

Representa una cita programada entre un duenio, una mascota, un veterinario y uno o varios servicios.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador de la cita. |
| `duenio` | `Duenio` | Propietario asociado a la cita. |
| `mascota` | `Mascota` | Mascota que recibira la atencion. |
| `veterinario` | `Veterinario` | Veterinario asignado. |
| `fecha` | `LocalDate` | Fecha programada. |
| `horaInicio` | `LocalTime` | Hora de inicio. |
| `horaFin` | `LocalTime` | Hora calculada segun la duracion. |
| `duracionMinutos` | `Integer` | Duracion de la cita. |
| `motivo` | `String` | Motivo de la cita. |
| `estado` | `EstadoCita` | Estado actual de la cita. |
| `subtotal` | `BigDecimal` | Suma de servicios antes de descuento. |
| `descuento` | `BigDecimal` | Descuento aplicado. |
| `total` | `BigDecimal` | Total final. |
| `detallesCosto` | `List<DetalleCostoCita>` | Servicios asociados con cantidades y costos. |
| `createdAt` | `LocalDateTime` | Fecha de creacion. |
| `updatedAt` | `LocalDateTime` | Fecha de ultima actualizacion. |

#### `EstadoCita`

Define los estados disponibles para el ciclo de vida de una cita:

```java
PROGRAMADA,
CONFIRMADA,
CANCELADA,
ATENDIDA,
NO_ASISTIO
```

**Actividades implementadas:**

- Crear entidad `Cita`.
- Relacionar cita con mascota, duenio, veterinario y servicios.
- Validar disponibilidad antes de guardar.
- Validar fecha y hora futuras.
- Calcular costo total al registrar o actualizar servicios.
- Definir estado inicial `PROGRAMADA`.
- Cancelar citas cambiando su estado a `CANCELADA`.
- Listar citas con filtros por estado, fecha, duenio, mascota y veterinario.

**Reglas de negocio:**

- Solo se pueden programar citas en fecha y hora futura.
- La mascota debe pertenecer al duenio indicado.
- Duenio, mascota, veterinario y servicios deben estar activos.
- La cita debe encajar dentro del horario activo del veterinario.
- No se permite crear o actualizar una cita si cruza con otra cita del mismo veterinario.
- Las citas canceladas no bloquean disponibilidad.
- Al crear una cita, el estado inicial es `PROGRAMADA`.
- No se puede modificar una cita cancelada.
- El total se calcula como `subtotal - descuento`.
- El descuento no puede ser mayor al subtotal.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/citas` | Programar cita. |
| `GET` | `/api/citas` | Listar citas con filtros por estado, fecha, duenio, mascota y veterinario. |
| `GET` | `/api/citas/{id}` | Obtener detalle de cita. |
| `PUT` | `/api/citas/{id}` | Reprogramar o actualizar cita. |
| `PATCH` | `/api/citas/{id}/cancelar` | Cancelar cita. |

**Ejemplo de request para programar cita:**

```json
{
  "duenioId": 1,
  "mascotaId": 1,
  "veterinarioId": 1,
  "fecha": "2026-05-25",
  "horaInicio": "09:00:00",
  "duracionMinutos": 30,
  "motivo": "Consulta preventiva",
  "servicios": [
    {
      "servicioId": 1,
      "cantidad": 1
    },
    {
      "servicioId": 2,
      "cantidad": 1
    }
  ],
  "descuento": 10.00
}
```

**Filtros disponibles en `GET /api/citas`:**

| Parametro | Ejemplo | Descripcion |
| --- | --- | --- |
| `estado` | `PROGRAMADA` | Filtra por estado de cita. |
| `fecha` | `2026-05-25` | Filtra por fecha exacta. |
| `duenioId` | `1` | Filtra por duenio. |
| `mascotaId` | `1` | Filtra por mascota. |
| `veterinarioId` | `1` | Filtra por veterinario. |

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `POST /api/citas` | `ADMIN`, `ASISTENTE` |
| `GET /api/citas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/citas/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/citas/{id}` | `ADMIN`, `ASISTENTE` |
| `PATCH /api/citas/{id}/cancelar` | `ADMIN`, `ASISTENTE` |
| `PATCH /api/citas/{id}/confirmar` | `ADMIN`, `ASISTENTE`, `DUENIO` |
| `GET /api/citas/alertas-confirmacion` | `ADMIN`, `ASISTENTE` |

**Resultado esperado:**

- La veterinaria puede registrar citas sin choques de horario.
- Cada cita tiene costo total calculado.
- El historial de la mascota queda preparado para la atencion.

### Paso 8: Confirmar citas antes de la fecha programada

**Que se esta haciendo:** se agrega control previo de asistencia para reducir ausencias y mejorar la organizacion diaria.

**Archivos principales actualizados:**

```text
persistence/entity/Cita.java
domain/dto/response/CitaResponse.java
domain/repository/CitaRepository.java
domain/service/CitaService.java
web/CitaController.java
```

**Campos agregados en `Cita`:**

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `requiereConfirmacion` | `Boolean` | Indica si la cita aun debe ser confirmada. |
| `fechaConfirmacion` | `LocalDateTime` | Fecha y hora en que se confirmo la cita. |
| `confirmadaPor` | `String` | Usuario autenticado que realizo la confirmacion. |

**Actividades implementadas:**

- Agregar campos `requiereConfirmacion`, `fechaConfirmacion` y `confirmadaPor`.
- Permitir confirmar cita desde el panel del asistente o duenio.
- Generar alerta cuando una cita cercana aun no esta confirmada.
- Definir regla de negocio: alertar 24 horas antes por defecto si sigue `PROGRAMADA`.
- Al crear o reprogramar una cita, queda con `requiereConfirmacion = true`.
- Al confirmar una cita, cambia a estado `CONFIRMADA`.
- Registrar automaticamente el usuario autenticado en `confirmadaPor`.

**Reglas de negocio:**

- Solo se pueden confirmar citas futuras.
- No se pueden confirmar citas `CANCELADA`, `ATENDIDA` o `NO_ASISTIO`.
- Una cita confirmada deja de requerir confirmacion.
- Las alertas muestran citas `PROGRAMADA` con `requiereConfirmacion = true`.
- La ventana de alerta por defecto es de 24 horas, pero se puede ajustar con el parametro `horas`.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `PATCH` | `/api/citas/{id}/confirmar` | Confirmar cita antes de la fecha programada. |
| `GET` | `/api/citas/alertas-confirmacion` | Listar citas proximas sin confirmar. |

**Ejemplo de uso para alertas:**

```text
GET /api/citas/alertas-confirmacion
GET /api/citas/alertas-confirmacion?horas=48
```

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `PATCH /api/citas/{id}/confirmar` | `ADMIN`, `ASISTENTE`, `DUENIO` |
| `GET /api/citas/alertas-confirmacion` | `ADMIN`, `ASISTENTE` |

**Resultado esperado:**

- Las citas proximas pueden marcarse como `CONFIRMADA`.
- El panel del dia muestra citas sin confirmar.

### Paso 9: Registrar inasistencias

**Que se esta haciendo:** se registra cuando el duenio y la mascota no asisten a una cita programada o confirmada.

**Archivos principales:**

```text
persistence/entity/Inasistencia.java
persistence/enums/EstadoCita.java
domain/repository/InasistenciaRepository.java
domain/dto/request/InasistenciaRequest.java
domain/dto/response/InasistenciaResponse.java
domain/service/InasistenciaService.java
web/InasistenciaController.java
```

**Entidad creada:**

#### `Inasistencia`

Representa el registro formal de una cita a la que el duenio y la mascota no asistieron.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador de la inasistencia. |
| `cita` | `Cita` | Cita marcada como no asistida. Es unica por cita. |
| `duenio` | `Duenio` | Duenio asociado a la cita. |
| `mascota` | `Mascota` | Mascota asociada a la cita. |
| `observacion` | `String` | Motivo o detalle registrado por el personal. |
| `registradoPor` | `String` | Usuario autenticado que registra la inasistencia. |
| `fechaRegistro` | `LocalDateTime` | Fecha y hora del registro. |

**Actividades implementadas:**

- Crear entidad `Inasistencia`.
- Relacionar inasistencia con `Cita`, `Duenio` y `Mascota`.
- Cambiar estado de cita a `NO_ASISTIO`.
- Guardar fecha, observacion y usuario que registra.
- Permitir reportes de inasistencias por duenio o por rango de fechas.
- Evitar duplicar inasistencia para una misma cita.
- Bloquear inasistencias antes de la fecha y hora de la cita.

**Reglas de negocio:**

- Solo se puede registrar inasistencia para citas `PROGRAMADA` o `CONFIRMADA`.
- No se puede registrar inasistencia para citas futuras.
- Una cita solo puede tener una inasistencia.
- Al registrar inasistencia, la cita cambia a estado `NO_ASISTIO`.
- Al registrar inasistencia, la cita deja de requerir confirmacion.
- El usuario autenticado queda guardado en `registradoPor`.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `PATCH` | `/api/citas/{id}/inasistencia` | Registrar inasistencia de una cita. |
| `GET` | `/api/inasistencias` | Listar inasistencias con filtros. |
| `GET` | `/api/inasistencias/{id}` | Obtener detalle de una inasistencia. |

**Ejemplo de request para registrar inasistencia:**

```json
{
  "observacion": "El duenio no se presento a la cita programada."
}
```

**Filtros disponibles en `GET /api/inasistencias`:**

| Parametro | Ejemplo | Descripcion |
| --- | --- | --- |
| `duenioId` | `1` | Filtra por duenio. |
| `fechaInicio` | `2026-05-01` | Fecha inicial de registro. |
| `fechaFin` | `2026-05-31` | Fecha final de registro. |

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `PATCH /api/citas/{id}/inasistencia` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/inasistencias` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/inasistencias/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Resultado esperado:**

- El sistema mantiene trazabilidad de ausencias.
- Se puede identificar duenios con inasistencias recurrentes.

### Paso 10: Registrar atencion, diagnosticos y observaciones clinicas

**Que se esta haciendo:** el veterinario documenta la atencion medica durante o despues de la cita.

**Archivos principales:**

```text
persistence/entity/AtencionClinica.java
domain/repository/AtencionClinicaRepository.java
domain/dto/request/AtencionClinicaRequest.java
domain/dto/response/AtencionClinicaResponse.java
domain/dto/response/HistoriaClinicaResponse.java
domain/service/AtencionClinicaService.java
web/AtencionClinicaController.java
```

**Entidad creada:**

#### `AtencionClinica`

Representa el registro medico generado cuando una cita es atendida.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador de la atencion. |
| `cita` | `Cita` | Cita atendida. Es unica por cita. |
| `mascota` | `Mascota` | Mascota atendida. |
| `veterinario` | `Veterinario` | Veterinario responsable. |
| `motivo` | `String` | Motivo clinico de la atencion. |
| `diagnostico` | `String` | Diagnostico registrado. |
| `tratamiento` | `String` | Tratamiento indicado. |
| `recomendaciones` | `String` | Recomendaciones para el duenio. |
| `observacionesClinicas` | `String` | Observaciones medicas adicionales. |
| `notasInternas` | `String` | Notas internas visibles para el equipo. |
| `fechaRegistro` | `LocalDateTime` | Fecha y hora del registro clinico. |

**Actividades implementadas:**

- Crear entidad `AtencionClinica`.
- Relacionar atencion con cita e historia clinica.
- Registrar motivo, diagnostico, tratamiento, recomendaciones y observaciones clinicas.
- Cambiar estado de cita a `ATENDIDA`.
- Permitir adjuntar notas internas si se requiere.
- Evitar duplicar atenciones para una misma cita.
- Consultar historia clinica completa por mascota.

**Reglas de negocio:**

- Solo se puede registrar atencion para citas `PROGRAMADA` o `CONFIRMADA`.
- No se puede registrar atencion antes de la fecha y hora de la cita.
- Una cita solo puede tener una atencion clinica.
- Al registrar atencion, la cita cambia a `ATENDIDA`.
- Al registrar atencion, la cita deja de requerir confirmacion.
- La historia clinica se arma desde atenciones clinicas y controles mensuales de la mascota.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/citas/{id}/atencion` | Registrar atencion clinica. |
| `GET` | `/api/mascotas/{id}/historia-clinica` | Consultar historia clinica completa. |
| `GET` | `/api/atenciones/{id}` | Consultar detalle de una atencion. |

**Ejemplo de request para registrar atencion:**

```json
{
  "motivo": "Vomitos y falta de apetito",
  "diagnostico": "Gastritis leve",
  "tratamiento": "Dieta blanda y medicacion por 3 dias",
  "recomendaciones": "Retornar si los sintomas continuan",
  "observacionesClinicas": "Paciente estable",
  "notasInternas": "Seguimiento telefonico recomendado"
}
```

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `POST /api/citas/{id}/atencion` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/mascotas/{id}/historia-clinica` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/atenciones/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Resultado esperado:**

- Cada cita atendida genera informacion clinica.
- La historia clinica queda actualizada con diagnosticos, tratamientos y observaciones.

### Paso 11: Control mensual de la mascota

**Que se esta haciendo:** se registra el seguimiento periodico de la mascota para evaluar su evolucion.

**Archivos principales:**

```text
persistence/entity/ControlMensualMascota.java
domain/repository/ControlMensualMascotaRepository.java
domain/dto/request/ControlMensualMascotaRequest.java
domain/dto/response/ControlMensualMascotaResponse.java
domain/service/ControlMensualMascotaService.java
web/ControlMensualMascotaController.java
```

**Entidad creada:**

#### `ControlMensualMascota`

Representa el seguimiento periodico de evolucion de una mascota.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador del control mensual. |
| `mascota` | `Mascota` | Mascota controlada. |
| `veterinario` | `Veterinario` | Veterinario que registra el control. |
| `fechaControl` | `LocalDate` | Fecha del control. |
| `anio` | `Integer` | Anio del control. |
| `mes` | `Integer` | Mes del control. |
| `pesoKg` | `BigDecimal` | Peso registrado. |
| `alimentacion` | `String` | Observacion sobre alimentacion. |
| `observaciones` | `String` | Observaciones generales. |
| `recomendaciones` | `String` | Recomendaciones del veterinario. |
| `createdAt` | `LocalDateTime` | Fecha de creacion. |
| `updatedAt` | `LocalDateTime` | Fecha de ultima actualizacion. |

**Actividades implementadas:**

- Crear entidad `ControlMensualMascota`.
- Registrar mes, anio, peso, alimentacion, recomendaciones y observaciones.
- Relacionar control con mascota y veterinario.
- Permitir consultar controles por mascota.
- Validar un control por mascota por mes, salvo que el negocio permita varios.
- Permitir actualizar un control mensual.

**Reglas de negocio:**

- La mascota debe estar activa.
- El veterinario debe estar activo.
- No se permite registrar mas de un control para la misma mascota en el mismo mes y anio.
- La fecha del control no puede ser futura.
- Los controles mensuales aparecen dentro de la historia clinica de la mascota.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/mascotas/{id}/controles-mensuales` | Registrar control mensual. |
| `GET` | `/api/mascotas/{id}/controles-mensuales` | Listar controles mensuales. |
| `GET` | `/api/controles-mensuales/{id}` | Consultar detalle de control mensual. |
| `PUT` | `/api/controles-mensuales/{id}` | Actualizar control mensual. |

**Ejemplo de request para control mensual:**

```json
{
  "veterinarioId": 1,
  "fechaControl": "2026-05-20",
  "pesoKg": 13.10,
  "alimentacion": "Alimentacion balanceada",
  "observaciones": "Buen estado general",
  "recomendaciones": "Mantener rutina de actividad"
}
```

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `POST /api/mascotas/{id}/controles-mensuales` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/mascotas/{id}/controles-mensuales` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/controles-mensuales/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/controles-mensuales/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Resultado esperado:**

- La veterinaria puede hacer seguimiento mensual.
- El veterinario puede visualizar evolucion de peso, condicion y observaciones.

### Paso 12: Gestionar vacunas y proxima dosis

**Que se esta haciendo:** se registra la aplicacion de vacunas y se calcula la proxima dosis para generar alertas.

**Archivos principales:**

```text
persistence/entity/Vacuna.java
persistence/entity/VacunaMascota.java
domain/repository/VacunaRepository.java
domain/repository/VacunaMascotaRepository.java
domain/dto/request/VacunaRequest.java
domain/dto/request/VacunaMascotaRequest.java
domain/dto/response/VacunaResponse.java
domain/dto/response/VacunaMascotaResponse.java
domain/service/VacunaService.java
web/VacunaController.java
```

**Entidades creadas:**

#### `Vacuna`

Representa el catalogo de vacunas que la veterinaria puede aplicar.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador de la vacuna. |
| `nombre` | `String` | Nombre unico de la vacuna. |
| `descripcion` | `String` | Descripcion o uso de la vacuna. |
| `intervaloProximaDosisDias` | `Integer` | Dias para calcular la siguiente dosis automaticamente. |
| `active` | `Boolean` | Permite activar o desactivar la vacuna. |
| `createdAt` | `LocalDateTime` | Fecha de creacion. |
| `updatedAt` | `LocalDateTime` | Fecha de ultima actualizacion. |

#### `VacunaMascota`

Representa una vacuna aplicada a una mascota.

| Campo | Tipo | Descripcion |
| --- | --- | --- |
| `id` | `Long` | Identificador del registro. |
| `mascota` | `Mascota` | Mascota vacunada. |
| `vacuna` | `Vacuna` | Vacuna aplicada. |
| `veterinario` | `Veterinario` | Veterinario responsable. |
| `cita` | `Cita` | Cita relacionada, si aplica. |
| `fechaAplicacion` | `LocalDate` | Fecha de aplicacion. |
| `lote` | `String` | Lote de la vacuna, si aplica. |
| `fechaProximaDosis` | `LocalDate` | Fecha de la siguiente dosis. |
| `observaciones` | `String` | Observaciones del registro. |
| `createdAt` | `LocalDateTime` | Fecha de creacion del registro. |

**Actividades implementadas:**

- Crear entidad `Vacuna`.
- Crear entidad `VacunaMascota`.
- Registrar vacuna aplicada, fecha, lote si aplica, veterinario y cita relacionada.
- Registrar o calcular `fechaProximaDosis`.
- Marcar vacunas proximas a vencer.
- Generar alerta cuando falten 30 dias o menos para la siguiente dosis.
- Administrar catalogo de vacunas.
- Listar historial de vacunas por mascota.
- Consultar proximas dosis por ventana configurable.

**Reglas de negocio:**

- Solo `ADMIN` puede crear, editar, activar o desactivar vacunas del catalogo.
- `ADMIN`, `ASISTENTE` y `VETERINARIO` pueden consultar catalogo e historial.
- La mascota, vacuna y veterinario deben estar activos para registrar una aplicacion.
- Si se informa `citaId`, la cita debe pertenecer a la misma mascota y al mismo veterinario.
- Si no se envia `fechaProximaDosis`, se calcula con `fechaAplicacion + intervaloProximaDosisDias`.
- Si la vacuna no tiene intervalo configurado y no se envia proxima dosis, el registro queda como `SIN_PROXIMA_DOSIS`.
- La proxima dosis manual debe ser posterior a la fecha de aplicacion.
- `GET /api/alertas/vacunas` usa 30 dias por defecto.

**Estados de alerta calculados:**

| Estado | Descripcion |
| --- | --- |
| `SIN_PROXIMA_DOSIS` | El registro no tiene fecha de siguiente dosis. |
| `VENCIDA` | La proxima dosis ya paso. |
| `PROXIMA` | La proxima dosis vence en 30 dias o menos. |
| `PROGRAMADA` | La proxima dosis esta fuera de la ventana de alerta. |

**Endpoints implementados:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/vacunas` | Crear vacuna en el catalogo. |
| `GET` | `/api/vacunas` | Listar vacunas activas por defecto. Permite `search` y `active`. |
| `GET` | `/api/vacunas/{id}` | Obtener detalle de vacuna. |
| `PUT` | `/api/vacunas/{id}` | Actualizar vacuna. |
| `PATCH` | `/api/vacunas/{id}/activar` | Reactivar vacuna. |
| `DELETE` | `/api/vacunas/{id}` | Desactivar vacuna. |
| `POST` | `/api/mascotas/{id}/vacunas` | Registrar vacuna aplicada. |
| `GET` | `/api/mascotas/{id}/vacunas` | Listar vacunas de una mascota. |
| `GET` | `/api/vacunas/proximas` | Consultar proximas dosis. Permite `dias`. |
| `GET` | `/api/alertas/vacunas` | Consultar alertas de vacunas. Permite `dias`. |

**Ejemplo de request para crear vacuna:**

```json
{
  "nombre": "Rabia",
  "descripcion": "Proteccion antirrabica anual.",
  "intervaloProximaDosisDias": 365
}
```

**Ejemplo de request para registrar vacuna aplicada:**

```json
{
  "vacunaId": 1,
  "veterinarioId": 1,
  "citaId": 1,
  "fechaAplicacion": "2026-05-20",
  "lote": "LOTE-001",
  "fechaProximaDosis": null,
  "observaciones": "Primera dosis aplicada sin reacciones."
}
```

**Ejemplos de consulta:**

```text
GET /api/vacunas/proximas
GET /api/vacunas/proximas?dias=60
GET /api/alertas/vacunas
GET /api/alertas/vacunas?dias=30
```

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `POST /api/vacunas` | `ADMIN` |
| `GET /api/vacunas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/vacunas/{id}` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `PUT /api/vacunas/{id}` | `ADMIN` |
| `PATCH /api/vacunas/{id}/activar` | `ADMIN` |
| `DELETE /api/vacunas/{id}` | `ADMIN` |
| `POST /api/mascotas/{id}/vacunas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/mascotas/{id}/vacunas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/vacunas/proximas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET /api/alertas/vacunas` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Resultado esperado:**

- La mascota tiene historial de vacunas.
- El sistema conoce la proxima dosis.
- Se generan alertas de vacuna antes del vencimiento.

### Paso 13: Crear panel de alertas del dia

**Que se esta haciendo:** se concentra la informacion operativa importante para el personal de la veterinaria.

**Archivos principales:**

```text
domain/dto/response/AlertaCitaResponse.java
domain/dto/response/AlertaVacunaResponse.java
domain/dto/response/ControlMensualPendienteResponse.java
domain/dto/response/PanelAlertasDiaResponse.java
domain/service/AlertaService.java
web/AlertaController.java
```

**Alertas implementadas:**

- Citas programadas para hoy.
- Citas sin confirmar.
- Citas confirmadas pendientes de atencion.
- Citas marcadas como no asistidas.
- Vacunas con proxima dosis en 30 dias o menos.
- Vacunas vencidas.
- Controles mensuales pendientes.

**Reglas de negocio:**

- El panel usa la fecha actual por defecto.
- Se puede consultar otra fecha con el parametro `fecha`.
- Las alertas de vacunas usan una ventana de 30 dias por defecto.
- Se puede cambiar la ventana de vacunas con `diasVacunas`.
- Las citas programadas del dia incluyen estados `PROGRAMADA` y `CONFIRMADA`.
- Las citas sin confirmar son citas `PROGRAMADA` con `requiereConfirmacion = true`.
- Las citas pendientes de atencion son citas `CONFIRMADA`.
- Las citas no asistidas son citas en estado `NO_ASISTIO`.
- Las vacunas vencidas son registros con `fechaProximaDosis` menor a la fecha actual.
- Las vacunas proximas son registros con `fechaProximaDosis` entre hoy y la ventana configurada.
- Los controles mensuales pendientes son mascotas activas sin control registrado en el mes y anio del panel.

**Endpoint implementado:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/alertas/dia` | Devuelve resumen operativo del dia. |

**Parametros disponibles:**

| Parametro | Ejemplo | Descripcion |
| --- | --- | --- |
| `fecha` | `2026-05-22` | Fecha del panel operativo. |
| `diasVacunas` | `30` | Ventana para vacunas proximas o vencidas. |

**Ejemplos de consulta:**

```text
GET /api/alertas/dia
GET /api/alertas/dia?fecha=2026-05-22
GET /api/alertas/dia?fecha=2026-05-22&diasVacunas=45
```

**Respuesta esperada:**

El endpoint devuelve contadores y listas para:

- `citasProgramadasHoy`
- `citasSinConfirmar`
- `citasConfirmadasPendientesAtencion`
- `citasNoAsistidasHoy`
- `vacunasProximas`
- `vacunasVencidas`
- `controlesMensualesPendientes`

**Permisos:**

| Endpoint | Roles permitidos |
| --- | --- |
| `GET /api/alertas/dia` | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Resultado esperado:**

- El frontend puede construir un dashboard operativo.
- El personal ve prioridades del dia sin revisar modulo por modulo.

### Paso 14: Reportes y consultas

**Que se esta haciendo:** se agregan consultas utiles para seguimiento administrativo y clinico.

**Consultas implementadas:**

- Citas por estado y rango de fechas.
- Citas por veterinario.
- Citas por mascota o duenio.
- Inasistencias por duenio.
- Vacunas proximas por fecha.
- Costo total por cita.
- Servicios mas solicitados.
- Historial clinico completo por mascota.

**Archivos principales:**

- `ReporteController`: expone los endpoints de consulta.
- `ReporteService`: centraliza la logica de reportes.
- `ReporteCitaResponse`: respuesta resumida para citas.
- `ReporteCostoCitaResponse`: total, subtotal, descuento y detalle de costos de una cita.
- `ServicioSolicitadoResponse`: resumen de servicios mas solicitados.
- `CitaRepository`: consulta dinamica de citas por filtros.
- `DetalleCostoCitaRepository`: consulta agrupada para servicios mas solicitados.

**Reglas de consulta:**

- Los filtros de fecha usan formato `yyyy-MM-dd`.
- Si se envia `fechaInicio` y `fechaFin`, `fechaFin` no puede ser menor que `fechaInicio`.
- El reporte de citas permite combinar filtros de estado, duenio, mascota, veterinario y rango de fechas.
- El reporte de vacunas proximas usa por defecto desde hoy hasta 30 dias despues si no se envian fechas.
- El costo total de una cita se obtiene desde los valores calculados en la cita y sus detalles de servicios.
- Los servicios mas solicitados se agrupan por nombre de servicio, sumando cantidad y monto generado.
- El historial clinico por mascota reutiliza las atenciones clinicas registradas en orden cronologico.

**Endpoints implementados:**

| Metodo | Ruta | Descripcion | Roles |
| --- | --- | --- | --- |
| `GET` | `/api/reportes/citas` | Lista citas por estado, fechas, veterinario, mascota o duenio. | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET` | `/api/reportes/inasistencias` | Lista inasistencias por duenio y rango de fechas. | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET` | `/api/reportes/vacunas-proximas` | Lista vacunas con proxima dosis dentro de un rango. | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET` | `/api/reportes/citas/{id}/costos` | Devuelve subtotal, descuento, total y detalle de servicios de una cita. | `ADMIN`, `ASISTENTE`, `VETERINARIO` |
| `GET` | `/api/reportes/servicios-mas-solicitados` | Devuelve servicios agrupados por cantidad solicitada y monto generado. | `ADMIN`, `ASISTENTE` |
| `GET` | `/api/reportes/mascotas/{id}/historia-clinica` | Devuelve el historial clinico completo de una mascota. | `ADMIN`, `ASISTENTE`, `VETERINARIO` |

**Parametros sugeridos:**

| Endpoint | Parametros |
| --- | --- |
| `/api/reportes/citas` | `estado`, `fechaInicio`, `fechaFin`, `veterinarioId`, `mascotaId`, `duenioId` |
| `/api/reportes/inasistencias` | `duenioId`, `fechaInicio`, `fechaFin` |
| `/api/reportes/vacunas-proximas` | `fechaInicio`, `fechaFin` |
| `/api/reportes/servicios-mas-solicitados` | `fechaInicio`, `fechaFin` |

**Resultado esperado:**

- El sistema permite tomar decisiones con informacion centralizada.
- Se mejora la organizacion interna de la veterinaria.
- El personal puede consultar reportes desde Swagger o desde el frontend sin revisar modulo por modulo.

## 9. Requerimientos funcionales consolidados

| Modulo | Requerimiento funcional | Backend |
| --- | --- | --- |
| Autenticacion | El sistema debe permitir iniciar sesion con JWT. | Login, generacion y validacion de token. |
| Seguridad | El sistema debe controlar acceso por roles. | Spring Security, roles y permisos. |
| Duenios | Registrar, consultar, editar y desactivar duenios. | CRUD con JPA. |
| Mascotas | Registrar y listar mascotas por duenio. | Relacion Duenio-Mascota. |
| Veterinarios | Registrar veterinarios y consultar disponibilidad. | Agenda, horarios y validacion de cruces. |
| Citas | Programar, confirmar, reprogramar y cancelar citas. | Estados de cita y validacion de horarios. |
| Inasistencias | Registrar ausencias a citas. | Estado `NO_ASISTIO` y entidad `Inasistencia`. |
| Historia clinica | Consultar historial por mascota. | Consultas cronologicas desde atenciones. |
| Observaciones clinicas | Registrar diagnosticos, tratamientos y observaciones. | Entidad `AtencionClinica`. |
| Control mensual | Registrar control mensual de mascota. | Entidad `ControlMensualMascota`. |
| Vacunas | Registrar vacunas y proxima dosis. | Entidad `VacunaMascota`. |
| Alertas de vacuna | Notificar vacunas proximas o vencidas. | Logica automatica de alertas. |
| Servicios | Gestionar servicios veterinarios y costos. | CRUD de servicios. |
| Costos | Calcular costo total por cita. | Detalle de servicios y total acumulado. |
| Panel de alertas | Mostrar alertas del dia. | Endpoint consolidado de alertas. |

## 10. Requerimientos no funcionales

| Requerimiento | Aplicacion en backend |
| --- | --- |
| Seguridad | JWT, roles, contrasenas encriptadas y endpoints protegidos. |
| Escalabilidad | Arquitectura por capas y servicios independientes. |
| Mantenibilidad | DTO, services, repositories y entidades separadas. |
| Integridad de datos | Relaciones JPA, validaciones y restricciones en base de datos. |
| Disponibilidad | API REST preparada para despliegue web. |
| Trazabilidad | Registro de estados, fechas y usuarios responsables. |
| Rendimiento | Consultas paginadas y filtros por fecha, estado y usuario. |
| Testabilidad | Tests unitarios e integracion con perfil `test`. |

## 11. Reglas de negocio principales

- Un duenio puede tener varias mascotas.
- Una mascota pertenece a un solo duenio.
- Una mascota puede tener muchas citas, vacunas, controles mensuales y atenciones clinicas.
- Una cita debe tener una mascota, un duenio, un veterinario, fecha, hora y estado.
- No se puede programar una cita si el veterinario no tiene disponibilidad.
- No se puede programar una cita en una fecha pasada.
- Una cita debe confirmarse antes de la fecha programada si el flujo de negocio lo exige.
- Una cita no atendida puede marcarse como `NO_ASISTIO`.
- Una cita atendida debe generar o actualizar la historia clinica de la mascota.
- El costo total de una cita se calcula segun los servicios asociados.
- Una vacuna aplicada puede generar una proxima dosis.
- Si la proxima dosis vence en 30 dias o menos, debe aparecer en alertas.
- El control mensual debe permitir seguimiento historico de la mascota.

## 12. Orden recomendado de implementacion

1. Base del proyecto, configuracion y health check.
2. Seguridad JWT, usuarios y roles.
3. CRUD de duenios.
4. CRUD de mascotas.
5. CRUD de veterinarios y disponibilidad.
6. CRUD de servicios y costos.
7. Programacion de citas.
8. Confirmacion, cancelacion, reprogramacion e inasistencia.
9. Atencion clinica e historia clinica.
10. Control mensual de mascotas.
11. Vacunas, proxima dosis y alertas.
12. Panel de alertas del dia.
13. Reportes y filtros avanzados.
14. Pruebas, validaciones y documentacion de API.

## 13. Estructura sugerida de endpoints

```text
/api/auth
/api/usuarios
/api/roles
/api/duenios
/api/mascotas
/api/veterinarios
/api/servicios
/api/citas
/api/historias-clinicas
/api/atenciones
/api/vacunas
/api/controles-mensuales
/api/alertas
```

## 14. Criterios de aceptacion generales

- El backend compila con Java 21.
- Las dependencias se gestionan con Maven.
- Las rutas privadas requieren JWT.
- Los permisos se validan segun rol.
- Las entidades principales tienen CRUD o endpoints de consulta.
- Las citas validan disponibilidad del veterinario.
- Las citas permiten confirmacion previa.
- Las inasistencias quedan registradas.
- La atencion clinica guarda diagnostico, tratamiento y observaciones.
- Las vacunas registran proxima dosis.
- Las alertas del dia muestran citas y vacunas pendientes.
- El costo total de la cita se calcula desde los servicios asociados.
- Los tests principales pasan en perfil `test`.

## 15. Resultado final esperado

Al completar esta guia, PetCare tendra un backend modular y mantenible, capaz de centralizar la informacion clinica y administrativa de la veterinaria. El sistema reducira errores de registro, mejorara la gestion de citas, permitira seguimiento clinico completo por mascota y generara alertas preventivas para vacunas, controles y atenciones del dia.
