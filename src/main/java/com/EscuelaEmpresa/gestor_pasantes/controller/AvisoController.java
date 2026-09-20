package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.AvisoService;

// Marca como leído uno de los avisos de la campana del header (ver
// ChromeContext.Aviso / ChromeModelAdvice). GET y no POST a propósito: es una
// acción de bajo riesgo, exclusiva de los propios datos del usuario logueado
// (no toca nada de nadie más, no es destructiva), así que evita la
// complicación de mandar el token CSRF desde un botón suelto que no vive
// dentro de ningún <form>.
@Controller
public class AvisoController {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final AvisoService avisoService;

    public AvisoController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                            AvisoService avisoService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.avisoService = avisoService;
    }

    @GetMapping("/alumno/avisos/marcar-leido")
    @ResponseBody
    public Map<String, Object> marcarLeido(@RequestParam String codigo, @RequestParam String clave,
                                            Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no es un alumno"));

        avisoService.marcarLeido(alumno, codigo, clave);

        return Map.of("ok", true);
    }
}
