---
sidebar_position: 3
---

# Seguridad

## Escaneo con Trivy

Corre en cada PR y push a `desarrollo` (nunca en `main`, que se asume ya validado) — ver [CI/CD](ci-cd.md). Cubre tres cosas:

- **Dependencias del repo** (`pom.xml`, `pnpm-lock.yaml`): vulnerabilidades conocidas en librerías declaradas.
- **Configuración** (Dockerfiles, `docker-compose.yml`): malas prácticas conocidas de la propia herramienta.
- **Imágenes construidas** (backend y frontend): vulnerabilidades del sistema operativo base y de lo que queda instalado en la imagen final.

`CRITICAL` y `HIGH` bloquean el pipeline; `MEDIUM`/`LOW` se reportan (subidos como SARIF a la pestaña **Security** de GitHub) pero no lo frenan.

## Un ejemplo real

Al introducir Trivy, el escaneo encontró vulnerabilidades reales en ambas imágenes — no era un ejercicio teórico. Así se resolvieron, como referencia de qué hacer cuando aparezca la próxima:

- **Paquetes del sistema desactualizados** (Alpine en la imagen de frontend, Ubuntu en la de backend): se añadió `apk upgrade`/`apt-get upgrade` en el Dockerfile, para que la imagen se construya siempre con los últimos parches del sistema disponibles en ese momento, en vez de depender de cuándo se reconstruyó la imagen base oficial.
- **Un binario sin usar** (`pebble`, una herramienta de pruebas ACME que trae de serie la imagen base de `eclipse-temurin` y que la aplicación nunca ejecuta): se eliminó del Dockerfile con `rm`. Menos superficie, cero riesgo de romper nada.
- **Dependencias Java con CVE conocida** (Tomcat embebido, driver de PostgreSQL): ambas vienen fijadas por el BOM de Spring Boot. Se sobreescribieron solo esas dos propiedades de versión en `pom.xml` (`tomcat.version`, `postgresql.version`), sin tocar la versión de Spring Boot en sí — el mecanismo que el propio Spring Boot ofrece para este caso.
- **Una dependencia JavaScript con un RCE conocido** (`serialize-javascript`, transitiva de los plugins de webpack de Docusaurus, sin relación con nuestro propio código): se fijó la versión corregida con un `overrides` en `docs-site/pnpm-workspace.yaml`, sin tocar Docusaurus en sí.
- **Ambos Dockerfiles corrían como root** (`DS-0002`, sin ningún `USER` declarado): se creó un usuario propio sin privilegios en cada imagen (`practikalia` en el backend, el `nginx` que ya trae la imagen base en el frontend). Como los puertos por debajo del 1024 requieren privilegios, Nginx pasó a escuchar internamente en el `8080` — `docker-compose.yml` sigue publicando el `80` de siempre hacia fuera, eso no cambia para quien lo usa.

Ninguno de estos hallazgos requirió downgradear una regla de seguridad ni ignorar el problema: se corrigió la causa.

## Cuándo se justifica ignorar un hallazgo

Cuando aparezca un falso positivo o una vulnerabilidad sin parche disponible y sin exposición real, se documenta con un `.trivyignore` que explique **qué es, por qué no aplica y desde cuándo** — nunca un ignore silencioso o global. Ejemplo real en este repo: `image-size` (transitiva de `@docusaurus/mdx-loader`) tiene dos CVEs de denegación de servicio sin parche todavía, pero solo se ejecuta en tiempo de build de Docusaurus sobre los propios assets del repositorio — nunca sobre contenido externo ni en el sitio ya publicado. Ver [`.trivyignore`](https://github.com/sdurutr436/practikalia/blob/main/.trivyignore) para la justificación completa.

## Reportar una vulnerabilidad del proyecto

Eso es distinto de lo que escanea Trivy (que mira dependencias e imágenes, no lógica de negocio). Para reportar un fallo de seguridad real en el propio código, ver [SECURITY.md](https://github.com/sdurutr436/practikalia/blob/main/SECURITY.md) — nunca en un issue o PR público.
