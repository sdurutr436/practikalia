-- El catalogo de etiquetas deja de ser una lista plana y pasa a ser un arbol de
-- tres niveles: sector -> actividad principal -> etiqueta. Una sola columna
-- (`padre_id`, contra la propia tabla) sostiene los tres, asi que no sale mas
-- caro que haber montado solo dos.
--
-- Los sectores siguen viviendo en esta misma tabla a proposito: `empresa.sector_id`
-- apunta aqui y la afinidad da bonus cuando un alumno marca el sector como
-- interes propio (`usuario_etiqueta`), cosa imposible con los sectores en una
-- tabla aparte.
ALTER TABLE etiqueta ADD COLUMN padre_id BIGINT REFERENCES etiqueta(id);

-- Raiz que NO es un sector: las modalidades de trabajo (Teletrabajo, Presencial,
-- Hibrido) valen para cualquier empresa, asi que ni se eligen como sector ni
-- cuelgan de uno. Sin esta marca, una raiz transversal y un sector serian
-- indistinguibles.
ALTER TABLE etiqueta ADD COLUMN transversal BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX idx_etiqueta_padre ON etiqueta(padre_id);
