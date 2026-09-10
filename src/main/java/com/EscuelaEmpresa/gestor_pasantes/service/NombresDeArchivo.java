package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.File;

/**
 * Deja un nombre de archivo en condiciones de viajar en una cabecera
 * Content-Disposition.
 *
 * Hace falta porque parte de esos nombres los eligio el alumno al subir el
 * documento. Estaba sanitizado en el ZIP y en la vista del administrador, pero
 * no en la vista del propio alumno, que lo pegaba crudo: un nombre con comillas
 * o con un punto y coma parte la cabecera en dos, y uno con salto de linea la
 * rompe entera. Al estar en un solo lugar, no puede volver a quedar la mitad
 * protegida y la mitad no.
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

    /**
     * La cabecera Content-Disposition entera y ya armada.
     *
     * @param disposicion "inline" para que el navegador lo muestre, "attachment" para que lo baje
     */
    public static String contentDisposition(String disposicion, String nombre, String respaldo) {
        // el nombre va entre comillas, que es lo que dice el RFC 6266 y lo unico
        // que hace que un nombre con espacios llegue completo
        return disposicion + "; filename=\"" + seguro(nombre, respaldo) + "\"";
    }
}
