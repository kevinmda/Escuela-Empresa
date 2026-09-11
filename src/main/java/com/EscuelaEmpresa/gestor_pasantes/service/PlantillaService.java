package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

// Punto unico de acceso a las plantillas oficiales (PDFs del PEL y el .docx del informe)
// que viven en src/main/resources/plantillas.
//
// Antes cada controlador las abria con new File("src/main/resources/plantillas/..."):
// una ruta relativa al directorio desde donde se lanza la app. Eso anda con
// "mvnw spring-boot:run" desde la raiz del proyecto, pero en cuanto se empaqueta el
// JAR para el VPS esa carpeta no existe en el disco (esta adentro del JAR) y todas
// las descargas tiraban FileNotFoundException. Leyendolas del classpath funcionan
// igual en los dos escenarios.
@Service
public class PlantillaService {

    private static final String CARPETA = "plantillas/";

    // Devuelve el PDF ya cargado en PDFBox. Quien lo pide es responsable de cerrarlo
    // (document.close()) despues de escribirlo en la respuesta.
    public PDDocument cargarPdf(String nombreArchivo) throws IOException {
        // Loader.loadPDF(File) permite leer bajo demanda, pero desde el classpath no
        // hay File: se lee entero a memoria. Las plantillas pesan pocos cientos de KB.
        return Loader.loadPDF(leerBytes(nombreArchivo));
    }

    // Para plantillas que no son PDF (el .docx del informe), se entrega el stream
    // crudo y quien lo pide lo cierra.
    public InputStream abrir(String nombreArchivo) throws IOException {
        return new ClassPathResource(CARPETA + nombreArchivo).getInputStream();
    }

    private byte[] leerBytes(String nombreArchivo) throws IOException {
        try (InputStream entrada = abrir(nombreArchivo)) {
            return entrada.readAllBytes();
        }
    }
}
