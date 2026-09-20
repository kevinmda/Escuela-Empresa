package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.EscuelaEmpresa.gestor_pasantes.dto.movil.AvisoDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.AvisoService;

// Mismo aviso que la campana del header web (ver AvisoService), expuesto en
// JSON para la app. Sin push real: la app los pide al abrir Inicio, no llegan
// solos con la app cerrada (para eso hace falta un servicio como Firebase
// Cloud Messaging, que no está configurado en este proyecto).
@RestController
@RequestMapping("/api/movil/avisos")
public class AvisosMovilController {

    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvisoService avisoService;

    public AvisosMovilController(AlumnoRepository alumnoRepository, UsuarioRepository usuarioRepository,
                                  AvisoService avisoService) {
        this.alumnoRepository = alumnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.avisoService = avisoService;
    }

    @GetMapping
    public List<AvisoDTO> listar(Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        return avisoService.avisosDelAlumno(alumno).stream().map(AvisoDTO::new).toList();
    }

    // POST y no GET (a diferencia de AvisoController, la versión web): ahí el
    // GET evitaba mandar el token CSRF desde un botón suelto sin <form>; acá
    // no hay CSRF de por medio (la API usa JWT), así que va la convención
    // normal de "esto cambia un dato" = POST.
    @PostMapping("/marcar-leido")
    public void marcarLeido(@RequestParam String codigo, @RequestParam String clave,
                             Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        avisoService.marcarLeido(alumno, codigo, clave);
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
