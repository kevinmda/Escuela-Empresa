-- Las horas de la planilla semanal pasan de FLOAT a DECIMAL(5,2).
--
-- Con FLOAT la suma de los dias se guardaba con error binario (7.5 + 8.25 daba
-- 15.749999) y eso era lo que terminaba en el PDF. DECIMAL(5,2) guarda exactamente
-- dos decimales, hasta 999.99.
--
-- Hay que correrlo A MANO sobre la base "escuela-empresa" antes de levantar la
-- version de la app que usa BigDecimal: spring.jpa.hibernate.ddl-auto=validate
-- se niega a arrancar si el tipo de la columna no coincide con el de la entidad.
-- Los valores existentes se convierten solos (MySQL redondea a dos decimales).

ALTER TABLE planilla_semanal
    MODIFY COLUMN total_horas DECIMAL(5,2);

ALTER TABLE planilla_semanal_detalle
    MODIFY COLUMN horas DECIMAL(5,2);
