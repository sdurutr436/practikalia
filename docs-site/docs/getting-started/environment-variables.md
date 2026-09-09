---
sidebar_position: 4
---

# Variables de entorno

Configuración vía un archivo `.env` en la raíz del repo (no se versiona — copia `.env.example` y ajusta los valores).

| Variable | Descripción |
|---|---|
| `DB_NAME` | Nombre de la base de datos PostgreSQL |
| `DB_USER` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL. **Obligatoria** — ni `docker-compose.yml` ni `docker-compose.prod.yml` arrancan sin ella. |
| `JWT_SECRET` (`jwt.secret`) | Secreto de firma de los JWT. **Obligatoria**, mismo motivo. Genera uno propio con `openssl rand -base64 32` — no reutilices el mismo valor entre instalaciones. |
| `ALLOWED_DOMAINS` | Dominios de correo institucional admitidos para el auto-registro de alumnado, separados por comas |

:::danger
Nunca commitees un `.env` con valores reales. El repositorio solo debe contener `.env.example`, con las variables sin valor. `DB_PASSWORD` y `JWT_SECRET` no tienen un valor por defecto a propósito: si falta alguna, `docker compose up` falla con un mensaje explícito en vez de arrancar con un secreto conocido.
:::

`docker-compose.yml` y `docker-compose.prod.yml` (ver [Despliegue](../operations/deployment.md)) consumen las mismas variables. Para desarrollo local sin Docker, defínelas como variables de entorno del sistema o en la configuración de ejecución de tu IDE — sin ellas, el backend arranca igual usando valores de solo-desarrollo definidos en el propio código, nunca expuestos fuera de `localhost`.
