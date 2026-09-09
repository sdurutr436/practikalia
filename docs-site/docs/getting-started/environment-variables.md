---
sidebar_position: 4
---

# Variables de entorno

Configuración vía un archivo `.env` en la raíz del repo (no se versiona — copia `.env.example` y ajusta los valores).

| Variable | Descripción |
|---|---|
| `DB_NAME` | Nombre de la base de datos PostgreSQL |
| `DB_USER` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `ALLOWED_DOMAINS` | Dominios de correo institucional admitidos para el auto-registro de alumnado, separados por comas |
| `JWT_SECRET` (`jwt.secret`) | Secreto de firma de los JWT. **No está cableada en `docker-compose.yml` todavía** — el backend arranca con un valor por defecto que solo sirve para desarrollo. |

:::danger
Nunca commitees un `.env` con valores reales. El repositorio solo debe contener `.env.example`, con valores de ejemplo. Antes de exponer una instancia a la red de un centro, fija tu propio `JWT_SECRET` — ver el aviso en [Despliegue](../operations/deployment.md).
:::

`docker-compose.yml` consume `DB_*` y `ALLOWED_DOMAINS` para levantar `postgres` y pasarle la configuración al `backend`. Para desarrollo local sin Docker, defínelas como variables de entorno del sistema o en la configuración de ejecución de tu IDE.
