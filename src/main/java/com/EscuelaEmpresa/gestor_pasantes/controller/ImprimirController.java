package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PadreTutor;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ImprimirController {
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;

    public ImprimirController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository, PlanillaSemanalRepository planillaSemanalRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
    }

    @GetMapping("/alumno/imprimir")
    public String mostrarImprimir() {
        return "alumno/imprimir";
    }

    @GetMapping("/alumno/imprimir/Contrato.pdf")
    public void generarPdfContrato(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 2. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/CONTRATO_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 3. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        // 4. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 411, 861);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 393, 848);
        if (alumno.getEmpresa() != null) { //Si ya tiene una empresa asignada entonces si se coloca en el pdf, sino no
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 203, 821.8f);
        }
        if (alumno.getPadreTutor() != null) { //Si ya tiene un Padre/Tutor entonces si se coloca en el pdf, sino no
            PadreTutor pt = alumno.getPadreTutor();
    
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
    
            String nombrePadreTutor = primerNombre + " " + primerApellido;
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 437, 273);
        }

        // 5. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 6. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 7. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Autorizacion.pdf")
    public void generarPdfAutorizacion(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 2. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/AUTORIZACION_PADRES_PEL_25.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 3. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        // 4. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 388, 767.2f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getCi(), 257, 739);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 156, 719);
        if (alumno.getEmpresa() != null) { //Si ya tiene una empresa asignada entonces si se coloca en el pdf, sino no
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 451, 698.9f);
        }
        if (alumno.getPadreTutor() != null) { //Si ya tiene un Padre/Tutor entonces si se coloca en el pdf, sino no
            PadreTutor pt = alumno.getPadreTutor();
    
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
    
            String nombrePadreTutor = primerNombre + " " + primerApellido;
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 307, 640);
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, pt.getCi(), 137, 617.3f);
        }
        //Segunda Autorizacion (la de repuesto)
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 388, 366.8f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getCi(), 257, 339);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 156, 319);
        if (alumno.getEmpresa() != null) { //Si ya tiene una empresa asignada entonces si se coloca en el pdf, sino no
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 451, 298.7f);
        }
        if (alumno.getPadreTutor() != null) { //Si ya tiene un Padre/Tutor entonces si se coloca en el pdf, sino no
            PadreTutor pt = alumno.getPadreTutor();
    
            String primerNombre = pt.getNombres().trim().split(" ")[0];
            String primerApellido = pt.getApellidos().trim().split(" ")[0];
    
            String nombrePadreTutor = primerNombre + " " + primerApellido;
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 307, 239.8f);
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, pt.getCi(), 137, 217.2f);
        }

        // 5. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 6. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Autorizacion_alumno.pdf");

        // 7. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Pel.pdf")
    public void generarPdfFichaFinalPel(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 2. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/FICHA_FINAL_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 3. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // 4. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTexto(contentStream, fuente, 10, alumno.getNombres() + " " + alumno.getApellidos(), 210, 756);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 210, 716);
        escribirTexto(contentStream, fuente, 10, String.valueOf(alumno.getEdad()), 399, 716);

        escribirTexto(contentStream, fuente, 10, alumno.getEspecialidad().getNombre(), 210, 677);
        escribirTexto(contentStream, fuente, 10, alumno.getCurso(), 402, 677);
        escribirTexto(contentStream, fuente, 10, alumno.getSeccion(), 488, 677);

        escribirTexto(contentStream, fuente, 10, alumno.getEmail(), 210, 599);
        escribirTexto(contentStream, fuente, 10, alumno.getTelefono(), 451, 599);

        List<PlanillaSemanal> todasLasPlanillas =
        planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

        if (todasLasPlanillas.isEmpty()) {
            // el alumno todavía no cargó ninguna planilla semanal
            throw new RuntimeException("El alumno no tiene planillas cargadas, no se puede generar el documento");
        }

        LocalDate fechaInicioPasantia = todasLasPlanillas.stream()
                .map(PlanillaSemanal::getFechaDesde)
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate fechaFinPasantia = todasLasPlanillas.stream()
                .map(PlanillaSemanal::getFechaHasta)
                .max(LocalDate::compareTo)
                .orElseThrow();

        Locale localeEspanol = new Locale.Builder()
                .setLanguage("es")
                .setRegion("ES")
                .build();

        DateTimeFormatter formatoLargo = new DateTimeFormatterBuilder()
                .appendPattern("d 'de' MMMM 'de' yyyy")
                .toFormatter(localeEspanol);

        escribirParrafo(contentStream, fuente, 10, fechaInicioPasantia.format(formatoLargo), 210, 560, 106, 14);
        escribirParrafo(contentStream, fuente, 10, fechaFinPasantia.format(formatoLargo), 451, 560, 106, 14);

        // 5. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 6. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Autorizacion_alumno.pdf");

        // 7. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    // Método reutilizable para centrar texto en una coordenada X específica
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
}
