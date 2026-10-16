-- Hasta ahora, borrar la fila de un alumno a mano (por ejemplo desde
-- phpMyAdmin) tiraba el error de MySQL #1451 (clave foránea) en cuanto ese
-- alumno tenía algún documento subido, alguna planilla semanal o alguna
-- opinión cargada: esas tablas apuntan a alumno con una FK sin ON DELETE
-- CASCADE, así que había que borrar primero, a mano, cada tabla hija antes
-- de poder borrar al alumno. De acá en más, borrar la fila de alumno borra
-- automáticamente todo lo que depende de él.

ALTER TABLE documento_scan DROP FOREIGN KEY fk_documento_alumno;
ALTER TABLE documento_scan
    ADD CONSTRAINT fk_documento_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al) ON DELETE CASCADE;

ALTER TABLE planilla_semanal DROP FOREIGN KEY fk_planilla_alumno;
ALTER TABLE planilla_semanal
    ADD CONSTRAINT fk_planilla_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al) ON DELETE CASCADE;

-- El detalle día por día de cada planilla también tiene que irse cuando se
-- borra la planilla (y la planilla, a su vez, cuando se borra el alumno).
ALTER TABLE planilla_semanal_detalle DROP FOREIGN KEY fk_detalle_planilla;
ALTER TABLE planilla_semanal_detalle
    ADD CONSTRAINT fk_detalle_planilla FOREIGN KEY (id_PS) REFERENCES planilla_semanal (id_PS) ON DELETE CASCADE;

ALTER TABLE opinion_empresa DROP FOREIGN KEY fk_opinion_alumno;
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
