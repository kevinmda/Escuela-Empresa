-- El coordinador de una especialidad es opcional (especialidad.id_Ad ya
-- admite NULL desde el V1), pero la FK no tenía ON DELETE: por defecto,
-- MySQL la trata como RESTRICT y no deja borrar la fila de administrador
-- mientras alguna especialidad todavía la tenga puesta como coordinador
-- -- el error que reportó el usuario al intentar borrar un coordinador.
--
-- Pasa a ON DELETE SET NULL: borrar al coordinador NO borra la
-- especialidad (sigue existiendo con sus alumnos, empresas y supervisores
-- intactos), simplemente la deja sin coordinador asignado, igual que si
-- nunca se hubiera puesto uno.
--
-- Borrar la especialidad en el otro sentido ya no estaba bloqueada por esta
-- relación (la FK vive en especialidad, no en administrador); si hace falta
-- borrarla sigue dependiendo de que no tenga alumnos, empresas o
-- supervisores asignados, que es un tema aparte.
-- El nombre real de la FK puede no ser el que pondría este script (ver
-- V10__alumno_borra_en_cascada.sql), así que se busca en information_schema
-- en vez de asumirlo.
SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'especialidad'
      AND COLUMN_NAME = 'id_Ad' AND REFERENCED_TABLE_NAME = 'administrador' LIMIT 1);
SET @sql := CONCAT('ALTER TABLE especialidad DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE especialidad
    ADD CONSTRAINT fk_especialidad_administrador
    FOREIGN KEY (id_Ad) REFERENCES administrador (id_Ad) ON DELETE SET NULL;
