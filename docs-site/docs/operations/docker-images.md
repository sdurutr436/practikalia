---
sidebar_position: 2
---

# Imágenes Docker

Publicadas en Docker Hub como [`sdurutr436/practikalia-backend`](https://hub.docker.com/r/sdurutr436/practikalia-backend) y [`sdurutr436/practikalia-frontend`](https://hub.docker.com/r/sdurutr436/practikalia-frontend). Ambas son standalone: no hace falta clonar el repositorio ni compilar nada para usarlas — ver [Docker](../architecture/docker.md) para por qué el frontend también lo es.

Para levantar las tres directamente con estas imágenes (sin construir nada), usa [`docker-compose.prod.yml`](https://github.com/sdurutr436/practikalia/blob/main/docker-compose.prod.yml) — ver [Despliegue](deployment.md).

| Rama | Etiquetas | Cuándo se publica |
|---|---|---|
| `main` | `latest`, `sha-<7 caracteres>` | En cada push a `main`, tras validar documentación (no hay tests de aplicación en `main`) |
| `desarrollo` | `beta`, `beta-<7 caracteres>` | En cada push a `desarrollo`, y solo si los tests y Trivy de ese mismo commit pasaron |

La etiqueta con el SHA corto es la trazable: identifica exactamente de qué commit salió esa imagen, incluso si `latest`/`beta` avanzan.

```bash
docker pull sdurutr436/practikalia-backend:latest
docker pull sdurutr436/practikalia-frontend:latest
```

:::info
Ningún PR publica imágenes, hacia `main` ni hacia `desarrollo` — solo las valida (`docker build`, sin `push`). Ver [CI/CD](ci-cd.md).
:::
