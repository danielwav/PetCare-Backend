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

## 4. Arquitectura propuesta

El backend se organiza por capas para separar responsabilidades y facilitar mantenimiento.

```text
src/main/java/com/petcare/backend
+-- config
|   +-- Configuraciones generales: seguridad, CORS, beans.
+-- security
|   +-- JWT, filtros, autenticacion y control de acceso.
+-- domain
|   +-- dto
|   +-- service
|   +-- repository
+-- persistence
|   +-- entity
+-- web
|   +-- Controladores REST.
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

## 5. Roles del sistema

Los roles deben quedar definidos desde los requerimientos funcionales porque condicionan que acciones puede ejecutar cada usuario.

| Rol | Permisos principales |
| --- | --- |
| `ADMIN` | Gestionar usuarios, roles, servicios, veterinarios, costos y configuraciones generales. |
| `VETERINARIO` | Consultar agenda, atender citas, registrar diagnosticos, tratamientos, observaciones clinicas, vacunas y controles mensuales. |
| `ASISTENTE` | Registrar duenios, mascotas, programar citas, confirmar citas, cancelar citas y consultar alertas del dia. |
| `DUENIO` | Consultar sus mascotas, citas, vacunas pendientes y estado de atenciones permitidas. |

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

**Resultado esperado:**

- El backend compila correctamente.
- El endpoint de salud responde.
- La aplicacion puede conectarse a MySQL.

### Paso 2: Configurar seguridad y JWT

**Que se esta haciendo:** se protege la API para que solo usuarios autenticados puedan acceder a los modulos internos.

**Actividades:**

- Crear entidad `Usuario`.
- Crear entidad `Rol`.
- Implementar registro e inicio de sesion.
- Encriptar contrasenas con `BCryptPasswordEncoder`.
- Generar JWT al iniciar sesion.
- Validar JWT en cada request.
- Configurar rutas publicas y privadas.
- Aplicar autorizacion por roles.

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

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/duenios` | Crear duenio. |
| `GET` | `/api/duenios` | Listar duenios con filtros. |
| `GET` | `/api/duenios/{id}` | Obtener detalle de duenio. |
| `PUT` | `/api/duenios/{id}` | Actualizar duenio. |
| `DELETE` | `/api/duenios/{id}` | Desactivar duenio. |

**Resultado esperado:**

- La veterinaria puede registrar, buscar y editar duenios.
- Cada duenio puede tener una o varias mascotas.

### Paso 4: Gestionar mascotas

**Que se esta haciendo:** se registra la informacion clinica base de cada mascota asociada a un duenio.

**Actividades:**

- Crear entidad `Mascota`.
- Relacionar `Mascota` con `Dueno`.
- Registrar especie, raza, sexo, fecha de nacimiento y datos basicos.
- Listar mascotas por duenio.
- Preparar relacion con historia clinica, citas, vacunas y controles mensuales.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/mascotas` | Crear mascota. |
| `GET` | `/api/mascotas` | Listar mascotas. |
| `GET` | `/api/duenios/{id}/mascotas` | Listar mascotas por duenio. |
| `GET` | `/api/mascotas/{id}` | Obtener detalle de mascota. |
| `PUT` | `/api/mascotas/{id}` | Actualizar mascota. |

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

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/veterinarios` | Crear veterinario. |
| `GET` | `/api/veterinarios` | Listar veterinarios. |
| `GET` | `/api/veterinarios/{id}/disponibilidad` | Consultar disponibilidad. |
| `PUT` | `/api/veterinarios/{id}` | Actualizar veterinario. |

**Resultado esperado:**

- El frontend puede mostrar horarios disponibles.
- El backend evita programar citas en horarios ocupados.

### Paso 6: Gestionar servicios y costos

**Que se esta haciendo:** se define el catalogo de servicios veterinarios y sus costos para calcular el costo total de cada cita.

**Actividades:**

- Crear entidad `Servicio`.
- Registrar nombre, descripcion, costo base y estado.
- Permitir activar o desactivar servicios.
- Crear entidad `DetalleCostoCita`.
- Asociar uno o varios servicios a una cita.
- Calcular subtotal, descuentos si aplican y costo total.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/servicios` | Crear servicio. |
| `GET` | `/api/servicios` | Listar servicios activos. |
| `PUT` | `/api/servicios/{id}` | Actualizar servicio y costo. |
| `DELETE` | `/api/servicios/{id}` | Desactivar servicio. |

**Resultado esperado:**

- Los costos quedan centralizados.
- Cada cita puede calcular su costo total automaticamente.

### Paso 7: Programar citas veterinarias

**Que se esta haciendo:** se implementa el flujo de agendamiento de citas entre mascota, duenio, veterinario y servicio.

**Actividades:**

- Crear entidad `Cita`.
- Relacionar cita con mascota, duenio, veterinario y servicios.
- Validar disponibilidad antes de guardar.
- Validar fecha y hora futuras.
- Calcular costo total al registrar o actualizar servicios.
- Definir estado inicial `PROGRAMADA`.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/citas` | Programar cita. |
| `GET` | `/api/citas` | Listar citas con filtros. |
| `GET` | `/api/citas/{id}` | Obtener detalle de cita. |
| `PUT` | `/api/citas/{id}` | Reprogramar o actualizar cita. |
| `PATCH` | `/api/citas/{id}/cancelar` | Cancelar cita. |

**Resultado esperado:**

- La veterinaria puede registrar citas sin choques de horario.
- Cada cita tiene costo total calculado.
- El historial de la mascota queda preparado para la atencion.

