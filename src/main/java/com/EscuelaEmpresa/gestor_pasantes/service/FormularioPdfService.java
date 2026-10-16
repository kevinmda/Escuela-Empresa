package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
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

    // Version anterior, usada por la API movil (FormulariosMovilController), que
    // todavia no tiene este formulario: supervisor y padre/encargado salen de la
    // ultima planilla/de la base, igual que antes. No se toca para no romper esa
    // integracion.
    public byte[] generarContrato(Alumno alumno, List<PlanillaSemanal> planillas) throws IOException {
        String supervisor = planillas.isEmpty() ? null : planillas.get(0).getSupervisor();
        String empresa = alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : null;

        // El alumno ahora puede tener varios padres/tutores cargados (ver V12),
        // pero este formulario en papel solo tiene lugar para uno: se usa el
        // primero (alumno.getPadresTutores() ya viene ordenado por id), igual
        // que antes cuando solo existía esa opción.
        String padreEncargado = null;
        if (!alumno.getPadresTutores().isEmpty()) {
            PadreTutor pt = alumno.getPadresTutores().get(0);
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
            padreEncargado = primerNombre + " " + primerApellido;
        }

        return generarContrato(alumno, supervisor, empresa, null, padreEncargado);
    }

    // Version web: supervisor, area y padre/encargado vienen del formulario de
    // /alumno/documentos/antes-de-empezar y los escribe el alumno; empresa viene
    // de la base (ImprimirController la resuelve, no confia en lo que llegue del
    // formulario para ese campo bloqueado). Nombre, especialidad y fecha tambien
    // salen de la base/del reloj: por eso esos tres aparecen bloqueados en el
    // formulario y estos no.
    public byte[] generarContrato(Alumno alumno, String supervisor, String empresa, String area,
                                   String padreEncargado) throws IOException {
        PDDocument document = plantillaService.cargarPdf("CONTRATO_PEL_2026.pdf");
        PDPage pagina = document.getPage(0);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 411, 861.5f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 393, 848.5f);

        if (supervisor != null && !supervisor.isBlank()) {
            escribirTextoCentrado(contentStream, fuente, 10, supervisor, 368, 835);
        }

        if (empresa != null && !empresa.isBlank()) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, empresa, 203, 821.8f);
        }

        // "...oportunidades de pasantía en el área de___, con el objetivo..."
        // (clausula PRIMERA). Blanco entre x=120 y x=272 aprox.
        if (area != null && !area.isBlank()) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, area, 196, 774.5f);
        }

        // "...a los___ días del mes de___de año 2025.-" (clausula DUODECIMA).
        // El año queda tal cual esta impreso en la plantilla: es texto fijo de
        // la plantilla, no algo que este codigo escribe, y quedo desactualizado
        // (dice 2025). Cambiarlo requiere editar el PDF fuente, no esta linea.
        LocalDate hoy = LocalDate.now();
        Locale localeEspanol = new Locale.Builder().setLanguage("es").setRegion("ES").build();
        String mes = hoy.format(DateTimeFormatter.ofPattern("MMMM", localeEspanol));
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, String.valueOf(hoy.getDayOfMonth()), 158, 248);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, mes, 305, 248);

        // "...el Padre o Encargado del ESTUDIANTE-PASANTE, el (la) señor(a),___"
        // (misma clausula DUODECIMA, blanco ancho: x=323.9 a 515.3). Antes salia
        // solo de la base (primer nombre + primer apellido); ahora lo escribe el
        // alumno en el formulario, con el nombre completo.
        if (padreEncargado != null && !padreEncargado.isBlank()) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, padreEncargado, 438, 273);
        }

        contentStream.close();
        return aBytes(document);
    }

    // Versión de la API móvil: el Padre/Encargado sale de lo que está cargado en
    // la base (el formulario web, en cambio, manda lo que el alumno escribe).
    public byte[] generarAutorizacion(Alumno alumno) throws IOException {
        // Mismo criterio que en generarContrato: con varios padres/tutores
        // cargados, se usa el primero.
        String nombrePadre = null;
        String ciPadre = null;
        if (!alumno.getPadresTutores().isEmpty()) {
            PadreTutor pt = alumno.getPadresTutores().get(0);
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
            nombrePadre = primerNombre + " " + primerApellido;
            ciPadre = pt.getCi();
        }
        return generarAutorizacion(alumno, nombrePadre, ciPadre);
    }

    // Versión web: nombre y cédula del Padre/Encargado vienen del formulario
    // tal cual los escribió el alumno, sin compararlos con la base.
    public byte[] generarAutorizacion(Alumno alumno, String nombrePadre, String ciPadre) throws IOException {
        PDDocument document = plantillaService.cargarPdf("AUTORIZACION_PADRES_PEL.pdf");
        PDPage pagina = document.getPage(0);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        // "Asunción,___de___de 202_": el año ya viene con "202" impreso en la
        // plantilla, solo falta el último dígito (sirve para cualquier año de
        // esta década). El documento trae dos copias idénticas de este
        // formulario en la misma hoja, así que esto se escribe dos veces, una
        // por copia (ver más abajo, antes de la "segunda autorizacion").
        LocalDate hoy = LocalDate.now();
        Locale localeEspanol = new Locale.Builder().setLanguage("es").setRegion("ES").build();
        String mes = hoy.format(DateTimeFormatter.ofPattern("MMMM", localeEspanol));
        String Anio = String.valueOf(hoy.getYear());

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, String.valueOf(hoy.getDayOfMonth()), 393.5f, 811);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, mes, 452, 811);
        escribirTexto(contentStream, fuente, tamanioFuente, Anio, 505, 809.4f);

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 388, 767.2f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getCi(), 257, 739);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 156, 719);
        if (alumno.getEmpresa() != null) {
            escribirEmpresaConContinuacion(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(),
                    448, 698.9f, 140, 173, 669.3f, 170);
        }
        if (nombrePadre != null && !nombrePadre.isBlank()) {
            escribirTexto(contentStream, fuente, tamanioFuente, nombrePadre, 277, 640);
        }
        if (ciPadre != null && !ciPadre.isBlank()) {
            escribirTexto(contentStream, fuente, tamanioFuente, ciPadre, 117, 617.3f);
        }
        // Segunda autorizacion (la de repuesto)
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, String.valueOf(hoy.getDayOfMonth()), 393.5f, 411);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, mes, 452, 411);
        escribirTexto(contentStream, fuente, tamanioFuente, Anio, 505, 409.4f);

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 388, 366.8f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getCi(), 257, 339);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 156, 319);
        if (alumno.getEmpresa() != null) {
            escribirEmpresaConContinuacion(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(),
                    448, 298.7f, 140, 173, 269.1f, 170);
        }
        if (nombrePadre != null && !nombrePadre.isBlank()) {
            escribirTexto(contentStream, fuente, tamanioFuente, nombrePadre, 277, 239.8f);
        }
        if (ciPadre != null && !ciPadre.isBlank()) {
            escribirTexto(contentStream, fuente, tamanioFuente, ciPadre, 117, 217.2f);
        }

        contentStream.close();
        return aBytes(document);
    }

    // Version anterior, usada por la API movil (FormulariosMovilController), que
    // todavia no pide el Área de pasantía. No se toca para no romper esa
    // integracion; tampoco exige las planillas minimas, solo que haya alguna.
    public byte[] generarFichaFinal(Alumno alumno, List<PlanillaSemanal> todasLasPlanillas) throws IOException {
        if (todasLasPlanillas.isEmpty()) {
            throw new ReglaNegocioException("El alumno no tiene planillas cargadas, no se puede generar el documento");
        }
        return generarFichaFinal(alumno, todasLasPlanillas, null);
    }

    // Version web: area viene del formulario de /alumno/documentos/al-terminar,
    // habilitado recien con las 5 planillas minimas (ver
    // ImprimirController.generarPdfFichaFinalPel). "Área de Pasantía" ya
    // estaba en la plantilla, sin usar -- por eso no hizo falta agregar nada
    // a la plantilla, solo escribir ahí.
    public byte[] generarFichaFinal(Alumno alumno, List<PlanillaSemanal> todasLasPlanillas, String area)
            throws IOException {
        if (todasLasPlanillas.isEmpty()) {
            throw new ReglaNegocioException("El alumno no tiene planillas cargadas, no se puede generar el documento");
        }

        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_PEL.pdf");
        PDPage pagina = document.getPage(0);

        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        escribirTexto(contentStream, fuente, 10, alumno.getNombres() + " " + alumno.getApellidos(), 210, 756);
        escribirTexto(contentStream, fuente, 10, vacioSiNulo(alumno.getCi()), 210, 716);
        escribirTexto(contentStream, fuente, 10, String.valueOf(alumno.getEdad()), 399, 716);

        escribirTexto(contentStream, fuente, 10, alumno.getEspecialidad().getNombre(), 210, 677);
        escribirTexto(contentStream, fuente, 10, vacioSiNulo(alumno.getCurso()), 402, 677);
        escribirTexto(contentStream, fuente, 10, vacioSiNulo(alumno.getSeccion()), 488, 677);

        escribirTexto(contentStream, fuente, 10, vacioSiNulo(alumno.getEmail()), 210, 599);
        escribirTexto(contentStream, fuente, 10, vacioSiNulo(alumno.getTelefono()), 451, 599);

        LocalDate fechaInicioPasantia = todasLasPlanillas.stream()
                .map(PlanillaSemanal::getFechaDesde)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "Ninguna de tus planillas tiene fecha de inicio cargada, no se puede generar el documento"));

        LocalDate fechaFinPasantia = todasLasPlanillas.stream()
                .map(PlanillaSemanal::getFechaHasta)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "Ninguna de tus planillas tiene fecha de fin cargada, no se puede generar el documento"));

        Locale localeEspanol = new Locale.Builder().setLanguage("es").setRegion("ES").build();

        DateTimeFormatter formatoLargo = new DateTimeFormatterBuilder()
                .appendPattern("d 'de' MMMM 'de' yyyy")
                .toFormatter(localeEspanol);

        escribirParrafo(contentStream, fuente, 10, fechaInicioPasantia.format(formatoLargo), 210, 560, 106, 14);
        escribirParrafo(contentStream, fuente, 10, fechaFinPasantia.format(formatoLargo), 451, 560, 106, 14);

        if (area != null && !area.isBlank()) {
            escribirParrafo(contentStream, fuente, 10, area, 210, 482.5f, 340, 14);
        }

        contentStream.close();
        return aBytes(document);
    }

    public byte[] generarFichaFinalEvaluativa(Alumno alumno) throws IOException {
        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_EVAL_PASANTE_PEL.pdf");
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

    // Para el nombre de la Empresa/Institución en AUTORIZACION_PADRES_PEL.pdf: la
    // plantilla ya trae, pensada para completarse a mano, una línea de puntos más
    // angosta justo después, antes de "...con una duración máxima de hasta ocho
    // (8) horas diarias." -- es donde alguien completando el formulario a lapicera
    // seguiría escribiendo si el nombre no entra en el primer renglón. Si entra,
    // se centra en el primer renglón como siempre; si no entra, se corta en una
    // palabra completa (nunca a la mitad de una palabra) y el resto sigue
    // centrado en el segundo renglón.
    private void escribirEmpresaConContinuacion(PDPageContentStream contentStream, PDType1Font fuente,
                                                 float tamanioFuente, String texto,
                                                 float centroLinea1, float yLinea1, float anchoLinea1,
                                                 float centroLinea2, float yLinea2, float anchoLinea2) throws IOException {

        float anchoCompleto = fuente.getStringWidth(texto) / 1000 * tamanioFuente;
        if (anchoCompleto <= anchoLinea1) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, texto, centroLinea1, yLinea1);
            return;
        }

        String[] palabras = texto.split(" ");
        StringBuilder primeraLinea = new StringBuilder();
        int cortaEn = palabras.length;

        for (int i = 0; i < palabras.length; i++) {
            String candidato = primeraLinea.isEmpty() ? palabras[i] : primeraLinea + " " + palabras[i];
            float ancho = fuente.getStringWidth(candidato) / 1000 * tamanioFuente;
            if (ancho > anchoLinea1 && !primeraLinea.isEmpty()) {
                cortaEn = i;
                break;
            }
            primeraLinea = new StringBuilder(candidato);
        }

        String segundaLinea = String.join(" ", Arrays.copyOfRange(palabras, cortaEn, palabras.length));

        escribirTextoCentrado(contentStream, fuente, tamanioFuente, primeraLinea.toString(), centroLinea1, yLinea1);
        if (!segundaLinea.isBlank()) {
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, segundaLinea, centroLinea2, yLinea2);
        }
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

    // PDFBox tira una excepción si se le pide escribir null como texto (showText no
    // lo acepta). Los campos del alumno pueden venir vacíos en la base -- curso,
    // sección, email y teléfono no son obligatorios al cargarlo -- y sin esto,
    // justamente esos casos rompían la generación entera del documento.
    private String vacioSiNulo(String texto) {
        return texto != null ? texto : "";
    }
}
