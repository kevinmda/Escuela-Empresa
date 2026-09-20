-- Guarda cuál fue el último aviso del header (ver ChromeContext.Aviso) que
-- cada alumno marcó como leído, por tipo. "clave" identifica la ocurrencia
-- puntual (por ejemplo la fecha del sábado que activó el recordatorio de
-- planilla): si la próxima semana llega un aviso del mismo tipo pero con una
-- clave distinta, ya no coincide con lo guardado acá y vuelve a contar como
-- no leído sin necesidad de borrar ni actualizar esta fila.
CREATE TABLE IF NOT EXISTS aviso_leido (
    id_Al INT         NOT NULL,
    tipo  VARCHAR(20) NOT NULL,
    clave VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_Al, tipo),
    CONSTRAINT fk_aviso_leido_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al)
);
