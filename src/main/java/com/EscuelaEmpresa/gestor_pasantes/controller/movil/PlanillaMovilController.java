package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.EscuelaEmpresa.gestor_pasantes.dto.PlanillaSemanalForm;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.DiaDTO;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.PlanillaDetalleDTO;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.PlanillaResumenDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalPdfService;
import com.EscuelaEmpresa.gestor_pasantes.service.PlanillaSemanalService;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

// Mismos flujos que PlanillaSemanalController (web), pero en JSON. guardarPlanilla()
// solo hace INSERT (nunca UPDATE) tanto aca como en la web: no existe "editar" una
// planilla ya guardada, por eso no hay PUT, solo POST (crear) y ver/borrar.
@RestController
@RequestMapping("/api/movil/planillas")
public class PlanillaMovilController {

    private static final String[] NOMBRES_DIA =
            {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;
    private final PlanillaSemanalService planillaSemanalService;
    private final PlanillaSemanalPdfService planillaSemanalPdfService;

    public PlanillaMovilController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
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

    @GetMapping
    public Page<PlanillaResumenDTO> listar(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        return planillaSemanalRepository
                .findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl(), PageRequest.of(page, size))
                .map(PlanillaResumenDTO::new);
    }

    @GetMapping("/{idPs}")
    public PlanillaDetalleDTO detalle(@PathVariable Integer idPs, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PlanillaSemanal planilla = obtenerPlanillaPropia(idPs, alumno);

        List<PlanillaSemanalDetalle> detalles =
                planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(idPs);

        List<DiaDTO> dias = detalles.stream()
                .map(d -> new DiaDTO(nombreDia(d.getFecha()), d.getFecha(), d.getDescripcion(), d.getHoras()))
                .toList();

        return new PlanillaDetalleDTO(planilla, dias);
    }

    // Mismo PDF que /alumno/planilla/Planilla_Semanal.pdf en la web (sobre la
    // plantilla oficial del PEL), reusando el mismo service -- nada de logica
    // nueva aca, solo el transporte a bytes para que la app lo abra con un
    // visor de PDF del sistema (ver AbrirPdf.java en el proyecto Android).
    @GetMapping("/{idPs}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Integer idPs, Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PlanillaSemanal planilla = obtenerPlanillaPropia(idPs, alumno);

        List<PlanillaSemanalDetalle> detalles =
                planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(idPs);

        byte[] pdfBytes = planillaSemanalPdfService.generarPdf(alumno, planilla, detalles);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", Descarga.inline("Planilla_Semanal.pdf", "Planilla_Semanal.pdf"))
                .body(pdfBytes);
    }

    @PostMapping
    public PlanillaResumenDTO crear(@RequestBody PlanillaSemanalForm form, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PlanillaSemanal creada = planillaSemanalService.guardarPlanilla(form, alumno);
        return new PlanillaResumenDTO(creada);
    }

    @DeleteMapping("/{idPs}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idPs, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PlanillaSemanal planilla = obtenerPlanillaPropia(idPs, alumno);

        planillaSemanalService.eliminarPlanilla(planilla);
        return ResponseEntity.noContent().build();
    }

    private PlanillaSemanal obtenerPlanillaPropia(Integer idPs, Alumno alumno) {
        PlanillaSemanal planilla = planillaSemanalRepository.findById(idPs)
                .orElseThrow(() -> new RecursoNoEncontradoException("Planilla no encontrada"));

        if (!planilla.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para acceder a esta planilla");
        }
        return planilla;
    }

    private String nombreDia(LocalDate fecha) {
        return NOMBRES_DIA[fecha.getDayOfWeek().getValue() - 1];
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
