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
    && rm -f /usr/bin/pebble \
    && groupadd -r practikalia && useradd -r -g practikalia practikalia
# ...
USER practikalia                       # nunca corre como root
```

La imagen final actualiza los paquetes del sistema en el momento de construirse (no depende de que la imagen base de Eclipse Temurin se haya reconstruido recientemente), elimina `pebble` (un binario de pruebas ACME que trae la imagen base y que la aplicación no usa) y ejecuta el `.jar` con un usuario propio sin privilegios, no como root.

## Frontend

```dockerfile title="frontend/Dockerfile"
FROM node:24-alpine AS build   # pnpm install + pnpm build
# ...
FROM nginx:alpine              # sirve los estáticos y hace de proxy a /api
RUN apk update && apk upgrade --no-cache
COPY --from=build /app/dist/practikalia-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# ...
USER nginx                     # nunca corre como root
```

Antes, `frontend/Dockerfile` solo ejecutaba `pnpm build` y no servía nada por sí mismo — un contenedor `nginx` aparte servía los estáticos desde un volumen compartido. Ahora la imagen de frontend **ya lleva Nginx dentro** con los estáticos compilados y su propia configuración (`frontend/nginx.conf`, la misma que antes vivía en la raíz del repo). Esto la convierte en una imagen standalone: un centro puede publicarla, descargarla y levantarla sin necesidad de compilar Angular ni de un contenedor `nginx` adicional — por eso `docker-compose.yml` pasó de cuatro servicios a tres.

:::info
Nginx corre como usuario `nginx`, sin privilegios de root — por eso escucha internamente en el puerto `8080` y no en el `80` (los puertos por debajo del 1024 requieren privilegios). `docker-compose.yml` sigue publicando el `80` de siempre hacia fuera; el `8080` es solo el puerto interno del contenedor.
:::

## Por qué importa para Trivy

Ambos ajustes (actualizar paquetes del sistema, quitar binarios sin usar) no son cosmética: al introducir el escaneo de seguridad en la Fase de CI/CD, Trivy encontró vulnerabilidades reales en ambas imágenes — ver [Seguridad](../operations/security.md) para el detalle y cómo se resolvieron.
