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

:::danger
Nunca commitees un `.env` con valores reales. El repositorio solo debe contener `.env.example`, con valores de ejemplo.
:::

`docker-compose.yml` consume estas variables para levantar `postgres` y pasarle la cadena de conexión al `backend`. Para desarrollo local sin Docker, defínelas como variables de entorno del sistema o en la configuración de ejecución de tu IDE.
