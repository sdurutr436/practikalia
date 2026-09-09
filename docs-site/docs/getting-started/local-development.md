---
sidebar_position: 3
---

# Desarrollo local

Para trabajar en el código sin reconstruir imágenes Docker en cada cambio, backend y frontend se ejecutan por separado.

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

Necesita una base de datos accesible (ver [variables de entorno](environment-variables.md)). Los tests, en cambio, no: corren sobre H2 en memoria — ver [Testing](../development/testing.md).

## Frontend

```bash
cd frontend
pnpm install
pnpm start
```

Sirve la aplicación con recarga en caliente en `http://localhost:4200`. `frontend/proxy.conf.json` reenvía las peticiones a `/api` hacia `http://localhost:8080` (el backend local), sin necesidad de nginx.
