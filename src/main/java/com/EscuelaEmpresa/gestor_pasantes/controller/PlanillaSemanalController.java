package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalPdfService;
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PlanillaSemanalController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;
    private final PlanillaSemanalService planillaSemanalService;
    private final PlanillaSemanalPdfService planillaSemanalPdfService;

    public PlanillaSemanalController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                    PlanillaSemanalRepository planillaSemanalRepository,
                                    PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository,
                                    PlanillaSemanalService planillaSemanalService,
                                    PlanillaSemanalPdfService planillaSemanalPdfService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
        this.planillaSemanalService = planillaSemanalService;
        this.planillaSemanalPdfService = planillaSemanalPdfService;
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

    @GetMapping("/alumno/planilla/Planilla_Semanal.pdf")
    public void generarPdfPlanillaSemanal(@RequestParam Integer idPs, Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 2. Buscar la planilla y validar que sea del alumno logueado
        PlanillaSemanal planilla = planillaSemanalRepository.findById(idPs)
                .orElseThrow(() -> new RuntimeException("Planilla no encontrada"));

        if (!planilla.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para generar esta planilla");
        }

        // 3. Traer los detalles
        List<PlanillaSemanalDetalle> detalles =
                planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(idPs);

        // 4. Generar el PDF usando el service compartido
        byte[] pdfBytes = planillaSemanalPdfService.generarPdf(alumno, planilla, detalles);

        // 5. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Planilla_Semanal_alumno.pdf");

        // 6. Enviar el PDF final al navegador
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/alumno/planilla/Planilla_Semanal_Vacio.pdf")
    public void generarPdfContratoVacio(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/CONTROL_SEMANAL_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 3. Enviar el PDF final al navegador
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
}