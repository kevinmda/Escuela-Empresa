package com.EscuelaEmpresa.gestor_pasantes.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las plantillas se abrian con new File("src/main/resources/plantillas/...") y por
 * eso solo aparecian si la aplicacion se arrancaba parada en la raiz del proyecto.
 * Empaquetada, esa carpeta no existe y se caian todas las generaciones de PDF.
 *
 * Este test las abre por el classpath, que es como las va a encontrar la aplicacion
 * empaquetada. No levanta Spring ni necesita la base de datos: corre solo.
 */
class PlantillasTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRATO_PEL_2025.pdf",
            "AUTORIZACION_PADRES_PEL_25.pdf",
            "FICHA_FINAL_PEL_2025.pdf",
            "FICHA_FINAL_EVAL_PASANTE_PEL_2025.pdf",
            "CONTROL_SEMANAL_PEL_2025.pdf"
    })
    void cadaPlantillaPdfSeAbreYTienePaginas(String nombreArchivo) throws IOException {
        try (PDDocument documento = Plantillas.abrirPdf(nombreArchivo)) {
            assertTrue(documento.getNumberOfPages() > 0,
                    nombreArchivo + " se abrio pero no tiene ni una pagina");
        }
    }

    // La ficha de evaluacion es la unica de la que se escribe en la pagina 4
    // (ImprimirController pide getPage(3)); si viniera con menos, esa descarga
    // se caeria en produccion y no aca.
    @Test
    void laFichaDeEvaluacionTieneLaPaginaQueSeLeEscribe() throws IOException {
        try (PDDocument documento = Plantillas.abrirPdf("FICHA_FINAL_EVAL_PASANTE_PEL_2025.pdf")) {
            assertTrue(documento.getNumberOfPages() >= 4,
                    "ImprimirController escribe en la pagina 4 de esta plantilla");
        }
    }

    @Test
    void laPlantillaDelInformeSeAbreComoFlujo() throws IOException {
        try (InputStream entrada = Plantillas.abrir("Informe_Pasantia_Plantilla.docx")) {
            assertNotNull(entrada);
            assertTrue(entrada.readAllBytes().length > 0, "la plantilla del informe llego vacia");
        }
    }

    @Test
    void unaPlantillaQueNoExisteAvisaCualFalta() {
        IOException error = org.junit.jupiter.api.Assertions.assertThrows(
                IOException.class,
                () -> Plantillas.abrirPdf("NO_EXISTE.pdf"));

        assertTrue(error.getMessage().contains("NO_EXISTE.pdf"),
                "el mensaje tiene que decir que plantilla falta, decia: " + error.getMessage());
    }
}
