-- Hasta ahora, borrar la fila de un alumno a mano (por ejemplo desde
-- phpMyAdmin) tiraba el error de MySQL #1451 (clave foránea) en cuanto ese
-- alumno tenía algún documento subido, alguna planilla semanal o alguna
-- opinión cargada: esas tablas apuntan a alumno con una FK sin ON DELETE
-- CASCADE, así que había que borrar primero, a mano, cada tabla hija antes
-- de poder borrar al alumno. De acá en más, borrar la fila de alumno borra
-- automáticamente todo lo que depende de él.

-- El nombre de cada FK no es siempre el que pondría este mismo script: bases
-- cargadas a mano desde un diagrama (MySQL Workbench, phpMyAdmin, etc.)
-- suelen traer constraints con otros nombres (ej. fk_Documento_Scan_Alumno1
-- en vez de fk_documento_alumno) aunque apunten a las mismas columnas. Para
-- no depender de con qué herramienta se creó cada base, el nombre real se
-- busca en information_schema antes de borrar la FK.
SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documento_scan'
      AND COLUMN_NAME = 'id_Al' AND REFERENCED_TABLE_NAME = 'alumno' LIMIT 1);
SET @sql := CONCAT('ALTER TABLE documento_scan DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE documento_scan
    ADD CONSTRAINT fk_documento_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al) ON DELETE CASCADE;

SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'planilla_semanal'
      AND COLUMN_NAME = 'id_Al' AND REFERENCED_TABLE_NAME = 'alumno' LIMIT 1);
SET @sql := CONCAT('ALTER TABLE planilla_semanal DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE planilla_semanal
    ADD CONSTRAINT fk_planilla_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al) ON DELETE CASCADE;

-- El detalle día por día de cada planilla también tiene que irse cuando se
-- borra la planilla (y la planilla, a su vez, cuando se borra el alumno).
SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'planilla_semanal_detalle'
      AND COLUMN_NAME = 'id_PS' AND REFERENCED_TABLE_NAME = 'planilla_semanal' LIMIT 1);
SET @sql := CONCAT('ALTER TABLE planilla_semanal_detalle DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE planilla_semanal_detalle
    ADD CONSTRAINT fk_detalle_planilla FOREIGN KEY (id_PS) REFERENCES planilla_semanal (id_PS) ON DELETE CASCADE;

SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'opinion_empresa'
      AND COLUMN_NAME = 'id_Al' AND REFERENCED_TABLE_NAME = 'alumno' LIMIT 1);
SET @sql := CONCAT('ALTER TABLE opinion_empresa DROP FOREIGN KEY `', @fk, '`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE opinion_empresa
    ADD CONSTRAINT fk_opinion_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al) ON DELETE CASCADE;

-- La cuenta de usuario (login) de un alumno va en el sentido contrario: es
-- alumno.id_Usr el que apunta a usuario.id_Usr, no al revés. Una FK con ON
-- DELETE CASCADE ahí borraría al alumno cuando se borra el usuario, que es
-- lo opuesto de lo que queremos (borrar el usuario cuando se borra el
-- alumno). Por eso esto se resuelve con un trigger y no con la FK.
DROP TRIGGER IF EXISTS trg_alumno_borra_usuario;

CREATE TRIGGER trg_alumno_borra_usuario
    AFTER DELETE ON alumno
    FOR EACH ROW
BEGIN
    IF OLD.id_Usr IS NOT NULL THEN
        DELETE FROM usuario WHERE id_Usr = OLD.id_Usr;
    END IF;
END;
