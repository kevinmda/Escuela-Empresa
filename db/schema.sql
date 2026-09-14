-- Esquema completo de la base "escuela-empresa" para una instalacion NUEVA.
--
-- La app corre con spring.jpa.hibernate.ddl-auto=validate: Hibernate no crea ni
-- modifica tablas, solo comprueba al arrancar que cada entidad tenga su tabla y
-- sus columnas con un tipo compatible. Este archivo es la contraparte: lo que
-- tiene que existir para que esa validacion pase.
--
-- Esta escrito a partir de las entidades de src/main/java/.../entity (nombres de
-- tabla, de columna y longitudes salen de ahi). Si en tu MySQL la base ya existe,
-- NO corras esto: mira db/migraciones/ para los cambios incrementales.
--
--   mysql -u root -p < db/schema.sql
--
-- Orden: cada tabla va despues de las que referencia por FK.

CREATE DATABASE IF NOT EXISTS `escuela-empresa`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `escuela-empresa`;

-- Credenciales y estado de la cuenta. Es la tabla contra la que se loguea todo el
-- mundo; el rol sale de si hay una fila en administrador o en alumno que la apunte.
CREATE TABLE usuario (
    id_Usr                 INT AUTO_INCREMENT PRIMARY KEY,
    ci                     VARCHAR(255),
    contrasena             VARCHAR(255) NOT NULL,          -- hash BCrypt
    email                  VARCHAR(255) NOT NULL UNIQUE,   -- es el "username" del login
    activo                 BOOLEAN NOT NULL DEFAULT FALSE, -- pasa a TRUE al verificar el codigo del primer ingreso
    token_activacion       VARCHAR(100),                   -- codigo de 6 digitos (activacion o recuperacion)
    token_expiracion       DATETIME,
    intentos_codigo        INT NOT NULL DEFAULT 0,
    intentos_login         INT NOT NULL DEFAULT 0,
    bloqueado_hasta        DATETIME,
    contrasena_por_defecto BOOLEAN NOT NULL DEFAULT TRUE   -- obliga a cambiarla en el primer uso
);

-- Administrativos y coordinadores comparten tabla y rol (ROLE_ADMIN); los
-- distingue `cargo`: 'administrativo' o 'coordinador'.
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

-- Una especialidad tiene un coordinador y un coordinador una sola especialidad
-- (de ahi el UNIQUE). Para un administrativo no hay fila que lo apunte.
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
    fechaNac  DATE,                       -- camelCase a proposito: la app no convierte nombres a snake_case
    telefono  VARCHAR(35),
    email     VARCHAR(100),
    curso     VARCHAR(45),
    seccion   VARCHAR(45),
    id_Usr    INT UNIQUE,
    id_Esp    INT,
    id_Emp    INT,                        -- se asigna desde Coordinacion; puede quedar NULL
    id_PT     INT,
    id_Sup    INT,                        -- idem
    CONSTRAINT fk_alumno_usuario      FOREIGN KEY (id_Usr) REFERENCES usuario (id_Usr),
    CONSTRAINT fk_alumno_especialidad FOREIGN KEY (id_Esp) REFERENCES especialidad (id_Esp),
    CONSTRAINT fk_alumno_empresa      FOREIGN KEY (id_Emp) REFERENCES empresa (id_Emp),
    CONSTRAINT fk_alumno_padre_tutor  FOREIGN KEY (id_PT)  REFERENCES padre_tutor (id_PT),
    CONSTRAINT fk_alumno_supervisor   FOREIGN KEY (id_Sup) REFERENCES supervisor (id_Sup)
);

-- Cabecera de la planilla de control semanal (una por semana, maximo 6 por alumno).
CREATE TABLE planilla_semanal (
    id_PS         INT AUTO_INCREMENT PRIMARY KEY,
    supervisor    VARCHAR(100),
    fecha_desde   DATE,
    fecha_hasta   DATE,
    total_horas   DECIMAL(5,0),             -- suma exacta de los dias, siempre entero
    conocimientos VARCHAR(265),
    experiencia   VARCHAR(200),
    aprendizaje   VARCHAR(200),
    id_Al         INT,
    CONSTRAINT fk_planilla_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al)
);

-- Un renglon por dia trabajado. La clave es compuesta: id_PSD arranca en 1 dentro
-- de cada planilla (lo asigna la app, no es AUTO_INCREMENT).
CREATE TABLE planilla_semanal_detalle (
    id_PS       INT NOT NULL,
    id_PSD      INT NOT NULL,
    fecha       DATE,
    descripcion VARCHAR(65),
    horas       DECIMAL(5,0),
    PRIMARY KEY (id_PS, id_PSD),
    CONSTRAINT fk_detalle_planilla FOREIGN KEY (id_PS) REFERENCES planilla_semanal (id_PS)
);

-- PDFs que el alumno sube (contrato firmado, planillas escaneadas, etc.).
-- El archivo vive en app.uploads.directorio con un nombre UUID; `nombre` es el original.
CREATE TABLE documento_scan (
    id_DS           INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100),
    ruta            VARCHAR(200),
    fecha_subida    DATETIME,
    tipo_documento  VARCHAR(50),             -- nombre del enum TipoDocumento (CONTRATO, PLANTILLA_SEMANAL, ...)
    hash_integridad VARCHAR(64),             -- SHA-256 del archivo
    validado        BOOLEAN DEFAULT FALSE,
    id_Al           INT,
    CONSTRAINT fk_documento_alumno FOREIGN KEY (id_Al) REFERENCES alumno (id_Al),
    INDEX idx_documento_scan_tipo (tipo_documento),
    INDEX idx_documento_scan_validado (validado)
);

-- No hay pantalla para dar de alta usuarios: el primer administrativo se carga a
-- mano. La contrasena es un hash BCrypt (ver README, "Primer usuario").
--
-- INSERT INTO usuario (ci, contrasena, email, activo) VALUES ('0', '<hash_bcrypt>', 'admin@colegio.edu.py', FALSE);
-- INSERT INTO administrador (nombres, apellidos, cargo, id_Usr) VALUES ('Nombre', 'Apellido', 'administrativo', LAST_INSERT_ID());
