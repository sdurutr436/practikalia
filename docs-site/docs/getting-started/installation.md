---
sidebar_position: 2
---

# Instalación

Practikalia está pensado para instalarse en un servidor dentro de la red del propio centro educativo, no en internet público.

```bash
git clone https://github.com/sdurutr436/practikalia.git
cd practikalia
cp .env.example .env   # ajusta DB_NAME, DB_USER, DB_PASSWORD
docker compose up --build -d
```

Esto levanta tres servicios: `postgres`, `backend` y `frontend` — este último es el único que necesita ser alcanzable desde otros equipos: es una imagen de Nginx con los estáticos ya compilados dentro, que sirve el frontend y reenvía `/api/` al backend interno.

:::warning
`docker-compose.yml` también publica el puerto `8080` del backend para depurar en directo. En un despliegue real conviene cerrarlo en el firewall del servidor: todo el tráfico de la app debe pasar por `frontend` en el puerto 80.
:::

Para que cualquier equipo del centro alcance la aplicación, el servidor necesita una IP fija (o reservada por DHCP) con el puerto 80 abierto en su firewall; cada PC accede con `http://<ip-del-servidor>/`. Ver [Despliegue](../operations/deployment.md) para más detalle.
