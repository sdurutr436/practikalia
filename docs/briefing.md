# Practikalia

Practikalia es una plataforma open source pensada para centros educativos que necesitan gestionar empresas de prácticas, conservar su histórico y orientar mejor la asignación del alumnado.

No es una bolsa de empleo ni un clon de LinkedIn. El objetivo es ayudar a que alumnos y profesores tengan una visión más útil, privada y trazable de las empresas colaboradoras.

Su público está centrado en empresas del sector tecnológico, pero es expandible a más institutos de otros sectores.

---

## Qué es

Practikalia es una red cerrada para centros educativos donde:

- El alumnado puede consultar empresas de prácticas, su afinidad estimada con cada una y marcar interés.
- El profesorado gestiona empresas, asignaciones de prácticas (con tutor de empresa y profesor de empresa), histórico, observaciones y datos sensibles.
- Las reviews de alumnos pasan por moderación docente; las de profesores se publican directamente.
- El sistema calcula afinidad entre alumno y empresa a partir de las etiquetas coincidentes y el sector.
- Se puede registrar si una empresa contrata y su tasa de contratación.
- Un administrador (un profesor con permisos ampliados, no un rol aparte) organiza el catálogo de sectores y etiquetas, da de alta al profesorado, gestiona la whitelist de correos permitidos y configura el nombre y el logo del centro.

Cada centro puede desplegar su propia instancia y gestionar sus propios datos.

---

## El problema que resuelve

En muchos centros, la información sobre empresas de prácticas está dispersa, en gigantes hojas de cálculo o se pierde con el tiempo al no actualizarse con frecuencia de forma automática.

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
- Facilitar que cualquier centro pueda desplegar su propia instancia y configurar su propia identidad (nombre, logo) y quién puede entrar.

---

## Características principales

### Directorio de empresas

- Ficha de empresa con nombre, descripción, imagen, dirección y sector/actividad.
- Etiquetas y observaciones relevantes; tutor de empresa y profesor de empresa asociados a cada práctica.
- Buscador y filtros avanzados (sector, etiquetas, estado de publicación), paginados desde el servidor.
- Información pública para alumnado; información sensible visible solo para profesorado.

### Sectores y etiquetas

- Catálogo en árbol de tres niveles: sector → actividad principal → etiqueta, más grupos transversales (etiquetas válidas para cualquier sector, como la modalidad de trabajo).
- Gestión completa (alta, renombrado y borrado) desde una pantalla de administrador, sin tocar la base de datos.

### Asignación de prácticas

- Asignar, cerrar y consultar el histórico de prácticas por alumno y por empresa.
- Cada asignación registra profesor de empresa, tutor de empresa y grado/año del alumno.

### Reviews y moderación

- Reviews de profesores, publicadas directamente.
- Reviews de alumnos sujetas a aprobación, con cola de moderación por estado (pendientes/aprobadas/rechazadas) y reversión de una decisión ya tomada.
- Trazabilidad de quién aprobó cada review. Red cerrada, sin exposición pública.

### Matching de afinidad

- Recomendación de empresas en función del perfil del alumno.
- Afinidad calculada por solapamiento de etiquetas entre alumno y empresa, con un extra si coincide el sector de interés del alumno.
- Explicación siempre visible: qué etiquetas coinciden y si coincide el sector, no solo un número.

### Expresión de interés

- El alumno puede marcar que una empresa le interesa.
- El profesorado puede ver qué alumnos están interesados en cada empresa.
- La asignación sigue estando mediada por el centro.

### Histórico y seguimiento

- Histórico de alumnos por empresa.
- Tasa de contratación por empresa, como señal de valor real.

### Alumnado

- Listado paginado con ficha editable, alta individual e importación masiva por CSV (con plantilla descargable, todo o nada).
- Auto-registro por correo institucional: la cuenta nace pendiente y un administrador la confirma.
- Contraseña inicial igual al DNI sin la letra; el alumnado debe cambiarla en el primer acceso.

### Profesorado

- Alta y edición del profesorado desde la interfaz.
- Reparto de tutorías de clase (grupo/curso) y de prácticas (empresas) por profesor.

### Configuración del centro

- Nombre y logo del centro, usados en el acceso, la cabecera y el favicon.
- Whitelist de correos permitidos, además de los dominios institucionales configurados en el servidor.

---

## Modelo de acceso

Practikalia funciona como red cerrada.

El acceso se realiza mediante correo institucional. Cada centro define qué dominios admite en su configuración de despliegue:

```env
ALLOWED_DOMAINS=g.educaand.es,iesmidominio.es
```

Además, un administrador puede permitir correos concretos fuera de esos dominios desde la pantalla de configuración del centro (whitelist de correos permitidos).

### Autenticación

- Inicio de sesión mediante correo institucional y contraseña, con cambio de contraseña obligatorio en el primer acceso.
- Auto-registro del alumnado con correo institucional: la cuenta nace pendiente de aprobación, nunca activa al instante. Restringido a los dominios permitidos por el centro; no admite correos externos.

#### A futuro:

- Verificación por código temporal enviado por correo (OTP).
- 2FA por email, tanto para confirmar cuenta como para iniciar sesión.
- Envío automático de la contraseña temporal por correo (hoy se comunica manualmente: la ve quien da de alta o aprueba la cuenta).
- OAuth, como opción adicional.

### Roles

#### Alumno

Puede ver:

- Empresas publicadas.
- Reviews aprobadas de alumnos y reviews de profesores.
- Descripción, imagen y dirección de la empresa.
- Su afinidad estimada con empresas, con explicación.
- Opción de marcar interés.

