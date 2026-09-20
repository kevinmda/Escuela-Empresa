-- V3 pasó a llamarse "codigo" a lo que originalmente era "tipo" (ver
-- AvisoLeido.java), pero la producción ya tenía V3 aplicada con el nombre
-- viejo. Como no se debe editar una migración ya aplicada, el cambio real de
-- esquema se hace acá, en una migración nueva.
ALTER TABLE aviso_leido
    CHANGE COLUMN tipo codigo VARCHAR(30) NOT NULL,
    MODIFY COLUMN clave VARCHAR(30) NOT NULL;
