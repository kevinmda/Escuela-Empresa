-- Esquema inicial. Espejo de db/schema.sql (que sigue existiendo para instalar
-- a mano sin Flyway), pero esta es la version que corre sola al arrancar la app.
--
-- En una base que YA tenia este esquema (creada antes de Flyway, con
-- db/schema.sql a mano), Flyway no ejecuta este archivo: lo marca como ya
-- aplicado (ver spring.flyway.baseline-on-migrate). Solo corre de verdad
-- contra una base nueva y vacia.

CREATE TABLE usuario (
    id_Usr                 INT AUTO_INCREMENT PRIMARY KEY,
    ci                     VARCHAR(255),
    contrasena             VARCHAR(255) NOT NULL,
    email                  VARCHAR(255) NOT NULL UNIQUE,
    activo                 BOOLEAN NOT NULL DEFAULT FALSE,
    token_activacion       VARCHAR(100),
    token_expiracion       DATETIME,
    intentos_codigo        INT NOT NULL DEFAULT 0,
    intentos_login         INT NOT NULL DEFAULT 0,
    bloqueado_hasta        DATETIME,
    contrasena_por_defecto BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE administrador (
    id_Ad     INT AUTO_INCREMENT PRIMARY KEY,
    nombres   VARCHAR(100),
    apellidos VARCHAR(100),
    ci        VARCHAR(25),
    telefono  VARCHAR(35),
    email     VARCHAR(100),
    cargo     VARCHAR(45),
    id_Usr    INT UNIQUE,
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_Usr) REFERENCES usuario (id_Usr)
);

CREATE TABLE especialidad (
    id_Esp INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    id_Ad  INT UNIQUE,
    CONSTRAINT fk_especialidad_administrador FOREIGN KEY (id_Ad) REFERENCES administrador (id_Ad)
);

CREATE TABLE empresa (
    id_Emp    INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(100),
    ruc       VARCHAR(25),
    telefono  VARCHAR(35),
    email     VARCHAR(100),
    direccion VARCHAR(100),
    id_Esp    INT,
    CONSTRAINT fk_empresa_especialidad FOREIGN KEY (id_Esp) REFERENCES especialidad (id_Esp)
);

CREATE TABLE supervisor (
    id_Sup    INT AUTO_INCREMENT PRIMARY KEY,
    nombres   VARCHAR(100),
    apellidos VARCHAR(100),
    email     VARCHAR(100),
    id_Esp    INT,
    CONSTRAINT fk_supervisor_especialidad FOREIGN KEY (id_Esp) REFERENCES especialidad (id_Esp)
);

CREATE TABLE padre_tutor (
    id_PT     INT AUTO_INCREMENT PRIMARY KEY,
    nombres   VARCHAR(100),
    apellidos VARCHAR(100),
    ci        VARCHAR(25),
    email     VARCHAR(100)
);

CREATE TABLE alumno (
    id_Al     INT AUTO_INCREMENT PRIMARY KEY,
    nombres   VARCHAR(100),
    apellidos VARCHAR(100),
    ci        VARCHAR(25),
    sexo      VARCHAR(15),
    fechaNac  DATE,
    telefono  VARCHAR(35),
    email     VARCHAR(100),
    curso     VARCHAR(45),
    seccion   VARCHAR(45),
    id_Usr    INT UNIQUE,
    id_Esp    INT,
    id_Emp    INT,
    id_PT     INT,
    id_Sup    INT,
    CONSTRAINT fk_alumno_usuario      FOREIGN KEY (id_Usr) REFERENCES usuario (id_Usr),
    CONSTRAINT fk_alumno_especialidad FOREIGN KEY (id_Esp) REFERENCES especialidad (id_Esp),
    CONSTRAINT fk_alumno_empresa      FOREIGN KEY (id_Emp) REFERENCES empresa (id_Emp),
    CONSTRAINT fk_alumno_padre_tutor  FOREIGN KEY (id_PT)  REFERENCES padre_tutor (id_PT),
    CONSTRAINT fk_alumno_supervisor   FOREIGN KEY (id_Sup) REFERENCES supervisor (id_Sup)
);

CREATE TABLE planilla_semanal (
    id_PS         INT AUTO_INCREMENT PRIMARY KEY,
    supervisor    VARCHAR(100),
    fecha_desde   DATE,
    fecha_hasta   DATE,
    total_horas   DECIMAL(5,2),
    conocimientos VARCHAR(265),
    experiencia   VARCHAR(200),
    aprendizaje   VARCHAR(200),
    id_Al         INT,
    CONSTRAINT fk_planilla_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al)
);

CREATE TABLE planilla_semanal_detalle (
    id_PS       INT NOT NULL,
    id_PSD      INT NOT NULL,
    fecha       DATE,
    descripcion VARCHAR(65),
    horas       DECIMAL(5,2),
    PRIMARY KEY (id_PS, id_PSD),
    CONSTRAINT fk_detalle_planilla FOREIGN KEY (id_PS) REFERENCES planilla_semanal (id_PS)
);

CREATE TABLE documento_scan (
    id_DS           INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100),
    ruta            VARCHAR(200),
    fecha_subida    DATETIME,
    tipo_documento  VARCHAR(50),
    hash_integridad VARCHAR(64),
    validado        BOOLEAN DEFAULT FALSE,
    id_Al           INT,
    CONSTRAINT fk_documento_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al),
    INDEX idx_documento_scan_tipo (tipo_documento),
    INDEX idx_documento_scan_validado (validado)
);
