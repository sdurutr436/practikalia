-- Amplía V13 con más reseñas (para probar el panel de moderación y la ficha
-- de empresa con contenido real) y dos alumnos nuevos para sostenerlas.
-- Mismo criterio que V13: dominio `.test`, ON CONFLICT DO NOTHING, claves
-- ajenas resueltas por clave natural.
--
-- Contraseña de las dos cuentas nuevas: Practikalia1!
-- Hashes BCrypt reales, generados igual que en V13.

INSERT INTO correo_permitido (correo) VALUES
    ('sara@practikalia.test'),
    ('pablo@practikalia.test')
ON CONFLICT (correo) DO NOTHING;

INSERT INTO usuario (correo, contrasena_hash, rol, es_admin, debe_cambiar_contrasena, activo, fecha_creacion, grado_id, anio) VALUES
    ('sara@practikalia.test',  '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAW'),  2026),
    ('pablo@practikalia.test', '$2a$10$8V.NeqP0K3T1EG5LPT11ZOw6IEtDL535440tf.yFlGR0ktyGukP0K', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'ASIR'), 2026)
ON CONFLICT (correo) DO NOTHING;

-- Sara en Nexo Logística (empresa sin alumnado asignado todavía en V13) y
-- Pablo en Cerámicas Bailén (segundo alumno de la misma empresa que Hugo).
INSERT INTO asignacion (alumno_id, empresa_id, tutor_centro_id, grado_id, anio, fecha_inicio, fecha_fin, contratado_posterior, fecha_creacion) VALUES
    ((SELECT id FROM usuario WHERE correo = 'sara@practikalia.test'),
     (SELECT id FROM empresa WHERE nombre = 'Nexo Logística'),
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'),
     (SELECT id FROM grado WHERE nombre = 'DAW'), 2026, DATE '2026-03-09', DATE '2026-06-19', NULL, NOW()),

    ((SELECT id FROM usuario WHERE correo = 'pablo@practikalia.test'),
     (SELECT id FROM empresa WHERE nombre = 'Cerámicas Bailén'),
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'),
     (SELECT id FROM grado WHERE nombre = 'ASIR'), 2026, DATE '2026-03-09', DATE '2026-06-19', FALSE, NOW());

-- Una pendiente más (tercera en la cola de moderación) y la primera
-- RECHAZADA de los datos de demostración — V13 no tenía ninguna.
INSERT INTO review (asignacion_id, autor_id, contenido, calificacion, estado, moderada_por_id, motivo_rechazo, fecha_creacion, fecha_moderacion) VALUES
    ((SELECT a.id FROM asignacion a JOIN usuario u ON u.id = a.alumno_id WHERE u.correo = 'sara@practikalia.test'),
     (SELECT id FROM usuario WHERE correo = 'sara@practikalia.test'),
     'Aprendí bastante de logística de almacén, aunque el primer mes fue solo observar.',
     4, 'PENDIENTE', NULL, NULL, NOW(), NULL),

    ((SELECT a.id FROM asignacion a JOIN usuario u ON u.id = a.alumno_id WHERE u.correo = 'pablo@practikalia.test'),
     (SELECT id FROM usuario WHERE correo = 'pablo@practikalia.test'),
     'No me dejaron acercarme a la línea de producción en ningún momento.',
     2, 'RECHAZADA',
     (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'),
     'Contenido a revisar con el alumno antes de publicar: contradice el informe de la empresa.',
     NOW(), NOW());
