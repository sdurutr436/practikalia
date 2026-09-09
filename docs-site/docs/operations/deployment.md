---
sidebar_position: 4
---

# Despliegue

Practikalia está pensado para un servidor dentro de la red de un centro educativo, no en internet público — ver [Arquitectura](../architecture/overview.md) para por qué `frontend` es el único servicio expuesto.

## La vía rápida: solo las imágenes publicadas

Un centro no necesita clonar el repositorio ni tener Java o Node instalados. Con `docker` y `docker compose`, y estos dos archivos:

- [`docker-compose.prod.yml`](https://github.com/sdurutr436/practikalia/blob/main/docker-compose.prod.yml) — usa las imágenes ya publicadas (ver [Imágenes Docker](docker-images.md)) en vez de compilar nada.
- `.env` — copiado de [`.env.example`](https://github.com/sdurutr436/practikalia/blob/main/.env.example), con `DB_PASSWORD` y `JWT_SECRET` propios (ver [Variables de entorno](../getting-started/environment-variables.md)).

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Levanta `postgres`, `backend` y `frontend`; solo `frontend` publica un puerto (el 80). Ni `docker-compose.yml` (la variante que compila desde el código, para desarrollo) ni `docker-compose.prod.yml` arrancan si falta `DB_PASSWORD` o `JWT_SECRET` en el `.env` — fallan con un mensaje explícito en vez de arrancar con un secreto por defecto.

## Requisitos de red

- El servidor necesita una IP fija (o reservada por DHCP) con el **puerto 80** abierto en su firewall. Ningún otro puerto necesita estar abierto hacia el resto de la red — `docker-compose.prod.yml` ni siquiera publica el 8080 del backend.
- Cada equipo del centro accede con `http://<ip-del-servidor>/`. Un nombre en vez de una IP (`http://practikalia.local/`) hay que resolverlo fuera de la app — DNS/router del centro o archivo hosts de cada equipo. **Queda pendiente (WIP)**, depende de la infraestructura de cada centro.

## Si prefieres construir desde el código

Sigue siendo posible — ver [Instalación](../getting-started/installation.md) (`docker-compose.yml`, con `build:` en vez de `image:`). Tiene el mismo requisito de `DB_PASSWORD`/`JWT_SECRET`, y además publica el puerto `8080` del backend para depurar en directo — ciérralo en el firewall en un despliegue real.
