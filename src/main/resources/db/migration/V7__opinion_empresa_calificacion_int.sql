-- La entidad mapea "calificacion" (int de Java) a INTEGER, pero V6 la creó
-- como TINYINT: Hibernate con ddl-auto=validate rechaza el arranque porque
-- el tipo de columna no coincide con lo que espera la entidad. El CHECK de
-- 1 a 5 sigue intacto -- MODIFY solo cambia el tipo de la columna.
ALTER TABLE opinion_empresa MODIFY calificacion INT NOT NULL;