### Paso 8: Confirmar citas antes de la fecha programada

**Que se esta haciendo:** se agrega control previo de asistencia para reducir ausencias y mejorar la organizacion diaria.

**Actividades:**

- Agregar campos `requiereConfirmacion`, `fechaConfirmacion` y `confirmadaPor`.
- Permitir confirmar cita desde el panel del asistente o duenio.
- Generar alerta cuando una cita cercana aun no esta confirmada.
- Definir regla de negocio: por ejemplo, alertar 24 horas antes si sigue `PROGRAMADA`.

**Endpoint sugerido:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `PATCH` | `/api/citas/{id}/confirmar` | Confirmar cita antes de la fecha programada. |

**Resultado esperado:**

- Las citas proximas pueden marcarse como `CONFIRMADA`.
- El panel del dia muestra citas sin confirmar.

### Paso 9: Registrar inasistencias

**Que se esta haciendo:** se registra cuando el duenio y la mascota no asisten a una cita programada o confirmada.

**Actividades:**

- Crear entidad `Inasistencia`.
- Relacionar inasistencia con `Cita`, `Duenio` y `Mascota`.
- Cambiar estado de cita a `NO_ASISTIO`.
- Guardar fecha, observacion y usuario que registra.
- Permitir reportes de inasistencias por duenio o por rango de fechas.

**Endpoint sugerido:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `PATCH` | `/api/citas/{id}/inasistencia` | Registrar inasistencia de una cita. |

**Resultado esperado:**

- El sistema mantiene trazabilidad de ausencias.
- Se puede identificar duenios con inasistencias recurrentes.

### Paso 10: Registrar atencion, diagnosticos y observaciones clinicas

**Que se esta haciendo:** el veterinario documenta la atencion medica durante o despues de la cita.

**Actividades:**

- Crear entidad `AtencionClinica`.
- Relacionar atencion con cita e historia clinica.
- Registrar motivo, diagnostico, tratamiento, recomendaciones y observaciones clinicas.
- Cambiar estado de cita a `ATENDIDA`.
- Permitir adjuntar notas internas si se requiere.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/citas/{id}/atencion` | Registrar atencion clinica. |
| `GET` | `/api/mascotas/{id}/historia-clinica` | Consultar historia clinica completa. |
| `GET` | `/api/atenciones/{id}` | Consultar detalle de una atencion. |

**Resultado esperado:**

- Cada cita atendida genera informacion clinica.

- Relacionar control con mascota y veterinario.
- Permitir consultar controles por mascota.
- Validar un control por mascota por mes, salvo que el negocio permita varios.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/mascotas/{id}/controles-mensuales` | Registrar control mensual. |
| `GET` | `/api/mascotas/{id}/controles-mensuales` | Listar controles mensuales. |
| `PUT` | `/api/controles-mensuales/{id}` | Actualizar control mensual. |

**Resultado esperado:**

- La veterinaria puede hacer seguimiento mensual.
- El veterinario puede visualizar evolucion de peso, condicion y observaciones.

### Paso 12: Gestionar vacunas y proxima dosis

**Que se esta haciendo:** se registra la aplicacion de vacunas y se calcula la proxima dosis para generar alertas.

**Actividades:**

- Crear entidad `Vacuna`.
- Crear entidad `VacunaMascota`.
- Registrar vacuna aplicada, fecha, lote si aplica, veterinario y cita relacionada.
- Registrar o calcular `fechaProximaDosis`.
- Marcar vacunas proximas a vencer.
- Generar alerta cuando falten 30 dias o menos para la siguiente dosis.

**Endpoints sugeridos:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `POST` | `/api/mascotas/{id}/vacunas` | Registrar vacuna aplicada. |
| `GET` | `/api/mascotas/{id}/vacunas` | Listar vacunas de una mascota. |
| `GET` | `/api/vacunas/proximas` | Consultar proximas dosis. |
| `GET` | `/api/alertas/vacunas` | Consultar alertas de vacunas. |

**Resultado esperado:**

- La mascota tiene historial de vacunas.
- El sistema conoce la proxima dosis.
- Se generan alertas de vacuna antes del vencimiento.

### Paso 13: Crear panel de alertas del dia

**Que se esta haciendo:** se concentra la informacion operativa importante para el personal de la veterinaria.

**Alertas recomendadas:**

- Citas programadas para hoy.
- Citas sin confirmar.
- Citas confirmadas pendientes de atencion.
- Citas marcadas como no asistidas.
- Vacunas con proxima dosis en 30 dias o menos.
- Vacunas vencidas.
- Controles mensuales pendientes.

**Endpoint sugerido:**

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/alertas/dia` | Devuelve resumen operativo del dia. |

**Resultado esperado:**

- El frontend puede construir un dashboard operativo.
- El personal ve prioridades del dia sin revisar modulo por modulo.

### Paso 14: Reportes y consultas

**Que se esta haciendo:** se agregan consultas utiles para seguimiento administrativo y clinico.

**Consultas recomendadas:**

- Citas por estado y rango de fechas.
- Citas por veterinario.
- Citas por mascota o duenio.
- Inasistencias por duenio.
- Vacunas proximas por fecha.
- Costo total por cita.
- Servicios mas solicitados.
- Historial clinico completo por mascota.

**Resultado esperado:**

- El sistema permite tomar decisiones con informacion centralizada.
- Se mejora la organizacion interna de la veterinaria.

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
