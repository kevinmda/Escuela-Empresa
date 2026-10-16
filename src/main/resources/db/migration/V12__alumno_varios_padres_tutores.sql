-- Antes, un alumno podía tener relacionado como máximo un padre/tutor
-- (alumno.id_PT -> padre_tutor.id_PT). Ahora puede tener varios: se da
-- vuelta la relación para que viva en padre_tutor (padre_tutor.id_Al ->
-- alumno.id_Al), así un mismo alumno puede tener tantas filas de
-- padre_tutor como necesite, en vez de una sola.

-- 1) Columna nueva en padre_tutor, todavía sin la FK: se agrega después de
--    migrar los datos para no pelear con filas que momentáneamente no
--    tienen id_Al puesto.
ALTER TABLE padre_tutor ADD COLUMN id_Al INT NULL;

-- 2) Migra la relación existente: el padre/tutor que ya tenía puesto cada
--    alumno pasa a apuntar hacia ese alumno en el sentido nuevo.
UPDATE padre_tutor pt
    JOIN alumno a ON a.id_PT = pt.id_PT
    SET pt.id_Al = a.id_Al;

-- 3) FK con ON DELETE CASCADE: si se borra el alumno (ver V10), sus
--    padres/tutores se van con él, igual que el resto de sus datos.
ALTER TABLE padre_tutor
    ADD CONSTRAINT fk_padre_tutor_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al) ON DELETE CASCADE;

-- 4) La columna vieja en alumno ya no hace falta.
ALTER TABLE alumno DROP FOREIGN KEY fk_alumno_padre_tutor;
ALTER TABLE alumno DROP COLUMN id_PT;
