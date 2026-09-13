package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.dto.ChromeContext;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Contrato único para el cromo autenticado. Así las once vistas no tienen que
 * reconstruir por su cuenta el rol, la identidad y la sección activa.
 */
@ControllerAdvice(annotations = Controller.class)
public class ChromeModelAdvice {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final AdministradorRepository administradorRepository;

    public ChromeModelAdvice(UsuarioRepository usuarioRepository,
                             AlumnoRepository alumnoRepository,
                             AdministradorRepository administradorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.administradorRepository = administradorRepository;
    }

    @ModelAttribute("chrome")
    public ChromeContext cargarChrome() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return null;
        }

        Administrador administrador = administradorRepository.findByUsuario_IdUsr(usuario.getIdUsr()).orElse(null);
        if (administrador != null) {
            String rol = "coordinador".equalsIgnoreCase(administrador.getCargo())
                    ? "coordinador"
                    : "administrador";
            // Administración y coordinación trabajan sobre conjuntos de especialidades;
            // el cromo queda deliberadamente neutro en vez de fingir una sola identidad.
            return new ChromeContext(
                    rol,
                    nombreCompleto(administrador.getNombres(), administrador.getApellidos(), usuario.getEmail()),
                    usuario.getEmail(),
                    null,
                    null);
        }

        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr()).orElse(null);
        if (alumno != null) {
            Especialidad especialidad = alumno.getEspecialidad();
            return new ChromeContext(
                    "alumno",
                    nombreCompleto(alumno.getNombres(), alumno.getApellidos(), usuario.getEmail()),
                    usuario.getEmail(),
                    especialidad != null ? especialidad.getIdEsp() : null,
                    especialidad != null ? especialidad.getNombre() : null);
        }

        return new ChromeContext("cuenta", usuario.getEmail(), usuario.getEmail(), null, null);
    }

    // usado por fragments/auth.html para armar og:image con URL absoluta: las
    // vistas previas de redes sociales no resuelven rutas relativas como "/img/..."
    @ModelAttribute("baseUrl")
    public String cargarBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    @ModelAttribute("paginaActual")
    public String cargarPaginaActual(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return switch (ruta) {
            case "/home" -> "inicio";
            case "/alumno/imprimir" -> "imprimir";
            case "/alumno/planilla" -> "planilla";
            case "/alumno/subir" -> "subir";
            case "/admin/alumnos" -> "alumnos";
            case "/admin/reportes" -> "reportes";
            case "/admin/supervisores" -> "supervisores";
            case "/admin/empresas" -> "empresas";
            case "/cambiar-contrasena" -> "contrasena";
            default -> "";
        };
    }

    private String nombreCompleto(String nombres, String apellidos, String respaldo) {
        String nombre = ((nombres == null ? "" : nombres) + " "
                + (apellidos == null ? "" : apellidos)).trim().replaceAll("\\s+", " ");
        return nombre.isEmpty() ? respaldo : nombre;
    }
}
