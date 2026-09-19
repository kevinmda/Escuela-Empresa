package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.AvisoLeido;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AvisoLeidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

// Marca como leído uno de los avisos de la campana del header (ver
// ChromeContext.Aviso / ChromeModelAdvice). GET y no POST a propósito: es una
// acción de bajo riesgo, exclusiva de los propios datos del usuario logueado
// (no toca nada de nadie más, no es destructiva), así que evita la
// complicación de mandar el token CSRF desde un botón suelto que no vive
// dentro de ningún <form>.
@Controller
public class AvisoController {

    private static final List<String> TIPOS_VALIDOS = List.of("pendiente", "listo");

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final AvisoLeidoRepository avisoLeidoRepository;

    public AvisoController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                            AvisoLeidoRepository avisoLeidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.avisoLeidoRepository = avisoLeidoRepository;
    }

    @GetMapping("/alumno/avisos/marcar-leido")
    @ResponseBody
    public Map<String, Object> marcarLeido(@RequestParam String tipo, @RequestParam String clave,
                                            Authentication authentication) {
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new ReglaNegocioException("Tipo de aviso inválido");
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no es un alumno"));

        // Una fila por (alumno, tipo): si ya había una de un aviso anterior de
        // este mismo tipo, se pisa con la clave nueva en vez de acumular
        // historial que nadie necesita leer.
        AvisoLeido avisoLeido = avisoLeidoRepository.findByIdAlAndTipo(alumno.getIdAl(), tipo)
                .orElseGet(AvisoLeido::new);
        avisoLeido.setIdAl(alumno.getIdAl());
        avisoLeido.setTipo(tipo);
        avisoLeido.setClave(clave);
        avisoLeidoRepository.save(avisoLeido);

        return Map.of("ok", true);
    }
}
