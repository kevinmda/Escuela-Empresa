package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;

@Service
public class PlanillaSemanalPdfService {

    public byte[] generarPdf(Alumno alumno, PlanillaSemanal planilla, List<PlanillaSemanalDetalle> detalles) throws IOException {

        // 1. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/CONTROL_SEMANAL_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 2. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM");

        // 3. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTexto(contentStream, fuente, 10, alumno.getEspecialidad().getNombre(), 386, 778);
        escribirTexto(contentStream, fuente, 10, alumno.getNombres() + " " + alumno.getApellidos(), 227, 752);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 227, 709);
        escribirTexto(contentStream, fuente, 10, planilla.getFechaDesde().format(formatoFecha), 505, 703);
        escribirTexto(contentStream, fuente, 10, alumno.getCurso() + " " + alumno.getSeccion(), 227, 683);
        escribirTexto(contentStream, fuente, 10, planilla.getFechaHasta().format(formatoFecha), 505, 677);

        // 4. Días de la semana, tomados de los detalles según DayOfWeek
        escribirDia(contentStream, fuente, detalles, DayOfWeek.MONDAY, 574);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.TUESDAY, 554);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.WEDNESDAY, 534);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.THURSDAY, 514);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.FRIDAY, 494);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.SATURDAY, 474);

        escribirTexto(contentStream, fuente, 10, formatearHoras(planilla.getTotalHoras()), 499, 454);

        // 5. Párrafos
        escribirParrafo(contentStream, fuente, 9, planilla.getConocimientos(), 250, 392, 288, 14);
        escribirParrafo(contentStream, fuente, 9, planilla.getExperiencia(), 250, 332, 288, 14);
        escribirParrafo(contentStream, fuente, 9, planilla.getAprendizaje(), 250, 287, 288, 14);

        String nombreApellido = alumno.getNombres().trim().split(" ")[0] + " " + alumno.getApellidos().trim().split(" ")[0];
        escribirTexto(contentStream, fuente, 10, nombreApellido, 86, 162);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 128, 138);

        // 6. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 7. Devolver el PDF como array de bytes (en vez de escribirlo directo a una response HTTP)
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        document.save(salida);
        document.close();

        return salida.toByteArray();
    }

    private void escribirTexto(PDPageContentStream contentStream, PDType1Font fuente, float tamanioFuente, String texto, float posicionX, float y) throws IOException {
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
                // La línea actual ya está completa, la dibujamos y arrancamos una nueva
                escribirTexto(contentStream, fuente, tamanioFuente, lineaActual.toString(), x, y);
                y -= interlineado;
                lineaActual = new StringBuilder(palabra);
            } else {
                lineaActual = new StringBuilder(lineaConPalabraNueva);
            }
        }

        // Dibujar la última línea que quedó pendiente
        if (!lineaActual.isEmpty()) {
            escribirTexto(contentStream, fuente, tamanioFuente, lineaActual.toString(), x, y);
        }
    }

    private void escribirDia(PDPageContentStream contentStream, PDType1Font fuente,
                            List<PlanillaSemanalDetalle> detalles, DayOfWeek dia, float y) throws IOException {

        PlanillaSemanalDetalle detalle = detalles.stream()
                .filter(d -> d.getFecha().getDayOfWeek() == dia)
                .findFirst()
                .orElse(null);

        String descripcion = (detalle != null) ? detalle.getDescripcion() : "-";
        String horas = (detalle != null) ? formatearHoras(detalle.getHoras()) : "-";

        escribirTexto(contentStream, fuente, 9, descripcion, 168, y);
        escribirTexto(contentStream, fuente, 10, horas, 499, y);
    }

    private String formatearHoras(Float horas) {
        if (horas == null) return "-";
        if (horas == Math.floor(horas)) {
            return String.valueOf(horas.intValue()); // 8.0 -> "8"
        }
        return String.format("%.1f", horas); // 7.5 -> "7.5"
    }
}