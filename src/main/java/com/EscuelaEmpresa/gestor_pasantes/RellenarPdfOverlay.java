package com.EscuelaEmpresa.gestor_pasantes;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class RellenarPdfOverlay {
    private static void escribirTexto(PDPageContentStream contentStream, PDType1Font fuente, float tamanioFuente, String texto, float posicionX, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(fuente, tamanioFuente);
        contentStream.newLineAtOffset(posicionX, y);
        contentStream.showText(texto);
        contentStream.endText();
    }

    private static void escribirParrafo(PDPageContentStream contentStream, PDType1Font fuente,
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

    public static void main(String[] args) throws IOException {

        File archivoOriginal = new File("src/main/resources/plantillas/FICHA_FINAL_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);

        PDPage pagina = document.getPage(0); // la primera página (índice 0)

        PDPageContentStream contentStream = new PDPageContentStream(
        document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        escribirTexto(contentStream, fuente, 10, "Juan Perez", 210, 756);
        escribirTexto(contentStream, fuente, 10, "1234567", 210, 716);
        escribirTexto(contentStream, fuente, 10, "17", 399, 716);
        escribirTexto(contentStream, fuente, 10, "Informática", 210, 677);
        escribirTexto(contentStream, fuente, 10, "3ro", 402, 677);
        escribirTexto(contentStream, fuente, 10, "1ra", 488, 677);
        escribirTexto(contentStream, fuente, 10, "alumno1@ctn.com", 210, 599);
        escribirTexto(contentStream, fuente, 10, "0981234567", 451, 599);
        escribirParrafo(contentStream, fuente, 10, "13 de octubre de 2026", 210, 560, 106, 14);
        escribirParrafo(contentStream, fuente, 10, "21 de noviembre de 2026", 451, 560, 106, 14);

        contentStream.close();

        document.save(new File("src/main/resources/plantillas/prueba_resultado.pdf"));
        document.close();

        System.out.println("✅ PDF generado, revisá prueba_resultado.pdf");
    }
}