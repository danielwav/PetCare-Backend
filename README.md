# PetCare Backend

Backend del sistema PetCare, una API REST para gestión veterinaria: usuarios, dueños, mascotas, veterinarios, asistentes, servicios, citas, atenciones clínicas, vacunas, inasistencias, alertas, reportes y notificaciones.

## Tecnologías

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC + Spring Security
- JWT con JJWT
- Spring Data JPA + Hibernate 7
- PostgreSQL (NeonDB)
- H2 para pruebas
- Maven + Lombok
- Bean Validation
- Gmail SMTP (envío de correos)
- Resend API (envío de correos alternativo)
- SpringDoc OpenAPI / Swagger UI
- JUnit 5, Mockito y AssertJ
- JaCoCo para cobertura

## Deploy

- **Render** (Docker): `https://petcare-backend-o9go.onrender.com`
- Rama `dev` se despliega automáticamente en Render
- Swagger: `https://petcare-backend-o9go.onrender.com/swagger-ui.html`
- Health: `https://petcare-backend-o9go.onrender.com/api/health`

## Estructura del proyecto

```text
src/main/java/com/petcare/backend
|-- config        CORS, seguridad, OpenAPI, async, Resend
|-- domain
|   |-- dto       Request y Response DTOs
|   |-- repository Interfaces Spring Data JPA
|   `-- service   Lógica de negocio
|-- health        Health check endpoint
|-- persistence
|   |-- entity    Entidades JPA
|   `-- enums     Enumeraciones (EstadoCita, EstadoMascota, RoleName, SexoMascota)
|-- security      JWT, filtro de autenticación, UserDetailsService
`-- web           Controladores REST
```

## Requisitos

- Java 21
- Maven Wrapper incluido
- PostgreSQL (o NeonDB en la nube)

### Variables de entorno (producción)

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | URL de conexión PostgreSQL |
| `DB_USERNAME` | Usuario de base de datos |
| `DB_PASSWORD` | Contraseña de base de datos |
| `JWT_SECRET` | Secreto para firmar JWT (Base64) |
| `MAIL_USERNAME` | Correo Gmail para envío SMTP |
| `MAIL_PASSWORD` | App password de Gmail |
| `FRONTEND_URL` | URL del frontend para enlaces de activación |
| `SERVER_PORT` | Puerto del servidor (Render asigna 10000) |

### Configuración local

```bash
export DB_URL=jdbc:postgresql://localhost:5432/petcare
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=UGV0Q2FyZS1EZXZlbG9wbWVudC1KMFQtU2VjcmV0
export FRONTEND_URL=http://localhost:4200
```

## Ejecutar localmente

```bash
./mvnw spring-boot:run
```

La API queda en:

```text
http://localhost:8090
```

## Swagger / OpenAPI

Con la app levantada:

```text
http://localhost:8090/swagger-ui.html
```

## Health check

```http
GET /api/health
```

Respuesta:

```json
{
  "status": "UP",
  "database": "UP"
}
```

## Pruebas

```bash
./mvnw test          # Tests unitarios
./mvnw verify        # Tests + cobertura JaCoCo
```

El reporte de cobertura queda en:

```text
target/site/jacoco/index.html
```

## Usuarios demo (seed data)

| Email | Contraseña | Rol |
|-------|-----------|-----|
| admin@petcare.com | admin123 | ADMIN |
| vet@petcare.com | vet123 | VETERINARIO |
| asistente@petcare.com | asistente123 | ASISTENTE |
| duenio@petcare.com | duenio123 | DUEÑO |
| supervisor@petcare.com | 123456 | ADMIN + VETERINARIO |

## Ramas

- `main`: producción estable
- `dev`: integración continua (desplegada en Render)
- `prueba`: desarrollo diario

## Frontend

- Web (GitHub Pages): `https://pabloyman-01.github.io/PetCare-Frontend/`
- APK Android: se genera con Capacitor desde el frontend
- iOS: proyecto Xcode en `ios/`
