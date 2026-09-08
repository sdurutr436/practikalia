-- Coloca en el arbol las etiquetas que V13 y V15 dejaron planas. Solo se agrupa
-- lo evidente: la rama de informatica, que es la unica donde el catalogo de hoy
-- ya distingue sector, actividad y etiqueta. Los otros ocho sectores se quedan
-- sin actividades hasta que alguien las cree desde /sectores, que inventar aqui
-- el arbol de la sanidad o del transporte seria inventarse el dominio.

-- La raiz nueva de informatica y una actividad que faltaba; 'Modalidad de
-- trabajo' no es un sector, es el grupo transversal que vale para todas.
INSERT INTO etiqueta (nombre, transversal) VALUES
    ('Informática y comunicaciones', FALSE),
    ('Sistemas y redes', FALSE),
    ('Modalidad de trabajo', TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 'Desarrollo web' y 'Ciberseguridad' venian de V13/V15 haciendo de sector; se
-- convierten en actividades de informatica. Las empresas que las tienen en
-- `sector_id` siguen apuntando a una fila valida.
UPDATE etiqueta AS hija
SET padre_id = padre.id
FROM (VALUES
    ('Desarrollo web',      'Informática y comunicaciones'),
    ('Ciberseguridad',      'Informática y comunicaciones'),
    ('Sistemas y redes',    'Informática y comunicaciones'),
    ('Java',                'Desarrollo web'),
    ('Angular',             'Desarrollo web'),
    ('Python',              'Desarrollo web'),
    ('Bases de datos',      'Desarrollo web'),
    ('Redes',               'Sistemas y redes'),
    ('Atención al público', 'Comercio'),
    ('Teletrabajo',         'Modalidad de trabajo'),
    ('Presencial',          'Modalidad de trabajo'),
    ('Híbrido',             'Modalidad de trabajo')
) AS v(nombre_hija, nombre_padre)
JOIN etiqueta AS padre ON padre.nombre = v.nombre_padre
WHERE hija.nombre = v.nombre_hija;
