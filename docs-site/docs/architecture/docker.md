---
sidebar_position: 4
---

# Docker

Ambos Dockerfiles son **multi-stage**: una etapa compila, la otra solo contiene lo necesario para ejecutar. Ninguna imagen final lleva código fuente, gestor de paquetes ni herramientas de build.

## Backend

```dockerfile title="backend/Dockerfile"
FROM eclipse-temurin:25-jdk AS build   # compila con Maven (mvnw)
# ...
FROM eclipse-temurin:25-jre            # solo el JRE + el jar
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/* \
    && rm -f /usr/bin/pebble
```

La imagen final actualiza los paquetes del sistema en el momento de construirse (no depende de que la imagen base de Eclipse Temurin se haya reconstruido recientemente) y elimina `pebble`, un binario de pruebas ACME que trae la imagen base de Eclipse Temurin y que la aplicación no usa — eliminarlo cierra varias CVEs de golpe sin tocar ninguna dependencia de la aplicación.

## Frontend

```dockerfile title="frontend/Dockerfile"
FROM node:24-alpine AS build   # pnpm install + pnpm build
# ...
FROM nginx:alpine              # sirve los estáticos y hace de proxy a /api
RUN apk update && apk upgrade --no-cache
COPY --from=build /app/dist/practikalia-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

Antes, `frontend/Dockerfile` solo ejecutaba `pnpm build` y no servía nada por sí mismo — un contenedor `nginx` aparte servía los estáticos desde un volumen compartido. Ahora la imagen de frontend **ya lleva Nginx dentro** con los estáticos compilados y su propia configuración (`frontend/nginx.conf`, la misma que antes vivía en la raíz del repo). Esto la convierte en una imagen standalone: un centro puede publicarla, descargarla y levantarla sin necesidad de compilar Angular ni de un contenedor `nginx` adicional — por eso `docker-compose.yml` pasó de cuatro servicios a tres.

## Por qué importa para Trivy

Ambos ajustes (actualizar paquetes del sistema, quitar binarios sin usar) no son cosmética: al introducir el escaneo de seguridad en la Fase de CI/CD, Trivy encontró vulnerabilidades reales en ambas imágenes — ver [Seguridad](../operations/security.md) para el detalle y cómo se resolvieron.
