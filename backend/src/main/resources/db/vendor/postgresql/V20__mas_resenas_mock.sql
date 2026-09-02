-- Amplia los datos de demostracion para que las tres pastillas de la pantalla
-- de moderacion salgan pobladas: 12 pendientes, 14 aprobadas y 5 rechazadas,
-- contando las 5 que ya venian de V13 y V14.
--
-- Ademas rellena nombre y apellidos del alumnado mock (columnas nuevas, que
-- hasta ahora solo llenaba el auto-registro): la cola de moderacion ensena el
-- nombre junto al correo, y sin esto solo se veria el correo.
--
-- Contrasena de las cuentas nuevas: Practikalia1! (mismo hash BCrypt que V14).

UPDATE usuario SET nombre = datos.nombre, apellido1 = datos.apellido1, apellido2 = datos.apellido2
FROM (VALUES
    ('lucia@practikalia.test', 'Lucía', 'Ramírez', 'Ortega'),
    ('ivan@practikalia.test', 'Iván', 'Cabrera', 'Soto'),
    ('nerea@practikalia.test', 'Nerea', 'Vilches', 'Mena'),
    ('hugo@practikalia.test', 'Hugo', 'Salas', 'Peral'),
    ('marta@practikalia.test', 'Marta', 'Bermejo', 'Nieto'),
    ('sara@practikalia.test', 'Sara', 'Quintana', 'Ruiz'),
    ('pablo@practikalia.test', 'Pablo', 'Herrera', 'Lozano')
) AS datos(correo, nombre, apellido1, apellido2)
WHERE usuario.correo = datos.correo;

INSERT INTO correo_permitido (correo) VALUES
    ('carmen@practikalia.test'),
    ('adrian@practikalia.test'),
    ('noelia@practikalia.test'),
    ('jorge@practikalia.test'),
    ('alba@practikalia.test'),
    ('ruben@practikalia.test'),
    ('elena@practikalia.test')
ON CONFLICT (correo) DO NOTHING;

