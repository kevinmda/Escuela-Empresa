package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ClassPathResource;

/**
 * Un unico lugar para abrir los formularios en blanco que la escuela usa como
 * base (contrato, autorizacion, fichas, control semanal, informe).
 *
 * Antes cada pantalla los abria con new File("src/main/resources/plantillas/..."),
 * que es una ruta relativa al directorio desde donde se ejecuta la aplicacion y
 * que ademas apunta al codigo fuente. Funciona mientras se arranca con
 * "mvnw spring-boot:run" parado en la raiz del proyecto, y deja de funcionar en
 * cuanto se empaqueta: adentro del .jar no hay ninguna carpeta src/. Se caian
 * las cinco generaciones de PDF y el informe .docx, todas juntas.
 *
 * Leyendolas del classpath se encuentran igual en las dos situaciones, porque
 * las plantillas viajan dentro del jar.
 */
public final class Plantillas {

    private static final String CARPETA = "plantillas/";

    private Plantillas() {
        // clase de utilidad: no se instancia
    }

    /**
     * Abre una plantilla PDF lista para escribirle encima.
     * Quien la abre es responsable de cerrarla.
     */
    public static PDDocument abrirPdf(String nombreArchivo) throws IOException {
        // Loader.loadPDF(byte[]) en vez de la variante con File: desde el classpath
        // no siempre hay un File detras -- dentro del jar la plantilla es una entrada
        // comprimida, no un archivo del disco.
        return Loader.loadPDF(leerBytes(nombreArchivo));
    }

    /**
     * Abre una plantilla como flujo de bytes, para los formatos que no son PDF
     * (el informe de pasantia es un .docx que lee Apache POI).
     */
    public static InputStream abrir(String nombreArchivo) throws IOException {
        return new ByteArrayInputStream(leerBytes(nombreArchivo));
    }

    private static byte[] leerBytes(String nombreArchivo) throws IOException {
        ClassPathResource recurso = new ClassPathResource(CARPETA + nombreArchivo);

        if (!recurso.exists()) {
            throw new IOException("Falta la plantilla " + CARPETA + nombreArchivo
                    + " en los recursos de la aplicacion");
        }

        try (InputStream entrada = recurso.getInputStream()) {
            return entrada.readAllBytes();
        }
    }
}
