package com.EscuelaEmpresa.gestor_pasantes.controller;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.EscuelaEmpresa.gestor_pasantes.dto.PlanillaSemanalForm;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PlanillaSemanalController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;
    private final PlanillaSemanalService planillaSemanalService;

    public PlanillaSemanalController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                    PlanillaSemanalRepository planillaSemanalRepository,
                                    PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository,
                                    PlanillaSemanalService planillaSemanalService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
        this.planillaSemanalService = planillaSemanalService;
    }

    @GetMapping("/alumno/planilla")
    public String mostrarFormulario(@RequestParam(required = false) Integer idPs,
                                    Model model,
                                    Authentication authentication) {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        List<PlanillaSemanal> planillas =
            planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        model.addAttribute("planillas", planillas);

        PlanillaSemanalForm form;

        if (idPs != null) {
            PlanillaSemanal planilla = planillaSemanalRepository.findById(idPs)
                .orElseThrow(() -> new RuntimeException("Planilla no encontrada"));

            // Seguridad: que el alumno no pueda ver planillas ajenas cambiando el idPs en la URL
            if (!planilla.getAlumno().getIdAl().equals(alumno.getIdAl())) {
                throw new AccessDeniedException("No tenés permiso para ver esta planilla");
            }

            form = planillaSemanalService.cargarParaEdicion(planilla);
            model.addAttribute("idPsSeleccionado", idPs);
        } else {
            form = new PlanillaSemanalForm();
        }

        model.addAttribute("planillaForm", form);
        model.addAttribute("habilitado", false); // siempre arranca deshabilitado, sea nuevo o cargado
        return "alumno/planillaSemanal";
    }

    @PostMapping("/alumno/planilla")
    public String guardarFormulario(@ModelAttribute("planillaForm") PlanillaSemanalForm form,
                                     Authentication authentication,
                                     Model model) {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        try {
            planillaSemanalService.guardarPlanilla(form, alumno);
            model.addAttribute("exito", "Planilla guardada correctamente.");
            model.addAttribute("planillaForm", new PlanillaSemanalForm());
            model.addAttribute("habilitado", false);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("planillaForm", form);
            model.addAttribute("habilitado", true);
        }

        List<PlanillaSemanal> planillas =
        planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        model.addAttribute("planillas", planillas);

        return "alumno/planillaSemanal";

    }

    @GetMapping("/alumno/planilla/PlanillaSemanal.pdf")
    public void generarPdfPlanillaSemanal(@RequestParam Integer idPs, Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 2. Buscar la planilla y validar que sea del alumno logueado
        PlanillaSemanal planilla = planillaSemanalRepository.findById(idPs)
                .orElseThrow(() -> new RuntimeException("Planilla no encontrada"));

        if (!planilla.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para generar esta planilla");
        }

        // 3. Traer los detalles y ordenarlos por día de la semana
        List<PlanillaSemanalDetalle> detalles =
                planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(idPs);

        // 4. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/CONTROL_SEMANAL_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 5. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM");

        // 6. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTexto(contentStream, fuente, 10, alumno.getEspecialidad().getNombre(), 386, 778);
        escribirTexto(contentStream, fuente, 10, alumno.getNombres() + " " + alumno.getApellidos(), 227, 752);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 227, 709);
        escribirTexto(contentStream, fuente, 10, planilla.getFechaDesde().format(formatoFecha), 505, 703);
        escribirTexto(contentStream, fuente, 10, alumno.getCurso() + " " + alumno.getSeccion(), 227, 683);
        escribirTexto(contentStream, fuente, 10, planilla.getFechaHasta().format(formatoFecha), 505, 677);

        // 7. Días de la semana, tomados de los detalles según DayOfWeek
        escribirDia(contentStream, fuente, detalles, DayOfWeek.MONDAY, 574);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.TUESDAY, 554);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.WEDNESDAY, 534);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.THURSDAY, 514);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.FRIDAY, 494);
        escribirDia(contentStream, fuente, detalles, DayOfWeek.SATURDAY, 474);

        escribirTexto(contentStream, fuente, 10, formatearHoras(planilla.getTotalHoras()), 499, 454);

        // 8. Párrafos
        escribirParrafo(contentStream, fuente, 9, planilla.getConocimientos(), 250, 392, 288, 14);
        escribirParrafo(contentStream, fuente, 9, planilla.getExperiencia(), 250, 332, 288, 14);
        escribirParrafo(contentStream, fuente, 9, planilla.getAprendizaje(), 250, 287, 288, 14);

        String nombreApellido = alumno.getNombres().trim().split(" ")[0] + " " + alumno.getApellidos().trim().split(" ")[0];
        escribirTexto(contentStream, fuente, 10, nombreApellido, 86, 162);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 128, 138);

        // 9. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 10. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Planilla_Semanal_alumno.pdf");

        // 11. Enviar el PDF final al navegador
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
