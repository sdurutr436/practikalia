# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y los mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## [Sin publicar]

### Added

- Dockerización de backend, frontend y nginx con Docker Compose.
- Esquema inicial de la entidad `Etiqueta`.
- Fase 1 — Usuario y autenticación básica: entidades `Usuario`/`CorreoPermitido`, JWT en cookie con alcance restringido, CSRF, honeypot, bloqueo por fuerza bruta, política de contraseña, endpoints de login/logout/me/cambiar-contraseña y alta de usuario, catálogo de errores.
- Migraciones Flyway (`etiqueta` baseline, `usuario` y `correo_permitido`).
- `LICENSE` (MIT), `CODE_OF_CONDUCT.md` y `SECURITY.md`.
