-- Datos de demostración para poder ver las pantallas con contenido real.
--
-- Todas las cuentas usan el dominio reservado `.test`, que por RFC 2606 no
-- puede existir de verdad: aunque alguien configure `allowed.domains`, estas
-- cuentas nunca colisionan con correos reales del centro. Van además a
-- `correo_permitido` una a una, porque `UsuarioService.login` exige que el
-- correo esté permitido y `allowed.domains` viene vacío por defecto.
--
-- Contraseña de todas: Practikalia1!
-- Los hashes son BCrypt reales generados con el mismo BCryptPasswordEncoder
-- que usa la aplicación, uno distinto por cuenta.
--
-- Las tablas con clave única (correo_permitido, grado, etiqueta, usuario) usan
-- ON CONFLICT DO NOTHING: si una instalación ya tiene el grado 'DAW' o la
-- etiqueta 'Java', se reutiliza la fila existente en vez de tumbar el arranque.
--
-- Las claves ajenas se resuelven por clave natural (correo, nombre) en vez de
-- fijar los id a mano: la secuencia IDENTITY sigue arrancando en 1 y el primer
-- alta hecha desde la aplicación no choca con estas filas.

INSERT INTO correo_permitido (correo) VALUES
    ('admin@practikalia.test'),
    ('robles@practikalia.test'),
    ('lucia@practikalia.test'),
    ('ivan@practikalia.test'),
    ('nerea@practikalia.test'),
    ('hugo@practikalia.test'),
    ('marta@practikalia.test')
ON CONFLICT (correo) DO NOTHING;

INSERT INTO grado (nombre) VALUES
    ('DAW'),
    ('DAM'),
    ('ASIR')
ON CONFLICT (nombre) DO NOTHING;

-- La tabla `etiqueta` hace de catálogo único: de aquí salen tanto el sector de
-- una empresa (empresa.sector_id) como las etiquetas sueltas.
INSERT INTO etiqueta (nombre) VALUES
    ('Desarrollo web'),
    ('Sanidad'),
    ('Agroalimentario'),
    ('Industria'),
    ('Transporte'),
    ('Teletrabajo'),
    ('Presencial'),
    ('Híbrido'),
    ('Java'),
    ('Angular'),
    ('Bases de datos'),
    ('Atención al público')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO usuario (correo, contrasena_hash, rol, es_admin, debe_cambiar_contrasena, activo, fecha_creacion, grado_id, anio) VALUES
    ('admin@practikalia.test',  '$2a$10$Ua7ra3LkUD5Mdrd8s6AkauiBoc2iMgqmsAeNquaEMfL2.fwTCe6yS', 'PROFESOR', TRUE,  FALSE, TRUE, NOW(), NULL, NULL),
    ('robles@practikalia.test', '$2a$10$QVbJRipuLOOaERew5vvyFOrSmH6WKHn8/juknJHE0Z5T6.mUCMfGW', 'PROFESOR', FALSE, FALSE, TRUE, NOW(), NULL, NULL)
ON CONFLICT (correo) DO NOTHING;

