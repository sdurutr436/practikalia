---
sidebar_position: 4
---

# Docker

**WIP** — pendiente de desarrollar en detalle: ambos Dockerfiles son multi-stage (`backend/Dockerfile`: JDK para compilar, JRE para ejecutar; `frontend/Dockerfile`: Node para compilar con pnpm, Nginx para servir los estáticos y hacer de proxy a `/api/`), y por qué eso permite publicar imágenes standalone que un centro puede descargar y orquestar sin construir desde el código fuente.
