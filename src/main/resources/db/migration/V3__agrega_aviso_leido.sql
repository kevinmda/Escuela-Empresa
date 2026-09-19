-- Guarda cuál fue el último aviso del header (ver ChromeContext.Aviso) que
-- cada alumno marcó como leído, por código. "clave" identifica la ocurrencia
-- puntual (por ejemplo la fecha del sábado que activó un recordatorio, o el
-- id del documento que completó un umbral): si más adelante llega un aviso
-- del mismo código pero con una clave distinta, ya no coincide con lo
-- guardado acá y vuelve a contar como no leído sin necesidad de borrar ni
-- actualizar esta fila.
CREATE TABLE IF NOT EXISTS aviso_leido (
    id_Al  INT         NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    clave  VARCHAR(30) NOT NULL,
    PRIMARY KEY (id_Al, codigo),
    CONSTRAINT fk_aviso_leido_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al)
);
