# PetCare Backend

Backend del sistema PetCare, una API REST para gestión veterinaria: usuarios, dueños, mascotas, veterinarios, asistentes, servicios, citas, atenciones clínicas, vacunas, inasistencias, alertas, reportes y notificaciones.

## Tecnologías

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC + Spring Security
- JWT con JJWT
- Spring Data JPA + Hibernate 7
- PostgreSQL (AWS RDS)
- H2 para pruebas
- Maven + Lombok
- Bean Validation
- Gmail SMTP (envío de correos)
- SpringDoc OpenAPI / Swagger UI
- JUnit 5, Mockito y AssertJ
- JaCoCo para cobertura
- AWS Elastic Beanstalk (deploy backend)
- AWS S3 + CloudFront (deploy frontend)
- AWS RDS PostgreSQL 16 (base de datos)

## Deploy

### AWS (Producción actual)

- **Backend API**: `https://d1eq863qpgnni5.cloudfront.net/api`
- **Frontend Web**: `https://d29ubrdgpv6m22.cloudfront.net`
- **Elastic Beanstalk**: `http://petcare-backend-prod.eba-238cqfih.us-east-1.elasticbeanstalk.com`
- **Base de Datos**: RDS PostgreSQL 16 (`petcare-db.ccteeoyow1ii.us-east-1.rds.amazonaws.com`)
- **CloudFront (Frontend)**: `d29ubrdgpv6m22.cloudfront.net`
- **CloudFront (Backend)**: `d1eq863qpgnni5.cloudfront.net`

### Render (Legacy)

- **Backend**: `https://petcare-backend-o9go.onrender.com`
- **Swagger**: `https://petcare-backend-o9go.onrender.com/swagger-ui.html`

## Estructura del proyecto

```text
src/main/java/com/petcare/backend
|-- config        CORS, seguridad, OpenAPI, async
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
- PostgreSQL (AWS RDS en producción)

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
| `SERVER_PORT` | Puerto del servidor (Render: 10000, AWS: 5000) |
| `JPA_DDL_AUTO` | Modo Hibernate (update/validate/create) |
| `MAIL_ENABLED` | Habilitar envío de correos (true/false) |

### Configuración local

```bash
export DB_URL=jdbc:postgresql://localhost:5432/petcare
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=UGV0Q2FyZS1EZXZlbG9wbWVudC1KMFQtU2VjcmV0
export FRONTEND_URL=http://localhost:4200
export MAIL_USERNAME=tcovenas456@gmail.com
export MAIL_PASSWORD=xufv oldj fpqx hlzp
export MAIL_ENABLED=true
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
- `prueba`: desarrollo diario (desplegada en AWS Elastic Beanstalk)

## Frontend

- Web (AWS CloudFront): `https://d29ubrdgpv6m22.cloudfront.net`
- Web (GitHub Pages): `https://pabloyman-01.github.io/PetCare-Frontend/`
- APK Android: se genera con Capacitor desde el frontend
- iOS: proyecto Xcode en `ios/`
