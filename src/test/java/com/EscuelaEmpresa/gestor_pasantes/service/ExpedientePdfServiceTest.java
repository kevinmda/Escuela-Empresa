package com.EscuelaEmpresa.gestor_pasantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;

// El orden de encuadernado es lo único propio de ExpedientePdfService (el resto
// es PDFMergerUtility, ya probado por PDFBox): autorización, contrato, las
// planillas semanales en el orden en que se subieron, ficha final y ficha
// final evaluativa. Esta prueba no toca el repositorio ni el filesystem.
class ExpedientePdfServiceTest {

    private static DocumentoSubido documento(TipoDocumento tipo, int minutosDesdeElInicio) {
        DocumentoSubido documento = new DocumentoSubido();
        documento.setTipoDocumento(tipo);
        documento.setFechaSubida(LocalDateTime.of(2026, 3, 1, 8, 0).plusMinutes(minutosDesdeElInicio));
        return documento;
    }

    @Test
    void ordenaLosCincoTiposSinImportarComoLlegaronDelRepositorio() {
        DocumentoSubido fichaEval = documento(TipoDocumento.FICHA_FINAL_EVALUATIVA, 50);
        DocumentoSubido fichaFinal = documento(TipoDocumento.FICHA_FINAL_ALUMNO, 40);
        DocumentoSubido plantilla = documento(TipoDocumento.PLANTILLA_SEMANAL, 20);
        DocumentoSubido contrato = documento(TipoDocumento.CONTRATO, 10);
        DocumentoSubido autorizacion = documento(TipoDocumento.AUTORIZACION, 0);

        // A proposito en un orden que no coincide ni con el de entrega ni con el
        // de encuadernado, como llegaria de findByAlumno_IdAlOrderByFechaSubidaDesc.
        List<DocumentoSubido> desordenados =
                List.of(fichaEval, plantilla, autorizacion, fichaFinal, contrato);

        List<DocumentoSubido> resultado = ExpedientePdfService.ordenarParaExpediente(desordenados);

        assertEquals(List.of(autorizacion, contrato, plantilla, fichaFinal, fichaEval), resultado);
    }

    @Test
    void lasSeisPlantillasSemanalesVanEnElOrdenEnQueSeSubieron() {
        DocumentoSubido semana3 = documento(TipoDocumento.PLANTILLA_SEMANAL, 30);
        DocumentoSubido semana1 = documento(TipoDocumento.PLANTILLA_SEMANAL, 10);
        DocumentoSubido semana2 = documento(TipoDocumento.PLANTILLA_SEMANAL, 20);

        List<DocumentoSubido> resultado =
                ExpedientePdfService.ordenarParaExpediente(List.of(semana3, semana1, semana2));

        assertEquals(List.of(semana1, semana2, semana3), resultado);
    }

    @Test
    void unTipoSinDocumentosSimplementeNoAparece() {
        DocumentoSubido contrato = documento(TipoDocumento.CONTRATO, 0);

        List<DocumentoSubido> resultado = ExpedientePdfService.ordenarParaExpediente(List.of(contrato));

        assertEquals(List.of(contrato), resultado);
    }
}
