-- Las horas de la planilla semanal pasan de DECIMAL(5,2) a DECIMAL(5,0).
--
-- Ya no se cargan horas fraccionadas (7.5, 7.25): siempre son un numero
-- entero. Los valores existentes se redondean solos al convertir la columna
-- (MySQL redondea al entero mas cercano).
--
-- Hay que correrlo A MANO sobre la base "escuela-empresa" antes de levantar la
-- version de la app con este cambio: spring.jpa.hibernate.ddl-auto=validate
-- se niega a arrancar si el tipo de la columna no coincide con el de la entidad.

ALTER TABLE planilla_semanal
    MODIFY COLUMN total_horas DECIMAL(5,0);

ALTER TABLE planilla_semanal_detalle
    MODIFY COLUMN horas DECIMAL(5,0);
