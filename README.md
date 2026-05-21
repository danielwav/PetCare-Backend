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
Puedes cambiar la conexion con variables de entorno:

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
