-- Nullable: las cuentas creadas desde POST /api/usuarios no tienen estos datos
-- y no hay nada que retro-rellenar. Solo el auto-registro los aporta.
ALTER TABLE usuario ADD COLUMN nombre VARCHAR(255);
ALTER TABLE usuario ADD COLUMN apellido1 VARCHAR(255);
ALTER TABLE usuario ADD COLUMN apellido2 VARCHAR(255);
ALTER TABLE usuario ADD COLUMN dni VARCHAR(9) UNIQUE;
