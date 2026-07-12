# Practikalia

Practikalia es una plataforma open source pensada para centros educativos que necesitan gestionar empresas de prácticas, conservar su histórico y orientar mejor la asignación del alumnado.

No es una bolsa de empleo ni un clon de LinkedIn. El objetivo es ayudar a que alumnos y profesores tengan una visión más útil, privada y trazable de las empresas colaboradoras.

Su público está centrado a empresas del sector tecnológico, pero es expandible a más institutos de otros sectores.

---

## Qué es

Practikalia es una red cerrada para centros educativos donde:

- El alumnado puede consultar empresas de prácticas.
- El profesorado puede registrar histórico, observaciones y datos sensibles.
- Las reviews de alumnos pasan por moderación docente.
- Las reviews de profesores se publican directamente.
- El sistema calcula afinidad entre alumno y empresa.
- Se puede registrar si una empresa contrata y su tasa de contratación.

Cada centro puede desplegar su propia instancia y gestionar sus propios datos.

---

## El problema que resuelve

En muchos centros, la información sobre empresas de prácticas está dispersa, en gigantes hojas de calculo o se pierde con el tiempo al no actualizarse con frecuencia de forma automática.

Eso provoca varios problemas:

- Alumnos que eligen empresa sin contexto real.
- Profesores que acumulan experiencia útil pero no tienen una actualización segura.
- Falta de trazabilidad sobre qué alumnos pasaron por una empresa y cómo fue la experiencia.
- Dificultad para decidir qué empresa encaja mejor con cada perfil.
- Ausencia de indicadores prácticos como la tasa de contratación real.

Practikalia nace para convertir ese conocimiento disperso en una herramienta útil y reutilizable.

---

## Objetivos

- Centralizar la información de empresas de prácticas.
- Mantener un histórico interno por centro.
- Permitir reviews útiles y moderadas.
- Mostrar al alumnado solo la información aprobada.
- Reservar al profesorado los datos sensibles y de seguimiento.
- Recomendar empresas en función del perfil del alumno.
- Facilitar que cualquier centro pueda desplegar su propia instancia.

---

## Características principales

### Directorio de empresas

- Ficha de empresa con nombre, descripción, imagen, dirección y sector.
- Tecnologías, etiquetas y observaciones relevantes.
- Información pública interna para alumnado.
- Información sensible visible solo para profesorado.

### Reviews y moderación

- Reviews de profesores.
- Reviews de alumnos sujetas a aprobación.
- Trazabilidad de quién aprobó cada review.
- Red cerrada, sin exposición pública.

### Matching de afinidad

- Recomendación de empresas en función del perfil del alumno.
- Afinidad basada en calificaciones, intereses, tecnologías y criterios definidos por el centro.
- Ordenación de empresas según encaje estimado.

### Expresión de interés

- El alumno puede marcar que una empresa le interesa.
- El profesorado puede ver qué alumnos están interesados en cada empresa.
- La asignación sigue estando mediada por el centro.

### Histórico y seguimiento

- Histórico de alumnos por empresa.
- Registro de experiencia previa.
- Información sobre contratación posterior.
- Tasa de contratación como señal de valor real.

---

## Modelo de acceso

Practikalia funciona como red cerrada.

El acceso se realiza mediante correo institucional permitido por la instancia. Cada centro define qué dominios puede usar en su configuración.

Ejemplo:

```env
ALLOWED_DOMAINS=g.educaand.es,iesmidominio.es
```

En caso de que no sea posible, el centro puede restringir el acceso a los alumnos en una whitelist. Se proporcionará el medio para que se más sencillo realizar esto.

### Autenticación

- Inicio de sesión mediante correo institucional y contraseña.

#### A futuro:

- Verificación por código temporal enviado por correo.
- 2FA por email tanto para confirmar cuenta como para iniciar sesión.
- Sin OAuth por ahora.
- Sin registro abierto con correos externos.

### Roles

#### Alumno

Puede ver:

- Empresas publicadas.
- Reviews aprobadas de alumnos.
- Reviews de profesores.
- Descripción, imagen y dirección de la empresa.
- Su afinidad estimada con empresas.
- Opción de marcar interés.

#### Profesor

Además de lo anterior, puede ver y gestionar:

- Datos sensibles de contacto.
- Histórico completo de alumnos por empresa.
- Reviews pendientes de moderación.
- Gestión de empresas.
- Gestión de intereses y seguimiento.
- Validación de contenido.

