package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
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
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PlanillaSemanalController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalService planillaSemanalService;

    public PlanillaSemanalController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
            PlanillaSemanalRepository planillaSemanalRepository, PlanillaSemanalService planillaSemanalService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
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
    public void generarPdfPlanillaSemanal(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 2. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/CONTROL_SEMANAL_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 3. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float tamanioFuente = 10;

        // 4. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTexto(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 386, 778);
        escribirTexto(contentStream, fuente, 10, alumno.getNombres() + " " + alumno.getApellidos(), 227, 752);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 227, 709);
        escribirTexto(contentStream, fuente, 10, "00/00", 505, 703);
        escribirTexto(contentStream, fuente, 10, alumno.getCurso() + " " + alumno.getSeccion(), 227, 683);
        escribirTexto(contentStream, fuente, 10, "00/00", 505, 677);
        String nombreApellido = alumno.getNombres().trim().split(" ")[0] + " " + alumno.getApellidos().trim().split(" ")[0];
        escribirTexto(contentStream, fuente, tamanioFuente, nombreApellido, 86, 162);
        escribirTexto(contentStream, fuente, 10, alumno.getCi(), 128, 138);

        // 5. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 6. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Planilla_Semanal_alumno.pdf");

        // 7. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    private void escribirTexto(PDPageContentStream contentStream, PDType1Font fuente, float tamanioFuente, String texto, float posicionX, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(fuente, tamanioFuente);
        contentStream.newLineAtOffset(posicionX, y);
        contentStream.showText(texto);
        contentStream.endText();
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }
}
