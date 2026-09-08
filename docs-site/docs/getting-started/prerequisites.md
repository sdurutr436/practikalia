---
sidebar_position: 1
---

# Requisitos

Software necesario para ejecutar Practikalia en local o construir sus imágenes Docker:

| Herramienta | Versión | Uso |
|---|---|---|
| Java (JDK) | 25 | Backend (Spring Boot) |
| Node.js | 24 | Frontend (Angular) |
| pnpm | 10.4.1 (fijado en `frontend/package.json`) | Gestor de paquetes del frontend — no usar `npm`/`yarn` |
| Docker y Docker Compose | reciente | Orquestar `postgres` + `backend` + `frontend` + `nginx` |
| PostgreSQL | 17 | Solo si no usas Docker Compose para la base de datos |

:::tip
No hace falta instalar Maven ni el CLI de Angular globalmente: `./mvnw` (en `backend/`) y los scripts de `pnpm` (en `frontend/`) ya resuelven ambos.
:::
