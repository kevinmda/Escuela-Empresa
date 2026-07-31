package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.EscuelaEmpresa.gestor_pasantes.dto.AlumnoCumplimientoDTO;
import com.EscuelaEmpresa.gestor_pasantes.dto.AlumnoFiltradoDTO;
import com.EscuelaEmpresa.gestor_pasantes.dto.CumplimientoSemanaDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.entity.Supervisor;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.EspecialidadRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.SupervisorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalPdfService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;
    private final PlanillaSemanalPdfService planillaSemanalPdfService;
    private final SupervisorRepository supervisorRepository;

    public AdminController(UsuarioRepository usuarioRepository,
                            AdministradorRepository administradorRepository,
                            EspecialidadRepository especialidadRepository,
                            AlumnoRepository alumnoRepository,
                            PlanillaSemanalRepository planillaSemanalRepository,
                            PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository,
                            PlanillaSemanalPdfService planillaSemanalPdfService,
                            SupervisorRepository supervisorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.especialidadRepository = especialidadRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
        this.planillaSemanalPdfService = planillaSemanalPdfService;
        this.supervisorRepository = supervisorRepository;
    }

    @GetMapping("/admin/alumnos")
    public String alumnos(Model model, Authentication authentication) {
        cargarFiltroEnModelo(model, authentication);
        return "administrador/alumnos";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model, Authentication authentication) {
        cargarFiltroEnModelo(model, authentication);
        return "administrador/reportes";
    }

    // Arma los datos del filtro (Especialidad/es o especialidad fija) que usan tanto
    // la pantalla de Alumnos como la de Reportes
    private void cargarFiltroEnModelo(Model model, Authentication authentication) {
        Administrador admin = obtenerAdminAutenticado(authentication);
        boolean esAdministrativo = "administrativo".equalsIgnoreCase(admin.getCargo());

        model.addAttribute("esAdministrativo", esAdministrativo);

        if (esAdministrativo) {
            model.addAttribute("especialidades", especialidadRepository.findAll());
        } else {
            if (admin.getEspecialidades() == null || admin.getEspecialidades().isEmpty()) {
                throw new RuntimeException("El coordinador no tiene especialidad asignada");
            }
            Especialidad especialidadFija = admin.getEspecialidades().get(0);
            model.addAttribute("especialidadFija", especialidadFija);
        }
    }

    // --- Endpoints AJAX para el filtro en cascada ---

    @GetMapping("/admin/api/cursos")
    @ResponseBody
    public List<String> cursosPorEspecialidad(@RequestParam Integer idEsp) {
        return alumnoRepository.findCursosPorEspecialidad(idEsp);
    }

    @GetMapping("/admin/api/secciones")
    @ResponseBody
    public List<String> seccionesPorEspecialidadYCurso(@RequestParam Integer idEsp, @RequestParam String curso) {
        return alumnoRepository.findSeccionesPorEspecialidadYCurso(idEsp, curso);
    }

    @GetMapping("/admin/api/alumnos-filtrados")
    @ResponseBody
    public List<AlumnoFiltradoDTO> alumnosFiltrados(@RequestParam Integer idEsp,
                                                      @RequestParam String curso,
                                                      @RequestParam String seccion) {
        return alumnoRepository.findByEspecialidad_IdEspAndCursoAndSeccion(idEsp, curso, seccion)
                .stream()
                .map(AlumnoFiltradoDTO::new)
                .toList();
    }

    // --- Reporte de cumplimiento semanal ---

    @GetMapping("/admin/api/cumplimiento")
    @ResponseBody
    public CumplimientoSemanaDTO cumplimiento(@RequestParam Integer idEsp,
                                               @RequestParam String curso,
                                               @RequestParam String seccion,
                                               @RequestParam(defaultValue = "0") int offset) {

        // 1. Calcular el lunes y el sábado de la semana consultada (offset 0 = semana actual,
        // -1 = semana anterior, +1 = semana siguiente, etc.)
        LocalDate lunesSemanaActual = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate semanaDesde = lunesSemanaActual.plusWeeks(offset);
        LocalDate semanaHasta = semanaDesde.plusDays(5); // lunes a sábado

        // 2. Traer los alumnos del filtro y chequear, uno por uno, si entregaron esa semana
        List<Alumno> alumnos = alumnoRepository.findByEspecialidad_IdEspAndCursoAndSeccion(idEsp, curso, seccion);

        List<AlumnoCumplimientoDTO> resultado = alumnos.stream()
                .map(alumno -> {
                    boolean entrego = planillaSemanalRepository
                            .existsByAlumno_IdAlAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
                                    alumno.getIdAl(), semanaHasta, semanaDesde);
                    return new AlumnoCumplimientoDTO(alumno.getIdAl(), alumno.getNombres(),
                            alumno.getApellidos(), alumno.getCi(), entrego);
                })
                .toList();

        return new CumplimientoSemanaDTO(semanaDesde, semanaHasta, resultado);
    }

    // --- Descarga en ZIP de todas las planillas de los alumnos filtrados ---

    @GetMapping("/admin/alumnos/zip")
    public void descargarZip(@RequestParam Integer idEsp,
                              @RequestParam String curso,
                              @RequestParam String seccion,
                              HttpServletResponse response) throws IOException {

        List<Alumno> alumnos = alumnoRepository.findByEspecialidad_IdEspAndCursoAndSeccion(idEsp, curso, seccion);

        String nombreZip = "Planillas_" + sanitizar(curso) + "_" + sanitizar(seccion) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreZip);

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            for (Alumno alumno : alumnos) {

                List<PlanillaSemanal> planillas =
                        planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

                if (planillas.isEmpty()) {
                    continue; // este alumno no cargó ninguna planilla todavía, no agrega nada
                }

                String carpetaAlumno = sanitizar(alumno.getApellidos() + "_" + alumno.getNombres());

                for (PlanillaSemanal planilla : planillas) {

                    List<PlanillaSemanalDetalle> detalles =
                            planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(planilla.getIdPs());

                    byte[] pdfBytes = planillaSemanalPdfService.generarPdf(alumno, planilla, detalles);

                    String nombreArchivo = carpetaAlumno + "/PlanillaSemanal_"
                            + planilla.getFechaDesde() + "_a_" + planilla.getFechaHasta() + ".pdf";

                    zos.putNextEntry(new ZipEntry(nombreArchivo));
                    zos.write(pdfBytes);
                    zos.closeEntry();
                }
            }
        }
    }

    // --- Gestión de Supervisores (solo Coordinador) ---

    @GetMapping("/admin/supervisores")
    public String supervisores(Model model, Authentication authentication) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        List<Supervisor> supervisores = supervisorRepository.findByEspecialidad_IdEsp(especialidadFija.getIdEsp());
        List<Alumno> alumnos = alumnoRepository.findByEspecialidad_IdEsp(especialidadFija.getIdEsp());

        model.addAttribute("especialidadFija", especialidadFija);
        model.addAttribute("supervisores", supervisores);
        model.addAttribute("alumnos", alumnos);

        return "coordinador/supervisores";
    }

    @PostMapping("/admin/supervisores/crear")
    public String crearSupervisor(@RequestParam String nombres,
                                   @RequestParam String apellidos,
                                   @RequestParam String email,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Supervisor supervisor = new Supervisor();
        supervisor.setNombres(nombres);
        supervisor.setApellidos(apellidos);
        supervisor.setEmail(email);
        supervisor.setEspecialidad(especialidadFija);

        supervisorRepository.save(supervisor);

        redirectAttributes.addFlashAttribute("exito", "Supervisor agregado correctamente.");
        return "redirect:/admin/supervisores";
    }

    @PostMapping("/admin/supervisores/asignar")
    public String asignarSupervisor(@RequestParam Integer idAl,
                                     @RequestParam(required = false) Integer idSup,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Alumno alumno = alumnoRepository.findById(idAl)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        // Seguridad: el coordinador solo puede tocar alumnos de su propia especialidad
        if (!alumno.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés modificar un alumno de otra especialidad");
        }

        if (idSup == null) {
            alumno.setSupervisor(null); // se puede desasignar eligiendo "-- Sin asignar --"
        } else {
            Supervisor supervisor = supervisorRepository.findById(idSup)
                    .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));

            if (!supervisor.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
                throw new AccessDeniedException("Ese supervisor no pertenece a tu especialidad");
            }

            alumno.setSupervisor(supervisor);
        }

        alumnoRepository.save(alumno);

        redirectAttributes.addFlashAttribute("exito", "Supervisor actualizado correctamente.");
        return "redirect:/admin/supervisores";
    }

    private Especialidad obtenerEspecialidadDeCoordinador(Administrador admin) {
        boolean esAdministrativo = "administrativo".equalsIgnoreCase(admin.getCargo());

        if (esAdministrativo) {
            // La pantalla de Supervisores es exclusiva de Coordinación
            throw new AccessDeniedException("Esta pantalla es solo para coordinadores");
        }

        if (admin.getEspecialidades() == null || admin.getEspecialidades().isEmpty()) {
            throw new RuntimeException("El coordinador no tiene especialidad asignada");
        }

        return admin.getEspecialidades().get(0);
    }

    private String sanitizar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private Administrador obtenerAdminAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return administradorRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
    }
}