INSERT INTO usuario (correo, contrasena_hash, rol, es_admin, debe_cambiar_contrasena, activo, fecha_creacion, grado_id, anio, nombre, apellido1, apellido2) VALUES
    ('carmen@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAW'), 2026, 'Carmen', 'Delgado', 'Pinto'),
    ('adrian@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAM'), 2026, 'Adrián', 'Mota', 'Vega'),
    ('noelia@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'ASIR'), 2026, 'Noelia', 'Cuesta', 'Aranda'),
    ('jorge@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAW'), 2026, 'Jorge', 'Padilla', 'Rincón'),
    ('alba@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAM'), 2026, 'Alba', 'Carmona', 'Espejo'),
    ('ruben@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'ASIR'), 2026, 'Rubén', 'Ibáñez', 'Prado'),
    ('elena@practikalia.test', '$2a$10$b1P90WsRXJOgCboDn8e4xOS6K/dCnPKSSJv7g6jCXe1yzLbknOPaO', 'ALUMNO', FALSE, FALSE, TRUE, NOW(), (SELECT id FROM grado WHERE nombre = 'DAW'), 2026, 'Elena', 'Montes', 'Gálvez')
ON CONFLICT (correo) DO NOTHING;

-- Una resena cuelga siempre de una asignacion, y solo se admite una resena por
-- asignacion: cada fila de aqui necesita su propio par alumno/empresa.
CREATE TEMPORARY TABLE resenas_mock (
    correo TEXT, empresa TEXT, calificacion INT, estado VARCHAR(20), contenido TEXT, motivo TEXT
) ON COMMIT DROP;

INSERT INTO resenas_mock (correo, empresa, calificacion, estado, contenido, motivo) VALUES
    ('lucia@practikalia.test', 'Alcores Ferretería', 4, 'PENDIENTE', 'Me enseñaron el almacén entero la primera semana y luego ya tuve mis propias tareas de mostrador.', NULL),
    ('lucia@practikalia.test', 'Bahía Solar', 5, 'APROBADA', 'Salí a instalaciones reales desde el segundo mes, con un técnico al lado explicando cada paso.', NULL),
    ('ivan@practikalia.test', 'Botica del Guadaíra', 4, 'APROBADA', 'Mucho trato con el público y con receta electrónica; se aprende a ir rápido sin equivocarse.', NULL),
    ('nerea@practikalia.test', 'Hostal Puerta Osario', 3, 'PENDIENTE', 'El trabajo está bien, pero los turnos cambiaban con muy poca antelación.', NULL),
    ('hugo@practikalia.test', 'Ludoteca Triana', 5, 'APROBADA', 'El equipo me dejó llevar un taller yo solo al final de las prácticas. Se nota que confían.', NULL),
    ('marta@practikalia.test', 'Marisma Redes', 4, 'PENDIENTE', 'Toqué cableado, switches y algo de monitorización. Faltaron más prácticas de configuración.', NULL),
    ('marta@practikalia.test', 'Estudio Cal y Canto', 5, 'APROBADA', 'Entré en el proceso completo de un encargo, desde el boceto hasta la entrega al cliente.', NULL),
    ('sara@practikalia.test', 'Guadalquivir Ciberseguridad', 5, 'APROBADA', 'Auditorías reales con supervisión constante. Es donde más he aprendido hasta ahora.', NULL),
    ('pablo@practikalia.test', 'Almazara Vega Sur', 1, 'RECHAZADA', 'No hice nada en dos meses, un desastre de sitio y la gente ni te habla.', 'Contenido sin concretar: no describe tareas ni seguimiento, hay que revisarlo con el alumno.'),
    ('carmen@practikalia.test', 'Grupo Ondara Software', 5, 'APROBADA', 'Revisiones de código cada semana y una mentora asignada desde el primer día.', NULL),
    ('carmen@practikalia.test', 'Datavera Analítica', 4, 'PENDIENTE', 'Mucho SQL y algo de visualización. El ritmo es alto, pero se lleva bien.', NULL),
    ('adrian@practikalia.test', 'Clínica Dental Aljarafe', 4, 'APROBADA', 'Gestión de citas y trato con pacientes; me tocó aprender a organizarme muy rápido.', NULL),
    ('adrian@practikalia.test', 'Alcores Ferretería', 3, 'PENDIENTE', 'Se aprende del almacén, pero repetí la misma tarea demasiadas semanas seguidas.', NULL),
    ('noelia@practikalia.test', 'Cerámicas Bailén', 4, 'APROBADA', 'Vi la línea de producción de principio a fin y me explicaron cada control de calidad.', NULL),
    ('noelia@practikalia.test', 'Nexo Logística', 2, 'RECHAZADA', 'El encargado no aparecía nunca y al final hacía lo que me daba la gana.', 'Falta detalle sobre las tareas reales; se le pide reescribirla antes de publicarla.'),
    ('jorge@practikalia.test', 'Bahía Solar', 4, 'PENDIENTE', 'Buen ambiente y trabajo variado, aunque las jornadas de campo se hacen largas en verano.', NULL),
    ('jorge@practikalia.test', 'Botica del Guadaíra', 5, 'APROBADA', 'Me dieron responsabilidad real con el inventario y me corrigieron con paciencia.', NULL),
    ('alba@practikalia.test', 'Hostal Puerta Osario', 4, 'APROBADA', 'Recepción, reservas y algo de administración. Sales sabiendo tratar con cualquiera.', NULL),
    ('alba@practikalia.test', 'Ludoteca Triana', 5, 'PENDIENTE', 'Trabajar con niños cansa, pero el equipo te apoya en todo momento y se aprende muchísimo.', NULL),
    ('ruben@practikalia.test', 'Marisma Redes', 4, 'APROBADA', 'Montajes de red en cliente final, siempre acompañado y con material actualizado.', NULL),
    ('ruben@practikalia.test', 'Estudio Cal y Canto', 2, 'RECHAZADA', 'Me pusieron a hacer recados y fotocopias, esto no es diseño ni es nada.', 'El tono no encaja con una reseña pública del centro. Reformular describiendo los hechos.'),
    ('elena@practikalia.test', 'Guadalquivir Ciberseguridad', 5, 'APROBADA', 'Formación interna todas las semanas y un plan de prácticas escrito desde el principio.', NULL),
    ('elena@practikalia.test', 'Almazara Vega Sur', 3, 'PENDIENTE', 'El proceso de la aceituna es interesante, pero la campaña deja poco tiempo para enseñar.', NULL),
    ('carmen@practikalia.test', 'Nexo Logística', 4, 'APROBADA', 'Rutas, albaranes y trato con transportistas. Muy organizado todo.', NULL),
    ('jorge@practikalia.test', 'Datavera Analítica', 3, 'PENDIENTE', 'Se trabaja con datos de verdad, aunque al principio cuesta entender el modelo.', NULL),
    ('alba@practikalia.test', 'Grupo Ondara Software', 1, 'RECHAZADA', 'Perdí el tiempo, no me enseñaron nada y encima el tutor pasaba de mí.', 'Acusación personal sin datos que la sostengan; debe reescribirse centrada en las prácticas.');

INSERT INTO asignacion (alumno_id, empresa_id, tutor_centro_id, grado_id, anio, fecha_inicio, fecha_fin, contratado_posterior, fecha_creacion)
SELECT u.id, e.id, t.id, u.grado_id, u.anio, DATE '2026-03-09', DATE '2026-06-19', NULL, NOW()
FROM resenas_mock r
JOIN usuario u ON u.correo = r.correo
JOIN empresa e ON e.nombre = r.empresa
CROSS JOIN (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test') AS t
ON CONFLICT (alumno_id, empresa_id, grado_id, anio) DO NOTHING;

-- Fechas escalonadas hacia atras para que el orden por fecha descendente de la
-- cola de moderacion no salga arbitrario.
INSERT INTO review (asignacion_id, autor_id, contenido, calificacion, estado, moderada_por_id, motivo_rechazo, fecha_creacion, fecha_moderacion)
SELECT a.id, u.id, r.contenido, r.calificacion, r.estado,
       CASE WHEN r.estado <> 'PENDIENTE' THEN (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test') END,
       r.motivo,
       NOW() - (ROW_NUMBER() OVER (ORDER BY u.correo, e.nombre)) * INTERVAL '1 day',
       CASE WHEN r.estado <> 'PENDIENTE' THEN NOW() END
FROM resenas_mock r
JOIN usuario u ON u.correo = r.correo
JOIN empresa e ON e.nombre = r.empresa
JOIN asignacion a ON a.alumno_id = u.id AND a.empresa_id = e.id
ON CONFLICT (asignacion_id) DO NOTHING;
