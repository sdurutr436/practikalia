---
sidebar_position: 3
---

# Troubleshooting

### `pnpm install` falla con `ERR_PNPM_IGNORED_BUILDS`

Pasa en `docs-site/` (Docusaurus depende de `@swc/core` y `core-js`, que tienen scripts de instalación). Es pnpm bloqueando por defecto la ejecución de scripts de dependencias no aprobadas explícitamente — no es un error del proyecto. Ya está resuelto en el repo vía `docs-site/pnpm-workspace.yaml` (`allowBuilds` + `onlyBuiltDependencies`); si aparece en una dependencia nueva, añádela ahí en vez de desactivar la protección globalmente.

### `docker compose up` falla con `port is already allocated` en el 80

Suele ser un contenedor de una sesión anterior que ya no existe en `docker-compose.yml` (por ejemplo, un `nginx` de antes de que `frontend` pasara a ser standalone — ver [Docker](../architecture/docker.md)) pero sigue corriendo y reteniendo el puerto. Reconcilia el proyecto con:

```bash
docker compose up -d --remove-orphans
```

Esto no borra volúmenes ni datos, solo contenedores que ya no están declarados en el compose actual.

### Un test de frontend falla con "not implemented" de jsdom

jsdom no implementa toda la API del DOM de un navegador real (por ejemplo, `scrollIntoView`). Si un test falla así sin que ninguna aserción haya fallado, casi seguro es esto — ver la nota en [Testing](testing.md).

### Escanear todo el repo con Trivy en local es muy lento en Windows

Si ejecutas `trivy fs` contra el repo completo montado en un contenedor Docker en Windows (Docker Desktop + WSL2), puede colgarse varios minutos o agotar el timeout, sobre todo si hay `node_modules` de por medio. El workflow de CI ya excluye esos directorios (`skip-dirs` en `development-ci.yml`) y corre sobre un checkout limpio en Linux, donde no se ha reproducido este problema. Escanear una sola imagen (`trivy image ...`) sí es rápido en cualquier entorno.
