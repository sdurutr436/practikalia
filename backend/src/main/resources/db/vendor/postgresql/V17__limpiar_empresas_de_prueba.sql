-- Borra las empresas que quedaron de probar la aplicación a mano ("Acme Corp",
-- "F2 Test SL", "No publicada"): ensucian el catálogo de demostración, y sus
-- imágenes apuntaban a ficheros de `uploads` que ya no existen.
--
-- En una instalación que no las tenga, cada sentencia no encuentra nada y no
-- borra nada. Se limpia primero lo que cuelga de ellas porque las claves
-- ajenas hacia `empresa` no tienen ON DELETE CASCADE.

CREATE TEMPORARY TABLE empresas_de_prueba ON COMMIT DROP AS
SELECT id FROM empresa WHERE nombre IN ('Acme Corp', 'F2 Test SL', 'No publicada');

DELETE FROM review
WHERE asignacion_id IN (
    SELECT a.id FROM asignacion a WHERE a.empresa_id IN (SELECT id FROM empresas_de_prueba));

DELETE FROM asignacion WHERE empresa_id IN (SELECT id FROM empresas_de_prueba);
DELETE FROM interes WHERE empresa_id IN (SELECT id FROM empresas_de_prueba);
DELETE FROM empresa_etiqueta WHERE empresa_id IN (SELECT id FROM empresas_de_prueba);
DELETE FROM empresa WHERE id IN (SELECT id FROM empresas_de_prueba);
