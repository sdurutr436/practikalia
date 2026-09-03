-- El tutor de clase. Vive en la clase y no en el profesor porque la relacion es
-- 1-1 por los dos lados: una clase tiene como mucho un tutor y un profesor
-- tutoriza como mucho una clase, y eso el UNIQUE lo garantiza aqui y no en
-- codigo. Los alumnos de la clase heredan su tutor sin guardar nada por alumno.
ALTER TABLE grado ADD COLUMN tutor_id BIGINT UNIQUE REFERENCES usuario(id);
