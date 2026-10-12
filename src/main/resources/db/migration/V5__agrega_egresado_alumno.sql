-- Marca si un alumno ya egreso (termino sus pasantias), para poder
-- filtrarlos sin borrarlos de la base. Arranca en FALSE para todos los
-- alumnos existentes.
ALTER TABLE alumno
    ADD COLUMN egresado BOOLEAN NOT NULL DEFAULT FALSE;
