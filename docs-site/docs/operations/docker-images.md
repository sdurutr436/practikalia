---
sidebar_position: 2
---

# Imágenes Docker

Publicadas en Docker Hub como `<usuario>/practikalia-backend` y `<usuario>/practikalia-frontend`. Ambas son standalone: no hace falta clonar el repositorio ni compilar nada para usarlas — ver [Docker](../architecture/docker.md) para por qué el frontend también lo es.

| Rama | Etiquetas | Cuándo se publica |
|---|---|---|
| `main` | `latest`, `sha-<7 caracteres>` | En cada push a `main`, tras validar documentación (no hay tests de aplicación en `main`) |
| `desarrollo` | `beta`, `beta-<7 caracteres>` | En cada push a `desarrollo`, y solo si los tests y Trivy de ese mismo commit pasaron |

La etiqueta con el SHA corto es la trazable: identifica exactamente de qué commit salió esa imagen, incluso si `latest`/`beta` avanzan.

```bash
docker pull <usuario>/practikalia-backend:latest
docker pull <usuario>/practikalia-frontend:latest
```

:::info
Ningún PR publica imágenes, hacia `main` ni hacia `desarrollo` — solo las valida (`docker build`, sin `push`). Ver [CI/CD](ci-cd.md).
:::
