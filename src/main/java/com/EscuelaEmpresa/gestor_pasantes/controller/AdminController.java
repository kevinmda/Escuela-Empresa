package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.EscuelaEmpresa.gestor_pasantes.dto.AlumnoFiltradoDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.EspecialidadRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
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

    public AdminController(UsuarioRepository usuarioRepository,
                            AdministradorRepository administradorRepository,
                            EspecialidadRepository especialidadRepository,
                            AlumnoRepository alumnoRepository,
                            PlanillaSemanalRepository planillaSemanalRepository,
                            PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository,
                            PlanillaSemanalPdfService planillaSemanalPdfService) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.especialidadRepository = especialidadRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
        this.planillaSemanalPdfService = planillaSemanalPdfService;
    }

    @GetMapping("/admin/alumnos")
    public String alumnos(Model model, Authentication authentication) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        boolean esAdministrativo = "administrativo".equalsIgnoreCase(admin.getCargo());

        model.addAttribute("esAdministrativo", esAdministrativo);

        if (esAdministrativo) {
            // Ve todas las especialidades y elige una en el filtro
            model.addAttribute("especialidades", especialidadRepository.findAll());
        } else {
            // Coordinador: su especialidad ya viene fija, no elige
            if (admin.getEspecialidades() == null || admin.getEspecialidades().isEmpty()) {
                throw new RuntimeException("El coordinador no tiene especialidad asignada");
            }
            Especialidad especialidadFija = admin.getEspecialidades().get(0);
            model.addAttribute("especialidadFija", especialidadFija);
        }

        return "administrador/alumnos";
    }

    @GetMapping("/admin/reportes")
    public String reportes() {
        return "administrador/reportes";
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