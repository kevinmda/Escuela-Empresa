package com.EscuelaEmpresa.gestor_pasantes.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import com.EscuelaEmpresa.gestor_pasantes.service.InformePasantiaService;
import com.EscuelaEmpresa.gestor_pasantes.service.NombresDeArchivo;
import com.EscuelaEmpresa.gestor_pasantes.service.Plantillas;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;

@Controller
public class PlanillaSemanalController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;
    private final PlanillaSemanalService planillaSemanalService;
    private final PlanillaSemanalPdfService planillaSemanalPdfService;
    private final InformePasantiaService informePasantiaService;

    public PlanillaSemanalController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                    PlanillaSemanalRepository planillaSemanalRepository,
                                    PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository,
                                    PlanillaSemanalService planillaSemanalService,
                                    PlanillaSemanalPdfService planillaSemanalPdfService,
                                    InformePasantiaService informePasantiaService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
        this.planillaSemanalService = planillaSemanalService;
        this.planillaSemanalPdfService = planillaSemanalPdfService;
        this.informePasantiaService = informePasantiaService;
    }

    @GetMapping("/alumno/planilla")
    public String mostrarFormulario(@RequestParam(required = false) Integer idPs,
                                    Model model,
                                    Authentication authentication) {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        List<PlanillaSemanal> planillas =
            planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        model.addAttribute("planillas", planillas);
        agregarEstadoInforme(model, alumno, planillas);

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
        agregarEstadoInforme(model, alumno, planillas);

        return "alumno/planillaSemanal";

    }

    @PostMapping("/alumno/planilla/{idPs}/eliminar")
    public String eliminarPlanilla(@PathVariable Integer idPs,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        PlanillaSemanal planilla = planillaSemanalRepository.findById(idPs)
                .orElseThrow(() -> new RuntimeException("Planilla no encontrada"));

        // seguridad: que el alumno no pueda borrar planillas ajenas cambiando el idPs en la URL
        if (!planilla.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para eliminar esta planilla");
        }

        planillaSemanalService.eliminarPlanilla(planilla);

        redirectAttributes.addFlashAttribute("exito", "Planilla eliminada correctamente.");
        return "redirect:/alumno/planilla";
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
        PDDocument document = Plantillas.abrirPdf("CONTROL_SEMANAL_PEL_2025.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/planilla/Informe_Pasantia.docx")
    public void generarInformePasantia(Authentication authentication, HttpServletResponse response,
                                        RedirectAttributes redirectAttributes) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // ordenamos de la semana mas antigua a la mas nueva (el repositorio las trae al reves)
        List<PlanillaSemanal> planillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        Collections.reverse(planillas);

        if (planillas.size() < 6) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no completaste las 6 semanas de planilla, no se puede generar el informe.");
            response.sendRedirect("/alumno/planilla");
            return;
        }

        if (alumno.getSupervisor() == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no tenés un supervisor/docente asignado, no se puede generar el informe.");
            response.sendRedirect("/alumno/planilla");
            return;
        }

        if (alumno.getEmpresa() == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no tenés una empresa asignada, no se puede generar el informe.");
            response.sendRedirect("/alumno/planilla");
            return;
        }

        XWPFDocument documento = informePasantiaService.generarInforme(alumno, planillas);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        documento.write(salida);
        documento.close();

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", NombresDeArchivo.contentDisposition(
                "attachment",
                "Informe_Pasantia_" + alumno.getNombres() + alumno.getApellidos() + ".docx",
                "Informe_Pasantia.docx"));
        response.getOutputStream().write(salida.toByteArray());
        response.getOutputStream().flush();
    }

    // El informe de pasantia solo se puede descargar si el alumno ya completo las 6 semanas,
    // y tiene supervisor/docente y empresa asignados. Se llama tanto desde el GET como el POST
    // de /alumno/planilla, para que la pantalla nunca le falten estos atributos al modelo
    // Estado que necesitan las dos partes de la pantalla: los requisitos del
    // informe final, y el riel de las seis semanas.
    private void agregarEstadoInforme(Model model, Alumno alumno, List<PlanillaSemanal> planillas) {

        // "planillas" viene ordenada de la mas nueva a la mas vieja, que es lo que
        // servia para un desplegable. El riel muestra las seis semanas en el orden
        // en que pasaron, asi que la vista necesita la lista al reves.
        List<PlanillaSemanal> planillasAsc = new ArrayList<>(planillas);
        Collections.reverse(planillasAsc);
        model.addAttribute("planillasAsc", planillasAsc);

        // El color de la especialidad del alumno. La pantalla lo usa igual que su
        // inicio: es su color, y esta es su planilla. Sin especialidad cargada el
        // bloque base de [data-esp] devuelve el par --ink / --text-on-ink.
        model.addAttribute("idEspecialidad",
                alumno.getEspecialidad() != null ? alumno.getEspecialidad().getIdEsp() : null);

        boolean tieneSeisPlanillas = planillas.size() >= 6;
        boolean tieneSupervisor = alumno.getSupervisor() != null;
        boolean tieneEmpresa = alumno.getEmpresa() != null;
        boolean informeHabilitado = tieneSeisPlanillas && tieneSupervisor && tieneEmpresa;

        model.addAttribute("informeHabilitado", informeHabilitado);
        model.addAttribute("tieneSeisPlanillas", tieneSeisPlanillas);
        model.addAttribute("tieneSupervisor", tieneSupervisor);
        model.addAttribute("tieneEmpresa", tieneEmpresa);
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }
}