INSERT INTO usuario (correo, contrasena_hash, rol, es_admin, debe_cambiar_contrasena, activo, fecha_creacion, grado_id, anio) VALUES
    ('lucia@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAW'),  2026),
    ('ivan@practikalia.test',  '$2a$10$8V.NeqP0K3T1EG5LPT11ZOw6IEtDL535440tf.yFlGR0ktyGukP0K', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAW'),  2026),
    ('nerea@practikalia.test', '$2a$10$4bFr6my5aZOJ3pe8kyugzO/bR4WjDj/2h5wE/kXD0mivj98wLOhV.', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAM'),  2026),
    ('hugo@practikalia.test',  '$2a$10$lIC5YndyukV6D87khhbiIeEzb9i22IubUrG0ZIpAPd.cz2gctpgU2', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAM'),  2026),
    -- Marta no tiene asignación: es la que hace que "alumnado sin asignar" no sea 0.
    ('marta@practikalia.test', '$2a$10$Ua7ra3LkUD5Mdrd8s6AkauiBoc2iMgqmsAeNquaEMfL2.fwTCe6yS', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'ASIR'), 2026)
ON CONFLICT (correo) DO NOTHING;

-- Cuatro publicadas y dos sin publicar, para que el listado del alumnado (solo
-- publicadas) y el del profesorado (todas) se vean distintos.
INSERT INTO empresa (nombre, descripcion, direccion, sector_id, observaciones, contacto_nombre, contacto_telefono, contacto_email, publicada, creada_por_id, fecha_creacion) VALUES
    ('Grupo Ondara Software',
     'Consultora de desarrollo web a medida. El alumnado entra en equipos con tareas reales desde la primera semana.',
     'Calle Betis 14, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Desarrollo web'),
     'Tres plazas por curso. Piden nociones de Git.',
     'Marta Ondara', '954 11 22 33', 'practicas@ondara.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Clínica Dental Aljarafe',
     'Clínica dental con cuatro gabinetes. Prácticas de atención al paciente y gestión de citas.',
     'Avenida de Andalucía 3, Tomares',
     (SELECT id FROM etiqueta WHERE nombre = 'Sanidad'),
     'Una plaza. Horario de tarde.',
     'Dr. Alberto Ruiz', '955 44 55 66', 'info@aljarafedental.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Cerámicas Bailén',
     'Fábrica de cerámica industrial. Mantenimiento de línea y control de calidad.',
     'Polígono El Acebuche s/n, Bailén',
     (SELECT id FROM etiqueta WHERE nombre = 'Industria'),
     'Requiere calzado de seguridad, lo aporta la empresa.',
     'Rosa Linares', '953 77 88 99', 'rrhh@ceramicasbailen.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Nexo Logística',
     'Operador logístico regional. Prácticas en gestión de almacén y trazabilidad de envíos.',
     'Carretera de la Esclusa 22, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Transporte'),
     'Dos plazas. Turno de mañana.',
     'Julio Cabrera', '954 33 44 55', 'personal@nexologistica.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'admin@practikalia.test'), NOW()),

    ('Almazara Vega Sur',
     'Almazara cooperativa. Prácticas en laboratorio de cata y control de acidez.',
     'Camino de la Vega 8, Écija',
     (SELECT id FROM etiqueta WHERE nombre = 'Agroalimentario'),
     'Solo campaña de octubre a enero.',
     'Pilar Vega', '955 12 13 14', 'coop@vegasur.test',
     FALSE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Datavera Analítica',
     'Pendiente de confirmar convenio para el curso que viene.',
     'Calle Luis Montoto 90, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Desarrollo web'),
     'Sin convenio firmado todavía, no publicar.',
     'Sergio Peña', '954 99 88 77', 'hola@datavera.test',
     FALSE, (SELECT id FROM usuario WHERE correo = 'admin@practikalia.test'), NOW());

INSERT INTO empresa_etiqueta (empresa_id, etiqueta_id)
SELECT e.id, t.id
FROM (VALUES
    ('Grupo Ondara Software',   'Teletrabajo'),
    ('Grupo Ondara Software',   'Java'),
    ('Grupo Ondara Software',   'Angular'),
    ('Clínica Dental Aljarafe', 'Presencial'),
    ('Clínica Dental Aljarafe', 'Atención al público'),
    ('Cerámicas Bailén',        'Híbrido'),
    ('Nexo Logística',          'Presencial'),
    ('Nexo Logística',          'Bases de datos'),
    ('Almazara Vega Sur',       'Presencial'),
    ('Datavera Analítica',      'Teletrabajo'),
    ('Datavera Analítica',      'Bases de datos')
) AS v(empresa, etiqueta)
JOIN empresa e ON e.nombre = v.empresa
JOIN etiqueta t ON t.nombre = v.etiqueta;

-- Etiquetas de interés del alumnado: alimentan el ranking de afinidad.
INSERT INTO usuario_etiqueta (usuario_id, etiqueta_id)
SELECT u.id, t.id
FROM (VALUES
    ('lucia@practikalia.test', 'Angular'),
    ('lucia@practikalia.test', 'Java'),
    ('lucia@practikalia.test', 'Teletrabajo'),
    ('ivan@practikalia.test',  'Java'),
    ('marta@practikalia.test', 'Bases de datos'),
    ('marta@practikalia.test', 'Presencial')
) AS v(correo, etiqueta)
JOIN usuario u ON u.correo = v.correo
JOIN etiqueta t ON t.nombre = v.etiqueta;