#### Administrador

Es un profesor con permisos ampliados (no un rol aparte): hereda todo lo del
profesor y además gestiona:

- Altas de profesores (ya operativo).
- Whitelist de correos permitidos para el registro.
- Catálogos de etiquetas y grados.
- Usuarios: listado y activación/desactivación.

---

## Stack del proyecto

### Frontend

- Angular
- TypeScript
- HTML / SCSS

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA

### Base de datos

- PostgreSQL

### Infraestructura

- Maven
- Nginx
- Docker / Docker Compose (opcional según despliegue)

---

## Estructura general

```text
practikalia/
├── frontend/        # Aplicación Angular
├── backend/         # API Spring Boot
├── docs/            # Documentación funcional y técnica
├── nginx/          # Configuración de despliegue
└── README.md
```

---

## Configuración básica

### Variables de entorno del backend

```env
APP_NAME=Practikalia
APP_ENV=development

DB_HOST=localhost
DB_PORT=5432
DB_NAME=practikalia
DB_USER=postgres
DB_PASSWORD=postgres

JWT_SECRET=change_me
JWT_EXPIRATION=604800000

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=noreply@example.com
MAIL_PASSWORD=change_me
MAIL_FROM=Practikalia <noreply@example.com>

ALLOWED_DOMAINS=g.educaand.es,iesmidominio.es
OTP_EXPIRATION_MINUTES=10
```

### Variables de entorno del frontend

```env
API_URL=http://localhost:8080/api
APP_NAME=Practikalia
```

---

## Ejecución en desarrollo

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

> Ajusta los scripts del frontend según la configuración real del proyecto (`npm start`, `ng serve` o equivalente).

---

## Flujo actual

1. El administrador o profesor crea la cuenta del usuario (alumno o profesor) en el sistema.
2. El sistema genera una contraseña temporal y la asocia al correo institucional del usuario.
3. El usuario recibe sus credenciales (correo + contraseña temporal) por el canal que defina el centro.
4. El usuario inicia sesión con la contraseña temporal.
5. El sistema obliga a cambiar la contraseña en el primer acceso.
6. Una vez cambiada, el usuario accede con su nueva contraseña en accesos posteriores.
7. Si es alumno, accede a la capa visible para alumnado.
8. Si es profesor, accede además a la capa de gestión.
9. El alumno consulta empresas, reviews y afinidad.
10. El alumno puede marcar interés.
11. El profesorado revisa, modera y decide con más contexto.

### Flujo futuro (mejora prevista)

En una fase posterior, el acceso pasará a verificarse mediante un código temporal (OTP) enviado al correo institucional, tanto en el registro como en cada inicio de sesión, eliminando la necesidad de contraseña persistente.

---

## Filosofía del proyecto

Practikalia no busca automatizar por completo la decisión del centro.

La idea es ofrecer mejor información para que el profesorado pueda decidir con más criterio y para que el alumnado entienda mejor qué empresas existen, qué encaje pueden tener y qué experiencias previas ha habido.

El sistema recomienda, pero no sustituye la mediación docente.

---

## Estado actual

Proyecto en construcción.

Líneas activas de trabajo:

- Modelo de datos de empresas, usuarios, reviews e intereses.
- Sistema de autenticación por correo institucional y contraseña.
- Panel de moderación docente.
- Algoritmo inicial de afinidad.
- Gestión de tasa de contratación.
- Despliegue self-hosted por instancia.

---

## Roadmap

### MVP

- Autenticación por correo institucional
- CRUD de empresas
- Reviews de profesores
- Reviews de alumnos con moderación
- Perfil de alumno
- Expresión de interés
- Matching básico
- Histórico por empresa

### Más adelante

- Importación de datos académicos desde fuentes externas
- Métricas avanzadas por empresa
- Dashboard para coordinación
- Mejoras del motor de afinidad
- OAuth como opción adicional
- Federación parcial entre instancias

---

## Público objetivo

- Centros de Formación Profesional
- Departamentos de orientación y prácticas
- Tutores docentes
- Coordinaciones FEOE / Dual
- Proyectos open source educativos

---

## Licencia

[MIT](../LICENSE).

La intención del proyecto es que sea open source y desplegable por cualquier centro en su propia infraestructura.

---

## Nombre

**Practikalia** es el nombre definitivo del proyecto.