package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.EscuelaEmpresa.gestor_pasantes.dto.movil.PerfilAlumnoDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/movil/perfil")
public class PerfilMovilController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;

    public PerfilMovilController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
    }

    @GetMapping
    public PerfilAlumnoDTO perfil(Authentication authentication) {
        return new PerfilAlumnoDTO(obtenerAlumnoAutenticado(authentication));
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
