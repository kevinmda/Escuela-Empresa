-- Opinión que un alumno deja sobre la empresa de su pasantía: comentario libre
-- + calificación de 1 a 5 estrellas. La ve el coordinador de su especialidad y
-- el administrativo; el alumno puede borrar la suya.
CREATE TABLE opinion_empresa (
    id_Opi         INT AUTO_INCREMENT PRIMARY KEY,
    id_Al          INT NOT NULL,
    id_Emp         INT,
    comentario     VARCHAR(1000) NOT NULL,
    calificacion   TINYINT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    CONSTRAINT fk_opinion_alumno  FOREIGN KEY (id_Al)  REFERENCES alumno (id_Al),
    CONSTRAINT fk_opinion_empresa FOREIGN KEY (id_Emp) REFERENCES empresa (id_Emp),
    CONSTRAINT chk_opinion_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);
