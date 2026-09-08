# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y los mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## [Sin publicar]

## [0.1.0-beta] - 2026-09-06

Primera beta: pensada para usarse desde la vista de administrador o de profesor con
permisos de administrador. La vista de un profesor sin esos permisos, la de alumnado
y el flujo completo de alta/primer acceso de una cuenta nueva existen pero aún no se
han verificado en uso real.

### Added

- Autenticación y alta de usuarios: login con JWT en cookie httpOnly, CSRF, honeypot, bloqueo por fuerza bruta, política de contraseña, cambio de contraseña obligatorio en el primer acceso y catálogo de errores.
- Auto-registro de alumnado por correo institucional: nace pendiente de aprobación, con DNI validado por letra de control y catálogo de grados público sin sesión para el formulario de alta.
- Directorio de empresas: listado paginado con buscador y filtros avanzados (nombre, sector, etiquetas, estado de publicación), ficha, alta/edición y subida de imagen.
- Sectores y etiquetas: catálogo en árbol de tres niveles (sector → actividad → etiqueta) más grupos transversales, con alta, renombrado y borrado desde una pantalla de administrador.
- Asignación de prácticas: alta, cierre e histórico por alumno y por empresa, con profesor de empresa, tutor de empresa y grado/año.
- Reviews: alta por profesorado (publicación directa) y por alumnado (con moderación), cola de moderación por estado (pendientes/aprobadas/rechazadas) con reversión de una decisión ya tomada.
- Intereses del alumnado por empresa, con listado de interesados para el profesorado.
- Afinidad alumno-empresa: cálculo con explicación (etiquetas coincidentes y sector), vista de autoservicio para el alumno y vista de tutor/profesor.
- Tasa de contratación por empresa.
- Alumnado: listado paginado con ficha editable, alta individual e importación masiva por CSV con plantilla descargable, confirmación de cuentas y contraseña inicial igual al DNI sin la letra.
- Profesorado: alta y edición, con reparto de tutorías de clase y de prácticas.
- Configuración del centro: nombre, logo y gestión de la whitelist de correos permitidos, desde la interfaz.
- Panel del centro: contadores de empresas y alumnado, carrusel de empresas publicadas y cola de reseñas pendientes con moderación en línea.
- Interfaz: migración completa a SCSS bajo ITCSS + BEM + Atomic Design, tokens de diseño definitivos e iconografía Fluent UI, cabecera con menú de hamburguesa en escritorio y acordeón en móvil.
- Despliegue: Docker Compose (PostgreSQL, backend, frontend, nginx) y datos de demostración.
- `LICENSE` (MIT), `CODE_OF_CONDUCT.md` y `SECURITY.md`.
