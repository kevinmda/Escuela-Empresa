package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PadreTutor;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;

// Extraido de ImprimirController para que tanto la web como la API movil (ver
// FormulariosMovilController) generen el mismo PDF llamando al mismo codigo, en
// vez de duplicar las coordenadas de cada campo en dos lugares. La logica es
// identica a como estaba antes: solo cambia que ahora devuelve byte[] en vez de
// escribir directo sobre el HttpServletResponse del controller web.
@Service
public class FormularioPdfService {

    private final PlantillaService plantillaService;

    public FormularioPdfService(PlantillaService plantillaService) {
        this.plantillaService = plantillaService;
    }

    public byte[] generarContrato(Alumno alumno, List<PlanillaSemanal> planillas) throws IOException {
        PDDocument document = plantillaService.cargarPdf("CONTRATO_PEL_2025.pdf");
        PDPage pagina = document.getPage(0);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 411, 861.5f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 393, 848.5f);

        String supervisor = planillas.isEmpty() ? null : planillas.get(0).getSupervisor();
        if (supervisor != null && !supervisor.isBlank()) {
            escribirTextoCentrado(contentStream, fuente, 10, supervisor, 368, 835);
        }

        if (alumno.getEmpresa() != null) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 203, 821.8f);
        }
        if (alumno.getPadreTutor() != null) {
            PadreTutor pt = alumno.getPadreTutor();
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
            String nombrePadreTutor = primerNombre + " " + primerApellido;
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 437, 273);
        }

        contentStream.close();
        return aBytes(document);
    }

    public byte[] generarAutorizacion(Alumno alumno) throws IOException {
        PDDocument document = plantillaService.cargarPdf("AUTORIZACION_PADRES_PEL_25.pdf");
        PDPage pagina = document.getPage(0);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 388, 767.2f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getCi(), 257, 739);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 156, 719);
        if (alumno.getEmpresa() != null) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 451, 698.9f);
        }
        if (alumno.getPadreTutor() != null) {
            PadreTutor pt = alumno.getPadreTutor();
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
            String nombrePadreTutor = primerNombre + " " + primerApellido;
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 307, 640);
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, pt.getCi(), 137, 617.3f);
        }
        // Segunda autorizacion (la de repuesto)
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 388, 366.8f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getCi(), 257, 339);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 156, 319);
        if (alumno.getEmpresa() != null) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 451, 298.7f);
        }
        if (alumno.getPadreTutor() != null) {
            PadreTutor pt = alumno.getPadreTutor();
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
            String nombrePadreTutor = primerNombre + " " + primerApellido;
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 307, 239.8f);
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, pt.getCi(), 137, 217.2f);
        }

        contentStream.close();
        return aBytes(document);
    }

    public byte[] generarFichaFinal(Alumno alumno, List<PlanillaSemanal> todasLasPlanillas) throws IOException {
        if (todasLasPlanillas.isEmpty()) {
            throw new ReglaNegocioException("El alumno no tiene planillas cargadas, no se puede generar el documento");
        }

        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_PEL_2025.pdf");
        PDPage pagina = document.getPage(0);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        escribirTexto(contentStream, fuente, 10, alumno.getNombres() + " " + alumno.getApellidos(), 210, 756);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 210, 716);
        escribirTexto(contentStream, fuente, 10, String.valueOf(alumno.getEdad()), 399, 716);

        escribirTexto(contentStream, fuente, 10, alumno.getEspecialidad().getNombre(), 210, 677);
        escribirTexto(contentStream, fuente, 10, alumno.getCurso(), 402, 677);
        escribirTexto(contentStream, fuente, 10, alumno.getSeccion(), 488, 677);

        escribirTexto(contentStream, fuente, 10, alumno.getEmail(), 210, 599);
        escribirTexto(contentStream, fuente, 10, alumno.getTelefono(), 451, 599);

        LocalDate fechaInicioPasantia = todasLasPlanillas.stream()
                .map(PlanillaSemanal::getFechaDesde)
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate fechaFinPasantia = todasLasPlanillas.stream()
                .map(PlanillaSemanal::getFechaHasta)
                .max(LocalDate::compareTo)
                .orElseThrow();

        Locale localeEspanol = new Locale.Builder().setLanguage("es").setRegion("ES").build();

        DateTimeFormatter formatoLargo = new DateTimeFormatterBuilder()
                .appendPattern("d 'de' MMMM 'de' yyyy")
                .toFormatter(localeEspanol);

        escribirParrafo(contentStream, fuente, 10, fechaInicioPasantia.format(formatoLargo), 210, 560, 106, 14);
        escribirParrafo(contentStream, fuente, 10, fechaFinPasantia.format(formatoLargo), 451, 560, 106, 14);

        contentStream.close();
        return aBytes(document);
    }

    public byte[] generarFichaFinalEvaluativa(Alumno alumno) throws IOException {
        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_EVAL_PASANTE_PEL_2025.pdf");
        PDPage pagina = document.getPage(3);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        escribirParrafo(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 445, 290, 127, 14);

        contentStream.close();
        return aBytes(document);
    }

    private byte[] aBytes(PDDocument document) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        document.save(salida);
        document.close();
        return salida.toByteArray();
    }

    private void escribirTextoCentrado(PDPageContentStream contentStream, PDType1Font fuente,
                                        float tamanioFuente, String texto,
                                        float centroX, float y) throws IOException {

        float anchoTexto = fuente.getStringWidth(texto) / 1000 * tamanioFuente;
        float posicionX = centroX - (anchoTexto / 2);

        contentStream.beginText();
        contentStream.setFont(fuente, tamanioFuente);
        contentStream.newLineAtOffset(posicionX, y);
        contentStream.showText(texto);
        contentStream.endText();
    }

    private void escribirTexto(PDPageContentStream contentStream, PDType1Font fuente, float tamanioFuente,
                                String texto, float posicionX, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(fuente, tamanioFuente);
        contentStream.newLineAtOffset(posicionX, y);
        contentStream.showText(texto);
        contentStream.endText();
    }

    private void escribirParrafo(PDPageContentStream contentStream, PDType1Font fuente,
                                  float tamanioFuente, String texto,
                                  float x, float yInicial, float anchoMaximo, float interlineado) throws IOException {

        String[] palabras = texto.split(" ");
        StringBuilder lineaActual = new StringBuilder();
        float y = yInicial;

        for (String palabra : palabras) {
            String lineaConPalabraNueva = lineaActual.isEmpty()
                    ? palabra
                    : lineaActual + " " + palabra;

            float anchoLinea = fuente.getStringWidth(lineaConPalabraNueva) / 1000 * tamanioFuente;

            if (anchoLinea > anchoMaximo) {
                escribirTexto(contentStream, fuente, tamanioFuente, lineaActual.toString(), x, y);
                y -= interlineado;
                lineaActual = new StringBuilder(palabra);
            } else {
                lineaActual = new StringBuilder(lineaConPalabraNueva);
            }
        }

        if (!lineaActual.isEmpty()) {
            escribirTexto(contentStream, fuente, tamanioFuente, lineaActual.toString(), x, y);
        }
    }
}
