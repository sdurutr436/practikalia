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

Ninguna de las tres requirió downgradear una regla de seguridad ni ignorar el hallazgo: se corrigió la causa.

## Cuándo se justifica ignorar un hallazgo

Cuando aparezca un falso positivo o una vulnerabilidad sin parche disponible y sin exposición real (por ejemplo, en una dependencia de test que nunca llega a producción), se documenta con un `.trivyignore` que explique **qué es, por qué no aplica y desde cuándo** — nunca un ignore silencioso o global.

## Reportar una vulnerabilidad del proyecto

Eso es distinto de lo que escanea Trivy (que mira dependencias e imágenes, no lógica de negocio). Para reportar un fallo de seguridad real en el propio código, ver [SECURITY.md](https://github.com/sdurutr436/practikalia/blob/main/SECURITY.md) — nunca en un issue o PR público.
