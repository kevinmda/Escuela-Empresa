package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.FormularioPdfService;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

// Mismos 4 formularios que ImprimirController expone en /alumno/imprimir/*.pdf
// para la web, generados con el mismo FormularioPdfService -- no hay logica
// nueva aca, solo el transporte a bytes para la app. Quedan afuera las
// versiones "_Vacio" (plantillas en blanco, sin datos del alumno): no tienen
// sentido en un celular personal donde el unico usuario es el dueno de la cuenta.
@RestController
@RequestMapping("/api/movil/formularios")
public class FormulariosMovilController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final FormularioPdfService formularioPdfService;

    public FormulariosMovilController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                       PlanillaSemanalRepository planillaSemanalRepository,
                                       FormularioPdfService formularioPdfService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.formularioPdfService = formularioPdfService;
    }

    @GetMapping("/contrato")
    public ResponseEntity<byte[]> contrato(Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        List<PlanillaSemanal> planillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

        return pdf(formularioPdfService.generarContrato(alumno, planillas), "Contrato.pdf");
    }

    @GetMapping("/autorizacion")
    public ResponseEntity<byte[]> autorizacion(Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        return pdf(formularioPdfService.generarAutorizacion(alumno), "Autorizacion.pdf");
    }

    @GetMapping("/ficha-final")
    public ResponseEntity<byte[]> fichaFinal(Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        List<PlanillaSemanal> planillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

        return pdf(formularioPdfService.generarFichaFinal(alumno, planillas), "Ficha_Final.pdf");
    }

    @GetMapping("/ficha-final-evaluativa")
    public ResponseEntity<byte[]> fichaFinalEvaluativa(Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        return pdf(formularioPdfService.generarFichaFinalEvaluativa(alumno), "Ficha_Final_Eval.pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", Descarga.inline(nombreArchivo, nombreArchivo))
                .body(contenido);
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
