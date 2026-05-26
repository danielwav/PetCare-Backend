# PetCare Backend

Backend del sistema PetCare, una API REST para gestionar operaciones de una veterinaria: usuarios, duenios, mascotas, veterinarios, asistentes, servicios, citas, atenciones clinicas, vacunas, alertas y reportes.

## Tecnologias

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- JWT con JJWT
- Spring Data JPA
- Hibernate
- MySQL
- H2 para pruebas
- Maven
- Lombok
- Bean Validation
- Spring Mail
- SpringDoc OpenAPI / Swagger UI
- JUnit 5, Mockito y AssertJ
- JaCoCo para cobertura

## Que contiene

El proyecto expone una API REST para:

- Autenticacion con login, registro, refresh token y usuario actual.
- Gestion de usuarios internos y roles.
- Gestion de duenios.
- Gestion de mascotas asociadas a duenios.
- Gestion de veterinarios y horarios.
- Gestion de asistentes.
- Catalogo de servicios veterinarios.
- Gestion de citas, confirmacion, cancelacion y costos.
- Registro de atenciones clinicas.
- Registro de inasistencias.
- Catalogo y aplicacion de vacunas.
- Alertas del dia para citas, vacunas y controles pendientes.
- Reportes operativos y clinicos.
- Health check para monitoreo.
- Documentacion interactiva con Swagger.

## Estructura del proyecto

```text
src/main/java/com/petcare/backend
|-- config        Configuracion de CORS, seguridad, OpenAPI y datos iniciales.
|-- domain
|   |-- dto       Requests y responses usados por la API.
|   |-- repository Interfaces de acceso a datos con Spring Data JPA.
|   `-- service   Logica de negocio del sistema.
|-- health        Endpoint de health check.
|-- persistence
|   |-- entity    Entidades JPA.
|   `-- enums     Enumeraciones del dominio.
|-- security      JWT, filtro de autenticacion y UserDetailsService.
`-- web           Controladores REST y manejo de excepciones.
```

Otros archivos importantes:

```text
src/main/resources/application.properties   Configuracion principal.
src/main/resources/schema-mysql.sql         Esquema SQL de referencia para MySQL.
src/test/resources/application-test.properties Configuracion de pruebas con H2.
scripts/backup-mysql.ps1                    Script de backup MySQL.
scripts/seed-data.sql                       Datos SQL de apoyo.
```

## Requisitos

- Java 21
- Maven Wrapper incluido en el repositorio
- MySQL 8 o compatible

Configuracion local por defecto:

```properties
DB_URL=jdbc:mysql://localhost:3306/petcare
DB_USERNAME=root
DB_PASSWORD=admin
```

La aplicacion tambien puede recibir estas variables de entorno:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/petcare?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="admin"
$env:JWT_SECRET="base64-secret-de-256-bits-o-mas"
```

## Ejecutar localmente

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

La API queda disponible en:

```text
http://localhost:8080
```

## Swagger / OpenAPI

Con la aplicacion levantada:

```text
http://localhost:8080/swagger-ui.html
```

Especificacion OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

Para probar endpoints protegidos, inicia sesion en `/api/auth/login`, copia el token y usa `Authorize` con:

```text
Bearer <token>
```

## Health check

```http
GET /api/health
```

Respuesta esperada:

```json
{
  "status": "UP",
  "database": "UP"
}
```

## Pruebas

Ejecutar tests:

```powershell
.\mvnw.cmd test
```

Ejecutar verificacion completa con JaCoCo:

```powershell
.\mvnw.cmd verify
```

El reporte HTML queda en:

```text
target/site/jacoco/index.html
```

La configuracion de pruebas usa H2 y desactiva la carga de datos demo con:

```properties
app.seed-data.enabled=false
```

## Datos iniciales

En ejecucion normal, `DataInitializer` crea roles base y datos demo si `app.seed-data.enabled` esta activo. Por defecto esta activo:

```properties
app.seed-data.enabled=true
```

Usuarios demo principales:

```text
admin@petcare.com / admin123
vet@petcare.com / vet123
asistente@petcare.com / asistente123
duenio@petcare.com / duenio123
supervisor@petcare.com / 123456
```

## Backup MySQL

El repositorio incluye un script para generar backups:

```powershell
.\scripts\backup-mysql.ps1
```

Tambien puede tomar la configuracion desde variables de entorno:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/petcare"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="admin"
.\scripts\backup-mysql.ps1
```

Para cambiar la retencion:

```powershell
.\scripts\backup-mysql.ps1 -RetentionDays 14
```

## Ramas principales

- `main`: rama estable para levantar el backend integrado.
- `dev`: rama de desarrollo e integracion antes de pasar a `main`.

