package com.EscuelaEmpresa.gestor_pasantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Las plantillas se leen del classpath y no del disco: esta prueba corre desde
// cualquier directorio de trabajo y con el JAR empaquetado, que es justo lo que
// antes fallaba.
class PlantillaServiceTest {

    private final PlantillaService servicio = new PlantillaService();

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRATO_PEL_2025.pdf",
            "AUTORIZACION_PADRES_PEL_25.pdf",
            "FICHA_FINAL_PEL_2025.pdf",
            "FICHA_FINAL_EVAL_PASANTE_PEL_2025.pdf",
            "CONTROL_SEMANAL_PEL_2025.pdf"
    })
    void cargaCadaPdfOficialConAlMenosUnaPagina(String nombre) throws Exception {
        try (PDDocument pdf = servicio.cargarPdf(nombre)) {
            assertNotNull(pdf.getPage(0));
        }
    }

    @Test
    void abreLaPlantillaDelInformeComoDocx() throws Exception {
        try (InputStream entrada = servicio.abrir("Informe_Pasantia_Plantilla.docx");
             XWPFDocument docx = new XWPFDocument(entrada)) {
            assertEquals(false, docx.getParagraphs().isEmpty());
        }
    }

    @Test
    void unaPlantillaInexistenteFallaClaro() {
        assertThrows(FileNotFoundException.class, () -> servicio.abrir("no_existe.pdf"));
    }
}
