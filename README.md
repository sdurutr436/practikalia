# Practikalia

Plataforma open source y autohosteable para que centros educativos gestionen sus empresas de prácticas: histórico, reviews moderadas y afinidad alumno-empresa.

No es una bolsa de empleo ni un clon de LinkedIn — es una red cerrada por centro.

Visión funcional completa, roles y roadmap: [docs/briefing.md](docs/briefing.md).

## Estado actual

Proyecto en construcción: scaffolding de Angular y Spring Boot, dockerizado y funcional. Todavía sin modelo de datos, autenticación ni features de negocio.

## Stack

- **Frontend**: Angular, TypeScript
- **Backend**: Java 25, Spring Boot, Spring Security, Spring Data JPA
- **Base de datos**: PostgreSQL
- **Infraestructura**: Docker Compose, Nginx

## Estructura

```text
practikalia/
├── backend/          # API Spring Boot
├── frontend/          # Aplicación Angular
├── docs/              # Documentación funcional y técnica
├── nginx/             # Configuración de nginx para despliegue
└── docker-compose.yml
```

## Desarrollo local

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
pnpm install
pnpm start
```

## Despliegue con Docker

```bash
cp .env.example .env   # ajusta las credenciales de la base de datos
docker compose up --build
```

La app queda disponible en `http://localhost` (Nginx sirve el frontend y hace de proxy a `/api`).

## Licencia

Pendiente de definir. El proyecto está pensado para ser open source y desplegable por cualquier centro en su propia infraestructura.
