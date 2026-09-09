---
sidebar_position: 4
---

# Despliegue

Practikalia está pensado para un servidor dentro de la red de un centro educativo, no en internet público — ver [Arquitectura](../architecture/overview.md) para por qué `frontend` es el único servicio expuesto.

## Requisitos de red

- El servidor necesita una IP fija (o reservada por DHCP) con el **puerto 80** abierto en su firewall. Ningún otro puerto necesita estar abierto hacia el resto de la red.
- Cada equipo del centro accede con `http://<ip-del-servidor>/`. Un nombre en vez de una IP (`http://practikalia.local/`) hay que resolverlo fuera de la app — DNS/router del centro o archivo hosts de cada equipo. **Queda pendiente (WIP)**, depende de la infraestructura de cada centro.
- `docker-compose.yml` publica también el puerto `8080` del backend, solo para depurar en directo. En un despliegue real, ciérralo en el firewall del servidor.

## Antes de exponerlo a la red del centro

:::danger Cambia el secreto del JWT
`jwt.secret` tiene un valor por defecto pensado solo para desarrollo (ver [Variables de entorno](../getting-started/environment-variables.md)). Un despliegue real necesita fijar un valor propio antes de dar acceso a nadie — de lo contrario, cualquiera con ese valor por defecto podría firmar tokens válidos.
:::

## Cómo se instala hoy

La única vía disponible hoy es clonar el repositorio y construir las imágenes en el propio servidor — ver [Instalación](../getting-started/installation.md). El paso de `docker compose build` compila backend y frontend desde el código fuente cada vez.

:::info Pendiente
Las imágenes ya se publican en Docker Hub (ver [Imágenes Docker](docker-images.md)), pero `docker-compose.yml` todavía usa `build:` en vez de `image:`. Para que un centro pudiera instalar Practikalia con un simple `docker compose pull && docker compose up -d`, sin clonar el repositorio ni tener Node/Java instalados, haría falta una variante de `docker-compose.yml` que referencie las imágenes publicadas. No existe todavía.
:::
