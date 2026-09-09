---
sidebar_position: 2
---

# Instalación

Practikalia está pensado para instalarse en un servidor dentro de la red del propio centro educativo, no en internet público. Dos formas de hacerlo:

## Con las imágenes publicadas (recomendado)

No hace falta clonar el repositorio ni tener Java o Node instalados.

```bash
curl -O https://raw.githubusercontent.com/sdurutr436/practikalia/main/docker-compose.prod.yml
curl -O https://raw.githubusercontent.com/sdurutr436/practikalia/main/.env.example
mv .env.example .env   # ajusta DB_PASSWORD y JWT_SECRET — ver más abajo
docker compose -f docker-compose.prod.yml up -d
```

## Construyendo desde el código

```bash
git clone https://github.com/sdurutr436/practikalia.git
cd practikalia
cp .env.example .env   # ajusta DB_PASSWORD y JWT_SECRET
docker compose up --build -d
```

Ambas vías levantan tres servicios: `postgres`, `backend` y `frontend` — este último es el único que necesita ser alcanzable desde otros equipos: es una imagen de Nginx con los estáticos ya compilados dentro, que sirve el frontend y reenvía `/api/` al backend interno.

:::danger
`DB_PASSWORD` y `JWT_SECRET` no tienen valor por defecto — `docker compose up` falla con un mensaje claro si faltan, en vez de arrancar con un secreto conocido. Genera `JWT_SECRET` con `openssl rand -base64 32`. Ver [Variables de entorno](environment-variables.md).
:::

:::warning
Solo si construyes desde el código: `docker-compose.yml` también publica el puerto `8080` del backend para depurar en directo (`docker-compose.prod.yml` no lo hace). En un despliegue real conviene cerrarlo en el firewall del servidor: todo el tráfico de la app debe pasar por `frontend` en el puerto 80.
:::

Para que cualquier equipo del centro alcance la aplicación, el servidor necesita una IP fija (o reservada por DHCP) con el puerto 80 abierto en su firewall; cada PC accede con `http://<ip-del-servidor>/`. Ver [Despliegue](../operations/deployment.md) para más detalle.
