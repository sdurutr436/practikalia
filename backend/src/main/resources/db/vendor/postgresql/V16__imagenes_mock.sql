-- Imagen de demostración para las 14 empresas de V13/V15. No son subidas
-- reales: son SVG versionados en `frontend/public/demo-empresas/`, que nginx
-- sirve desde la raíz igual que el resto del frontend. Así los datos de
-- demostración se ven igual en cualquier clon, sin depender del volumen
-- `uploads_data`, que es local a cada instalación y no está en el repo.
--
-- Una imagen subida de verdad guarda aquí `/uploads/empresas/<uuid>.<ext>`;
-- por eso el UPDATE respeta las que ya tengan algo.

UPDATE empresa e
SET imagen = v.imagen
FROM (VALUES
    ('Grupo Ondara Software',       '/demo-empresas/grupo-ondara-software.svg'),
    ('Clínica Dental Aljarafe',     '/demo-empresas/clinica-dental-aljarafe.svg'),
    ('Cerámicas Bailén',            '/demo-empresas/ceramicas-bailen.svg'),
    ('Nexo Logística',              '/demo-empresas/nexo-logistica.svg'),
    ('Almazara Vega Sur',           '/demo-empresas/almazara-vega-sur.svg'),
    ('Datavera Analítica',          '/demo-empresas/datavera-analitica.svg'),
    ('Alcores Ferretería',          '/demo-empresas/alcores-ferreteria.svg'),
    ('Bahía Solar',                 '/demo-empresas/bahia-solar.svg'),
    ('Botica del Guadaíra',         '/demo-empresas/botica-del-guadaira.svg'),
    ('Hostal Puerta Osario',        '/demo-empresas/hostal-puerta-osario.svg'),
    ('Ludoteca Triana',             '/demo-empresas/ludoteca-triana.svg'),
    ('Marisma Redes',               '/demo-empresas/marisma-redes.svg'),
    ('Estudio Cal y Canto',         '/demo-empresas/estudio-cal-y-canto.svg'),
    ('Guadalquivir Ciberseguridad', '/demo-empresas/guadalquivir-ciberseguridad.svg')
) AS v(nombre, imagen)
WHERE e.nombre = v.nombre AND e.imagen IS NULL;