-- Una abierta (Lucía: es "mi empresa asignada" en el panel de alumnado) y tres
-- cerradas, con los tres estados de contratación posterior: sí, no y sin decidir.
INSERT INTO asignacion (alumno_id, empresa_id, tutor_centro_id, grado_id, anio, fecha_inicio, fecha_fin, contratado_posterior, fecha_creacion) VALUES
    ((SELECT id FROM usuario WHERE correo = 'lucia@practikalia.test'),
     (SELECT id FROM empresa WHERE nombre = 'Grupo Ondara Software'),
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'),
     (SELECT id FROM grado WHERE nombre = 'DAW'), 2026, DATE '2026-03-09', NULL, NULL, NOW()),

    ((SELECT id FROM usuario WHERE correo = 'ivan@practikalia.test'),
     (SELECT id FROM empresa WHERE nombre = 'Grupo Ondara Software'),
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'),
     (SELECT id FROM grado WHERE nombre = 'DAW'), 2026, DATE '2026-03-09', DATE '2026-06-19', TRUE, NOW()),

    ((SELECT id FROM usuario WHERE correo = 'nerea@practikalia.test'),
     (SELECT id FROM empresa WHERE nombre = 'Clínica Dental Aljarafe'),
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'),
     (SELECT id FROM grado WHERE nombre = 'DAM'), 2026, DATE '2026-03-09', DATE '2026-06-19', FALSE, NOW()),

    ((SELECT id FROM usuario WHERE correo = 'hugo@practikalia.test'),
     (SELECT id FROM empresa WHERE nombre = 'Cerámicas Bailén'),
     (SELECT id FROM usuario WHERE correo = 'admin@practikalia.test'),
     (SELECT id FROM grado WHERE nombre = 'DAM'), 2026, DATE '2026-03-09', DATE '2026-06-19', NULL, NOW());

-- Una aprobada (se ve en la ficha de empresa) y dos pendientes (llenan la cola
-- de moderación del panel de profesorado).
INSERT INTO review (asignacion_id, autor_id, contenido, calificacion, estado, moderada_por_id, motivo_rechazo, fecha_creacion, fecha_moderacion) VALUES
    ((SELECT a.id FROM asignacion a JOIN usuario u ON u.id = a.alumno_id WHERE u.correo = 'ivan@practikalia.test'),
     (SELECT id FROM usuario WHERE correo = 'ivan@practikalia.test'),
     'Me dieron tareas reales desde la primera semana y siempre tuve a alguien a quien preguntar.',
     5, 'APROBADA',
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NULL, NOW(), NOW()),

    ((SELECT a.id FROM asignacion a JOIN usuario u ON u.id = a.alumno_id WHERE u.correo = 'nerea@practikalia.test'),
     (SELECT id FROM usuario WHERE correo = 'nerea@practikalia.test'),
     'Buen trato del equipo, aunque el horario cambiaba bastante de una semana a otra.',
     4, 'PENDIENTE', NULL, NULL, NOW(), NULL),

    ((SELECT a.id FROM asignacion a JOIN usuario u ON u.id = a.alumno_id WHERE u.correo = 'hugo@practikalia.test'),
     (SELECT id FROM usuario WHERE correo = 'hugo@practikalia.test'),
     'Faltó seguimiento por parte de la empresa, pero se aprende mucho del proceso de fábrica.',
     3, 'PENDIENTE', NULL, NULL, NOW(), NULL);

-- Intereses del alumnado en empresas a las que todavía no está asignado.
INSERT INTO interes (alumno_id, empresa_id, grado_id, anio, fecha_creacion)
SELECT u.id, e.id, u.grado_id, u.anio, NOW()
FROM (VALUES
    ('lucia@practikalia.test', 'Clínica Dental Aljarafe'),
    ('lucia@practikalia.test', 'Nexo Logística'),
    ('marta@practikalia.test', 'Grupo Ondara Software'),
    ('marta@practikalia.test', 'Nexo Logística')
) AS v(correo, empresa)
JOIN usuario u ON u.correo = v.correo
JOIN empresa e ON e.nombre = v.empresa;
