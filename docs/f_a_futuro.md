# A futuro

Decisiones descartadas **por ahora**, no para siempre: cosas con un motivo real
para existir algún día, pero sin caso de uso, dependencia disparadora o
demanda confirmada todavía. Cuando se cumpla el disparador de cada una, se
revisita — no antes.

Distinto de `docs/todo/` (notas locales de trabajo, no se comparten): esto es
documentación del proyecto, se commitea.

---

## Dependencias / infraestructura

- **`spring-boot-starter-mail`** — envío real de la contraseña temporal / OTP por correo. Se añade cuando exista el listener de email de verdad (`EmailContrasenaTemporalListener`), que hoy es un evento (`ContrasenaTemporalGeneradaEvent`) sin nadie escuchando. Ver [docs/todo/u_fase1_usuario_auth.md](todo/u_fase1_usuario_auth.md).
- **MariaDB** como motor de BD alternativo a PostgreSQL. Se revisita si un instituto real lo pide — soportar dos motores implica mantener dialecto y migraciones Flyway para ambos sin demanda hoy.
- **Caffeine (cache)** — sin ningún punto caliente identificado todavía. Candidato natural: el algoritmo de afinidad (Fase 6 del backend).
- **Publicar imágenes en Docker Hub + pipeline de CI** — no tiene sentido hasta que el proyecto llegue a la versión 1.0 (hoy es el scaffold de Angular + Spring Boot).

## Código backend

- **`EtiquetaException` compartida en `practikalia.etiqueta`** — hoy "etiqueta no encontrada" está duplicada por feature a propósito (dos usos no justifican la utilidad común): `EmpresaException.etiquetaNoEncontrada()` (`400`, legado de Fase 2) y `UsuarioException.etiquetaNoEncontrada()` (`404`, Fase 8). Disparador: un **tercer consumidor** del concepto (candidato: Fase 9/Afinidad, si valida ids de etiqueta en un endpoint propio). Al extraerla, el status canónico es **`404 NOT_FOUND`** (decisión cerrada en el addendum post-implementación del prompt de Fase 8: es el status correcto para "el recurso referenciado no existe") y el `400` de Empresa se migra a `404` en esa misma extracción — es un cambio de contrato de los endpoints de empresa, avisarlo en el commit.

## Autenticación

- **OTP / 2FA por correo** — sustituiría o complementaría el login por contraseña actual. Roadmap del briefing, "más adelante".
- **OAuth como opción adicional** — hoy explícitamente "sin OAuth por ahora".

## Producto (roadmap del briefing, sección "Más adelante")

- Importación de datos académicos desde fuentes externas.
- Métricas avanzadas por empresa.
- Dashboard de coordinación.
- Mejoras del motor de afinidad (más allá del matching básico del MVP).
- Federación parcial entre instancias.

---

Al añadir algo nuevo aquí: una línea con el motivo por el que no se hace ya, y si aplica, la condición que dispara revisarlo.
