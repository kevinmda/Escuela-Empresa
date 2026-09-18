package com.EscuelaEmpresa.gestor_pasantes.util;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;

// Arma el header Content-Disposition de las descargas de forma segura.
//
// Antes se concatenaba a mano: "inline; filename=" + nombre. Con un nombre que
// traiga comillas o punto y coma el header se rompe (o se le puede inyectar otro
// parametro), y con acentos o espacios ("Informe_Pasantia_María Pérez.docx") el
// navegador recibe bytes fuera de ASCII y muestra cualquier cosa. Spring ya sabe
// codificar esto segun el RFC 6266 (filename*=UTF-8''...): se usa eso.
public final class Descarga {

    private Descarga() {}

    // Para abrir en el navegador (PDFs).
    public static String inline(String nombreArchivo, String respaldo) {
        return ContentDisposition.inline()
                .filename(nombreSeguro(nombreArchivo, respaldo), StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    // Para forzar la descarga (ZIP, DOCX).
    public static String adjunto(String nombreArchivo, String respaldo) {
        return ContentDisposition.attachment()
                .filename(nombreSeguro(nombreArchivo, respaldo), StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    // Nombre de archivo para lo que el alumno genera y descarga de sí mismo
    // (Contrato, Autorización, Ficha Final, Informe...): "Documento_NombreApellido.ext",
    // con el primer nombre y el primer apellido, cada uno con su primera letra en
    // mayúscula, pegados sin espacio ni guión bajo entre sí.
    public static String nombreDocumento(String documento, String nombres, String apellidos, String extension) {
        return nombreDocumento(documento, nombres, apellidos, null, extension);
    }

    // Igual que el de arriba, pero para una planilla puntual: agrega el número
    // de esa semana al final ("..._3.pdf"), el mismo número que ya se muestra
    // en el riel de semanas ("Semana 3").
    public static String nombreDocumento(String documento, String nombres, String apellidos,
                                          Integer numeroPlanilla, String extension) {
        String nombre = documento + "_" + primeraPalabraCapitalizada(nombres) + primeraPalabraCapitalizada(apellidos);
        if (numeroPlanilla != null) {
            nombre += "_" + numeroPlanilla;
        }
        return nombre + "." + extension;
    }

    // Toma la primera palabra (nombres y apellidos suelen traer más de una) y le
    // pone mayúscula inicial, sea cual sea cómo esté cargada en la base.
    private static String primeraPalabraCapitalizada(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String primeraPalabra = texto.trim().split("\\s+")[0];
        if (primeraPalabra.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(primeraPalabra.charAt(0)) + primeraPalabra.substring(1).toLowerCase();
    }

    // El nombre original puede venir nulo o vacio (subidas viejas), o traer una
    // ruta entera si el navegador la mando asi: se queda solo el nombre.
    private static String nombreSeguro(String nombreArchivo, String respaldo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return respaldo;
        }
        String soloNombre = nombreArchivo.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        return soloNombre.isBlank() ? respaldo : soloNombre;
    }
}
