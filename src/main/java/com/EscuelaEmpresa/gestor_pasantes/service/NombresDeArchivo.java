package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.File;

/**
 * Deja un nombre de archivo listo para usarse como entrada de un ZIP o como
 * nombre de archivo suelto.
 *
 * Hace falta porque parte de esos nombres los eligio el alumno al subir el
 * documento: uno con "../" se escaparia de la carpeta del ZIP, y uno con
 * caracteres de control o separadores rompe el archivo resultante.
 */
public final class NombresDeArchivo {

    private NombresDeArchivo() {
        // clase de utilidad: no se instancia
    }

    /**
     * El nombre sin nada que pueda romper la cabecera ni escaparse a otra carpeta:
     * se queda con la ultima parte de la ruta y reemplaza los caracteres de control,
     * los separadores y las comillas.
     *
     * @param respaldo que usar si el nombre viene vacio o queda vacio despues de limpiarlo
     */
    public static String seguro(String nombre, String respaldo) {
        if (nombre == null || nombre.isBlank()) {
            return respaldo;
        }

        // getName() se queda con lo que va despues del ultimo separador, asi que
        // "../../etc/passwd" queda en "passwd"
        String limpio = new File(nombre).getName()
                .replaceAll("[\\x00-\\x1F\\x7F\\\\/:*?\"<>|;]", "_");

        return limpio.isBlank() ? respaldo : limpio;
    }
}
