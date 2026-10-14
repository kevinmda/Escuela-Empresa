-- Autorización que da el propio alumno (casilla en su Inicio) para que sus
-- datos sigan en el sistema cuando termine la pasantía, en vez de borrarse.
-- Arranca en FALSE para todos los alumnos existentes: nadie queda marcado
-- como egresado sin haberlo elegido primero.
ALTER TABLE alumno
    ADD COLUMN autoriza_conservar_datos BOOLEAN NOT NULL DEFAULT FALSE;
