# PetCare-Backend
Repositorio del Backend del proyecto PetCare

## Stack

- Java 21
- Spring Boot 4
- Spring Security + JWT
- Spring Data JPA
- MySQL
- Maven
- Lombok

## Configuracion local

Por defecto la aplicacion usa MySQL en `localhost:3306/petcare`.


```bash
DB_URL=jdbc:mysql://localhost:3306/petcare
DB_USERNAME=root
DB_PASSWORD=tu_password
JWT_SECRET=base64-secret-de-256-bits-o-mas
```

## Ejecutar

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```http
GET /api/health
```

Respuesta esperada cuando la aplicacion y la base de datos estan disponibles:

```json
{
  "status": "UP",
  "database": "UP"
}
```

Este endpoint esta pensado para monitoreo, balanceadores o plataformas de despliegue.

## Disponibilidad 99%

El backend queda preparado para monitoreo con `GET /api/health`, pero el 99% real se garantiza en infraestructura:

- Ejecutar la aplicacion en un servicio con reinicio automatico.
- Usar una base de datos administrada o respaldada.
- Configurar monitoreo sobre `/api/health`.
- Mantener backups y restauracion probada.
- Separar variables por ambiente: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.

## Swagger / OpenAPI

Con la aplicacion levantada, la documentacion de la API esta disponible en:

```text
http://localhost:8080/swagger-ui.html
```

La especificacion OpenAPI se expone en:

```text
http://localhost:8080/v3/api-docs
```

Para probar endpoints protegidos, primero inicia sesion en `/api/auth/login`, copia el token y usa el boton `Authorize` con formato:

```text
Bearer <token>
```
