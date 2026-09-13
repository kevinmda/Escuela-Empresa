package com.EscuelaEmpresa.gestor_pasantes.config;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
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

// Mientras un usuario tenga contrasena_por_defecto = true (la contrasena inicial,
// compartida entre varios alumnos), este interceptor lo obliga a pasar por
// /cambiar-contrasena antes de poder usar cualquier otra pantalla. Sin esto, el
// aviso de "cambiá tu contraseña" era solo un cartel y la contraseña compartida
// seguía sirviendo para siempre.
public class ContrasenaPorDefectoInterceptor implements HandlerInterceptor {

    // Rutas a las que sí se puede llegar aunque tengas la contraseña por defecto:
    // la propia pantalla de cambio, el logout, y las dos páginas legales. Estas
    // últimas porque el pie de la pantalla de cambio de contraseña las enlaza: sin
    // esta excepción, el alumno que quisiera leer qué se guarda sobre él antes de
    // seguir usando el sistema sería rebotado de vuelta a cambiar la contraseña.
    private static final Set<String> RUTAS_PERMITIDAS = Set.of(
            "/cambiar-contrasena",
            "/logout",
            "/privacidad",
            "/terminos"
    );

    // Una vez que se confirma que el usuario ya NO tiene la contraseña por defecto,
    // se marca la sesión y no se vuelve a consultar la base en cada request.
    private static final String SESION_OK = "contrasenaPropiaVerificada";

    private final UsuarioRepository usuarioRepository;

    public ContrasenaPorDefectoInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
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
        if (usuario != null && Boolean.TRUE.equals(usuario.getContrasenaPorDefecto())) {
            response.sendRedirect(request.getContextPath() + "/cambiar-contrasena?forzar");
            return false;
        }

        session.setAttribute(SESION_OK, Boolean.TRUE);
        return true;
    }
}
