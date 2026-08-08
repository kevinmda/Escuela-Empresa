package com.EscuelaEmpresa.gestor_pasantes.config;

import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Reemplaza el .defaultSuccessUrl("/home", true) de SecurityConfig. Ademas de redirigir,
// resetea intentosLogin y bloqueadoHasta cada vez que alguien se loguea con exito,
// para que un login correcto "limpie" cualquier historial de intentos fallidos previos.
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;

    public LoginSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        String email = authentication.getName();
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setIntentosLogin(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        });

        response.sendRedirect("/home");
    }
}
