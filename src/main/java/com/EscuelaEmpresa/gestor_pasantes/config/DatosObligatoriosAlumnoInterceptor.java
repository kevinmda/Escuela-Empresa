package com.EscuelaEmpresa.gestor_pasantes.config;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

// CI, sexo, fecha de nacimiento y telefono se usan para generar los
// documentos de la pasantia: si a un alumno le falta cualquiera de los
// cuatro, esos documentos salen mal o ni se pueden armar. El formulario de
// "completar datos" del Inicio (ver HomeController.completarDatos) por si
// solo era nada mas que una sugerencia -- nada impedia que el alumno lo
// ignorara y siguiera usando Documentos/Subir/Planilla con datos
// incompletos. Este interceptor, con la misma receta que ya usa
// ContrasenaPorDefectoInterceptor con la contrasena por defecto, lo obliga a
// pasar por el Inicio y completarlos antes de dejarlo entrar a cualquier
// otra pantalla.
public class DatosObligatoriosAlumnoInterceptor implements HandlerInterceptor {

    // El propio Inicio (donde estan los formularios para completar), el
    // endpoint que los guarda, el toggle de conservar datos (vive en la
    // misma pantalla), y las rutas que ContrasenaPorDefectoInterceptor ya
    // deja pasar siempre -- un alumno con datos incompletos tiene que poder
    // seguir cambiando su contraseña o cerrando sesión igual que cualquiera.
    private static final Set<String> RUTAS_PERMITIDAS = Set.of(
            "/home",
            "/alumno/completar-datos",
            "/alumno/autoriza-conservar-datos",
            "/cambiar-contrasena",
            "/logout",
            "/privacidad",
            "/terminos"
    );

    // Igual que SESION_OK en ContrasenaPorDefectoInterceptor: una vez
    // confirmado que los cuatro datos están cargados, se marca la sesión y
    // no se vuelve a consultar la base en cada request.
    private static final String SESION_OK = "datosAlumnoCompletos";

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;

    public DatosObligatoriosAlumnoInterceptor(UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean autenticado = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        if (!autenticado) {
            return true;
        }

        HttpSession session = request.getSession();
        if (Boolean.TRUE.equals(session.getAttribute(SESION_OK))) {
            return true;
        }

        if (RUTAS_PERMITIDAS.contains(request.getServletPath())) {
            return true;
        }

        Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        if (usuario == null) {
            return true;
        }

        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr()).orElse(null);
        if (alumno == null) {
            // Administrador o coordinador: esta regla es solo del alumno. Se
            // marca igual para no volver a consultar en cada request de un
            // usuario que nunca va a tener una fila en Alumno.
            session.setAttribute(SESION_OK, Boolean.TRUE);
            return true;
        }

        if (faltaAlgunDato(alumno)) {
            response.sendRedirect(request.getContextPath() + "/home?completarDatos");
            return false;
        }

        session.setAttribute(SESION_OK, Boolean.TRUE);
        return true;
    }

    private boolean faltaAlgunDato(Alumno alumno) {
        return esVacio(alumno.getCi())
                || esVacio(alumno.getSexo())
                || alumno.getFechaNac() == null
                || esVacio(alumno.getTelefono());
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
