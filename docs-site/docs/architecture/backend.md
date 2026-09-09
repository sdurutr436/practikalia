---
sidebar_position: 2
---

# Backend

Java 25 + Spring Boot 4.1, con Spring Security, Spring Data JPA, Flyway y JWT (jjwt).

## Paquete por feature, no por capa

El código en `backend/src/main/java/practikalia/` se organiza por dominio (`usuario`, `empresa`, `etiqueta`, `asignacion`, `review`, `interes`, `afinidad`, `centro`, `grado`, `panel`, `common`), no por capa técnica. Dentro de cada paquete de dominio conviven la entidad JPA, su DTO, su repositorio, su servicio y su controlador:

```text
practikalia/usuario/
├── Usuario.java              # entidad JPA
├── UsuarioDto.java           # DTO expuesto por la API
├── UsuarioRepository.java
├── UsuarioService.java
├── UsuarioController.java
├── alumnado/                 # sub-dominio
├── correo/
├── jwt/
└── profesorado/
```

Un dominio grande se subdivide en sub-paquetes (como `usuario/jwt` o `usuario/alumnado`) en vez de crecer con capas transversales (`controllers/`, `services/`, `repositories/`) que dispersarían un mismo concepto de negocio por todo el árbol.

## DTO en todas las capas

Ningún método de servicio invocado desde un controlador acepta o devuelve una entidad JPA directamente: siempre un DTO. Esto evita serializar relaciones lazy de Hibernate por accidente y desacopla el contrato de la API del modelo de persistencia.

## Migraciones (Flyway)

Las migraciones viven en `backend/src/main/resources/db/migration/` (`V1__baseline_etiqueta.sql`, `V2__...`, numeradas secuencialmente) y son comunes a cualquier motor. Lo específico de un motor concreto (hoy, los datos de demostración de PostgreSQL) va en `db/vendor/<motor>/`, fuera de `db/migration` a propósito: Flyway escanea cada ubicación configurada de forma recursiva, así que una subcarpeta de `db/migration` se cargaría igual para todos los motores. Los tests corren sobre H2, que ni entiende la sintaxis de esos scripts de PostgreSQL ni debe ver esas filas de demostración — ver [Testing](../development/testing.md).

## Seguridad

- Login con JWT en cookie, cambio de contraseña obligatorio en el primer acceso.
- Auto-registro de alumnado por correo institucional, pendiente de aprobación docente.
- El secreto de firma del JWT (`jwt.secret`) tiene un valor por defecto **solo válido en desarrollo** — ver el aviso en [Variables de entorno](../getting-started/environment-variables.md).
