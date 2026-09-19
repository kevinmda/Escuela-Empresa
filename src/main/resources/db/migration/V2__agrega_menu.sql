-- Tablas de menu por usuario, agregadas por kevinmda (ver volcado que compartio).
-- Solo estructura: las dos nacen vacias, sin datos de prueba que migrar.

CREATE TABLE IF NOT EXISTS menu (
    id_Menu        INT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(45) NOT NULL,
    predeterminado BOOLEAN NOT NULL,
    activo         BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario_menu (
    id_Usr  INT NOT NULL,
    id_Menu INT NOT NULL,
    PRIMARY KEY (id_Usr, id_Menu),
    CONSTRAINT fk_usuario_menu_usuario FOREIGN KEY (id_Usr) REFERENCES usuario (id_Usr),
    CONSTRAINT fk_usuario_menu_menu FOREIGN KEY (id_Menu) REFERENCES menu (id_Menu)
);
