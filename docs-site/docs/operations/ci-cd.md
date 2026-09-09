---
sidebar_position: 1
---

# CI/CD

Dos workflows de GitHub Actions, uno por rama protegida. Ninguna otra rama dispara nada.

| Evento | Documentación | Docker | Tests | Trivy |
|---|---|---|---|---|
| PR → `main` | Valida que Docusaurus compila (no publica) | Valida que ambas imágenes construyen (no publica) | No | No |
| Push a `main` | Publica en GitHub Pages | Publica `latest` + `sha-<corto>` en Docker Hub | No | No |
| PR → `desarrollo` | No | Valida que ambas imágenes construyen (no publica) | Sí (backend + frontend) | Sí |
| Push a `desarrollo` | No | Publica `beta` + `beta-<corto>` en Docker Hub | Sí | Sí |

`main` nunca ejecuta tests de aplicación ni Trivy — se asume que ese código ya pasó por `desarrollo`. La publicación de imágenes solo ocurre en un `push` (nunca en un PR), y en `desarrollo` está condicionada a que tests y Trivy hayan pasado antes.

## [`.github/workflows/development-ci.yml`](https://github.com/sdurutr436/practikalia/blob/desarrollo/.github/workflows/development-ci.yml)

- `backend-tests`: `./mvnw test`, con gate de cobertura JaCoCo (80% de líneas).
- `frontend-tests`: `pnpm test` con cobertura (80% de líneas) + `pnpm build`.
- `trivy-fs`: dependencias (`pom.xml`, `pnpm-lock.yaml`) y configuración de Dockerfiles/compose.
- `trivy-image` (matriz backend/frontend): construye cada imagen y la escanea.
- `docker-publish` (matriz backend/frontend, solo en push, solo si todo lo anterior pasa): publica la imagen beta.

## [`.github/workflows/main-release.yml`](https://github.com/sdurutr436/practikalia/blob/desarrollo/.github/workflows/main-release.yml)

- `docs-build` / `docs-deploy`: build de Docusaurus; el deploy a Pages solo corre en push.
- `docker-validate` (solo en PR): construye ambas imágenes sin publicar.
- `docker-release` (solo en push): publica la imagen estable.

## Seguridad de Trivy

Cada escaneo corre dos veces: una con todas las severidades (sube el informe completo a GitHub Security como SARIF, no bloquea), y otra filtrada a `CRITICAL`/`HIGH` que sí bloquea el pipeline si encuentra algo. Detalle en [Seguridad](security.md).

## Permisos

Cada workflow parte de `contents: read` a nivel global; `pages: write`/`id-token: write` (deploy de Pages) y `security-events: write` (subir SARIF) están acotados solo a los jobs que de verdad los necesitan, nunca a nivel de todo el workflow.
