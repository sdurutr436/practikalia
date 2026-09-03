-- Configuración de la instancia: nombre y logo del centro. Practikalia se
-- despliega por instituto, no es multi-centro, así que esta tabla tiene
-- exactamente una fila (id fijo, sin generador) que el PUT actualiza; no hay
-- POST que cree centros ni DELETE.
CREATE TABLE centro (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    logo VARCHAR(255)
);

INSERT INTO centro (id, nombre, logo) VALUES (1, 'Practikalia', NULL);
