package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;

// Arma "Documentos adjuntos": un unico PDF con todo lo que el alumno subio en
// /alumno/subir, en el mismo orden en que se presentan en papel. Solo tiene
// sentido pedirlo cuando el expediente ya esta completo (los diez comprobantes);
// antes de eso faltarian paginas y el PDF resultante seria enganoso.
@Service
public class ExpedientePdfService {

    // Autorizacion y contrato se firman antes de empezar; las planillas se
    // entregan semana a semana durante la pasantia; las dos fichas se
    // completan al terminar. Ese es el orden en el que van encuadernadas.
    private static final List<TipoDocumento> ORDEN_EXPEDIENTE = List.of(
            TipoDocumento.AUTORIZACION,
            TipoDocumento.CONTRATO,
            TipoDocumento.PLANTILLA_SEMANAL,
            TipoDocumento.FICHA_FINAL_ALUMNO,
            TipoDocumento.FICHA_FINAL_EVALUATIVA
    );

    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final LimitesDocumentoService limitesDocumentoService;

    public ExpedientePdfService(DocumentoSubidoRepository documentoSubidoRepository,
                                 LimitesDocumentoService limitesDocumentoService) {
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.limitesDocumentoService = limitesDocumentoService;
    }

    public boolean estaCompleto(Integer idAlumno) {
        return limitesDocumentoService.expedienteCompleto(idAlumno);
    }

    // Combina los PDFs del alumno y escribe el resultado en la salida. Quien
    // llama es responsable de esa salida (por ejemplo, el OutputStream de la
    // respuesta HTTP): esta función no la cierra.
    public void generarPdf(Alumno alumno, OutputStream salida) throws IOException {
        if (!estaCompleto(alumno.getIdAl())) {
            throw new ReglaNegocioException(
                    "Todavía no se entregaron los 10 comprobantes del expediente. " +
                    "Los documentos adjuntos se pueden generar recién cuando la entrega esté completa.");
        }

        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());

        PDFMergerUtility combinador = new PDFMergerUtility();
        int agregados = 0;

        for (DocumentoSubido documento : ordenarParaExpediente(documentos)) {
            File archivoFisico = new File(documento.getRutaArchivo());
            if (!archivoFisico.exists()) {
                continue; // si el archivo ya no esta en el disco, seguimos con el resto
            }
            combinador.addSource(archivoFisico);
            agregados++;
        }

        if (agregados == 0) {
            throw new RecursoNoEncontradoException(
                    "Los archivos del expediente ya no están disponibles en el servidor");
        }

        combinador.setDestinationStream(salida);
        combinador.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
    }

    // Separado de generarPdf para poder probar el orden (lo único realmente
    // propio de esta clase) sin depender del repositorio ni de archivos en disco.
    static List<DocumentoSubido> ordenarParaExpediente(List<DocumentoSubido> documentos) {
        return ORDEN_EXPEDIENTE.stream()
                .flatMap(tipo -> documentos.stream()
                        .filter(documento -> documento.getTipoDocumento() == tipo)
                        // Entre varios del mismo tipo (las seis planillas semanales),
                        // van en el orden en que se subieron.
                        .sorted(Comparator.comparing(DocumentoSubido::getFechaSubida)))
                .toList();
    }
}