Esta vista existe pero todavía no se ha verificado en uso real (ver [Estado actual](#estado-actual)).

#### Profesor

Además de lo anterior, puede ver y gestionar:

- Datos sensibles de contacto.
- Histórico completo de alumnos por empresa.
- Reviews pendientes de moderación.
- Gestión de empresas y de asignaciones de prácticas.
- Gestión de intereses y seguimiento.

La vista de un profesor sin permisos de administrador tampoco se ha verificado todavía en uso real.

#### Administrador

Es un profesor con permisos ampliados (no un rol aparte): hereda todo lo del profesor y además gestiona:

- Altas y edición de profesorado, con su reparto de tutorías.
- Aprobación de auto-registros y activación/desactivación de cuentas.
- Whitelist de correos permitidos.
- Catálogo de sectores y etiquetas (árbol completo).
- Configuración del centro: nombre y logo.

El catálogo de grados/ciclos sigue gestionándose directamente en base de datos por cada centro; no tiene pantalla ni endpoint de escritura todavía.

---

## Stack del proyecto

### Frontend

- Angular 22, TypeScript
- SCSS (ITCSS + BEM + Atomic Design)
- pnpm

### Backend

- Java 25, Spring Boot 4.1
- Spring Security, Spring Data JPA, Flyway
- JWT (jjwt), springdoc-openapi

### Base de datos

- PostgreSQL 17

### Infraestructura

- Docker / Docker Compose
- Nginx como proxy inverso

---

## Estructura general

```text
practikalia/
├── backend/           # API Spring Boot
├── frontend/          # Aplicación Angular
├── docs/              # Documentación funcional y técnica
├── nginx/             # Configuración de nginx para despliegue
└── docker-compose.yml
```

---

## Configuración básica

### Variables de entorno

```env
DB_NAME=practikalia
DB_USER=practikalia
DB_PASSWORD=change_me

# Dominios de correo que el centro admite al dar de alta alumnado, separados por comas.
ALLOWED_DOMAINS=g.educaand.es,iesmidominio.es
```

Ver `.env.example` en la raíz del repo. El envío de correo (contraseña temporal, OTP) no
está implementado todavía: esas variables no existen aún.

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
pnpm install
pnpm start
```

Despliegue completo con Docker Compose, documentado en el
[README](../README.md#despliegue-en-tu-propio-servidor-circuito-cerrado).

---

## Flujo actual

1. El administrador o profesor crea la cuenta del usuario (alumno o profesor), o el
   propio alumno se auto-registra con su correo institucional y queda pendiente.
2. El sistema genera una contraseña temporal (el DNI sin la letra, para alumnado)
   asociada al correo.
3. Un administrador confirma la cuenta si nació pendiente (auto-registro o
   importación por CSV); una cuenta creada a mano nace ya confirmada.
4. El usuario recibe sus credenciales por el canal que defina el centro — no se
   envían por correo de forma automática.
5. El usuario inicia sesión con la contraseña temporal.
6. El sistema obliga a cambiar la contraseña en el primer acceso.
7. Una vez cambiada, el usuario accede con su nueva contraseña en accesos posteriores.
8. Si es alumno, accede a la capa visible para alumnado; si es profesor, accede
   además a la capa de gestión.
9. El alumno consulta empresas, reviews y afinidad, y puede marcar interés.
10. El profesorado revisa, modera, asigna prácticas y decide con más contexto.

### Flujo futuro (mejora prevista)

En una fase posterior, el acceso pasará a verificarse mediante un código temporal (OTP) enviado al correo institucional, tanto en el registro como en cada inicio de sesión, eliminando la necesidad de contraseña persistente.

---

## Filosofía del proyecto

Practikalia no busca automatizar por completo la decisión del centro.

La idea es ofrecer mejor información para que el profesorado pueda decidir con más criterio y para que el alumnado entienda mejor qué empresas existen, qué encaje pueden tener y qué experiencias previas ha habido.

El sistema recomienda, pero no sustituye la mediación docente.

---

## Estado actual

Primera beta (`0.1.0-beta`), verificada desde la vista de administrador / profesor con
permisos de administrador. Construido y funcionando:

- Autenticación, alta de usuarios y auto-registro de alumnado.
- Directorio de empresas, con buscador, filtros y asignación de prácticas.
- Catálogo de sectores y etiquetas en árbol, gestionable por un administrador.
- Reviews con moderación por estado y reversión de una decisión ya tomada.
- Intereses y afinidad básica (solapamiento de etiquetas + sector).
- Gestión de profesorado y configuración del centro (nombre, logo, whitelist).
- Panel diferenciado por rol, con contadores del centro.

Pendiente de verificar en uso real: la vista de un profesor sin permisos de
administrador, la vista de alumnado, y el flujo completo de alta/primer acceso de una
cuenta nueva.

Huecos conocidos: la edición del perfil de empresa está por revisar, y los
contadores de alumnado del panel (activado/sin asignar) todavía no enlazan a la
vista de alumnado filtrada correspondiente.

---

## Roadmap

### MVP — completo

- Autenticación por correo institucional
- CRUD de empresas
- Reviews de profesores
- Reviews de alumnos con moderación
- Perfil de alumno
- Expresión de interés
- Matching básico
- Histórico por empresa

### Más adelante

- Envío automático de contraseña temporal y OTP por correo
- Catálogo de grados editable desde la interfaz
- Importación de datos académicos desde fuentes externas
- Métricas avanzadas por empresa
- Dashboard para coordinación
- Mejoras del motor de afinidad (pesos configurables por el centro)
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
