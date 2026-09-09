---
sidebar_position: 1
---

# Visión general

Practikalia son tres piezas independientes que se orquestan con `docker-compose.yml`:

![Arquitectura de despliegue de Practikalia: el equipo del centro solo habla con frontend en el puerto 80; backend y postgres viven en una red interna sin exposición externa](/img/architecture-overview.svg)

- **`frontend`** es el único servicio que necesita ser alcanzable desde fuera del servidor. Es una imagen de Nginx que ya lleva dentro los estáticos de Angular compilados — no hay build en tiempo de despliegue. Nginx hace de proxy inverso hacia `backend` para todo lo que llega a `/api/`, así que el navegador nunca ve el host ni el puerto reales de la API. Ver [Docker](docker.md).
- **`backend`** es la API de Spring Boot. No se expone directamente en un despliegue real (el puerto 8080 que publica `docker-compose.yml` es solo para depurar en local — ver [Instalación](../getting-started/installation.md)).
- **`postgres`** solo es accesible entre contenedores de la propia red de Docker Compose.

Esta ruta enmascarada (todo pasa por `frontend` en el puerto 80) es deliberada: permite instalar Practikalia en la red de un centro educativo sin tener que configurar CORS ni distribuir la URL real del backend a cada equipo — ver [Despliegue](../operations/deployment.md).

## Backend y frontend, por separado

- [Backend](backend.md): organización del código Java, patrón DTO+Repository.
- [Frontend](frontend.md): organización de componentes Angular, ITCSS + BEM.
