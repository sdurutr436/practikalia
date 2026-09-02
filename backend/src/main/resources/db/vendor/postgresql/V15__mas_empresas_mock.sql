-- Amplía el catálogo de empresas de V13 hasta pasar de una página: con 9 por
-- página, 14 empresas (10 publicadas y 4 sin publicar) dan dos páginas en
-- "Todas" y en "Publicadas", y una sola en "Sin publicar".
-- Mismo criterio que V13/V14: dominio `.test`, ON CONFLICT DO NOTHING, claves
-- ajenas resueltas por clave natural. Sin alumnado, asignaciones ni reseñas
-- nuevas: esto es solo catálogo para ver el listado, la búsqueda y los filtros.

-- Sectores y etiquetas nuevos, para que los filtros avanzados tengan variedad.
INSERT INTO etiqueta (nombre) VALUES
    ('Comercio'),
    ('Hostelería'),
    ('Educación'),
    ('Energía'),
    ('Diseño gráfico'),
    ('Python'),
    ('Redes'),
    ('Ciberseguridad')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO empresa (nombre, descripcion, direccion, sector_id, observaciones, contacto_nombre, contacto_telefono, contacto_email, publicada, creada_por_id, fecha_creacion) VALUES
    ('Alcores Ferretería',
     'Ferretería con tres tiendas en la comarca. Prácticas en almacén, atención al público y pedidos web.',
     'Calle Real 41, Alcalá de Guadaíra',
     (SELECT id FROM etiqueta WHERE nombre = 'Comercio'),
     'Una plaza. Buscan a alguien que se maneje con el TPV.',
     'Manuel Alcores', '955 61 62 63', 'tienda@alcoresferreteria.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Bahía Solar',
     'Instaladora de fotovoltaica residencial. El alumnado acompaña a los equipos de montaje y hace el registro de partes.',
     'Polígono Guadalquivir, nave 7, Coria del Río',
     (SELECT id FROM etiqueta WHERE nombre = 'Energía'),
     'Dos plazas. Imprescindible carné de conducir.',
     'Elena Bahía', '954 71 72 73', 'rrhh@bahiasolar.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'admin@practikalia.test'), NOW()),

    ('Botica del Guadaíra',
     'Farmacia de barrio con laboratorio de formulación magistral propio.',
     'Plaza del Cabildo 2, Alcalá de Guadaíra',
     (SELECT id FROM etiqueta WHERE nombre = 'Sanidad'),
     'Una plaza, turno partido. Piden discreción con los datos de pacientes.',
     'Rocío Guadaíra', '955 68 69 70', 'farmacia@boticaguadaira.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Hostal Puerta Osario',
     'Hostal de 22 habitaciones en el centro. Recepción, reservas online y atención al cliente.',
     'Calle Puerta Osario 15, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Hostelería'),
     'Dos plazas. Se valora inglés hablado.',
     'Curro Jiménez', '954 41 42 43', 'reservas@puertaosario.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Ludoteca Triana',
     'Centro de refuerzo y ocio educativo para primaria. Apoyo en aula y preparación de material.',
     'Calle Pagés del Corro 120, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Educación'),
     'Una plaza de tarde. Exige certificado de delitos sexuales.',
     'Inma Triana', '954 33 21 10', 'coordinacion@ludotecatriana.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Marisma Redes',
     'Integradora de redes y sistemas para pymes. Cableado, configuración de switches y soporte de primer nivel.',
     'Avenida de la Innovación 4, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Desarrollo web'),
     'Dos plazas. Es la empresa que más ASIR ha contratado los últimos años.',
     'Fran Marisma', '955 02 03 04', 'sistemas@marismaredes.test',
     TRUE, (SELECT id FROM usuario WHERE correo = 'admin@practikalia.test'), NOW()),

    ('Estudio Cal y Canto',
     'Estudio de diseño gráfico y marca. Prácticas en maquetación y piezas para redes.',
     'Calle Feria 88, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Diseño gráfico'),
     'Convenio pendiente de firma para el próximo curso, no publicar todavía.',
     'Nuria Cal', '954 90 91 92', 'hola@calycanto.test',
     FALSE, (SELECT id FROM usuario WHERE correo = 'robles@practikalia.test'), NOW()),

    ('Guadalquivir Ciberseguridad',
     'Consultora de seguridad ofensiva. Interesada en alumnado de segundo con base de redes.',
     'Calle Arquitectura 9, Sevilla',
     (SELECT id FROM etiqueta WHERE nombre = 'Ciberseguridad'),
     'Visitados en junio: quieren empezar en 2027, aún sin convenio.',
     'Dani Guadalquivir', '955 10 11 12', 'contacto@guadalquivirsec.test',
     FALSE, (SELECT id FROM usuario WHERE correo = 'admin@practikalia.test'), NOW());

INSERT INTO empresa_etiqueta (empresa_id, etiqueta_id)
SELECT e.id, t.id
FROM (VALUES
    ('Alcores Ferretería',           'Presencial'),
    ('Alcores Ferretería',           'Atención al público'),
    ('Bahía Solar',                  'Presencial'),
    ('Botica del Guadaíra',          'Presencial'),
    ('Botica del Guadaíra',          'Atención al público'),
    ('Hostal Puerta Osario',         'Presencial'),
    ('Hostal Puerta Osario',         'Atención al público'),
    ('Ludoteca Triana',              'Presencial'),
    ('Marisma Redes',                'Híbrido'),
    ('Marisma Redes',                'Redes'),
    ('Marisma Redes',                'Bases de datos'),
    ('Estudio Cal y Canto',          'Teletrabajo'),
    ('Estudio Cal y Canto',          'Diseño gráfico'),
    ('Guadalquivir Ciberseguridad',  'Teletrabajo'),
    ('Guadalquivir Ciberseguridad',  'Redes'),
    ('Guadalquivir Ciberseguridad',  'Python')
) AS v(empresa, etiqueta)
JOIN empresa e ON e.nombre = v.empresa
JOIN etiqueta t ON t.nombre = v.etiqueta;
