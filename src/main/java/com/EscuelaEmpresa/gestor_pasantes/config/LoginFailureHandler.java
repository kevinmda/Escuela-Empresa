package com.EscuelaEmpresa.gestor_pasantes.config;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.EmailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

// Este handler reemplaza el comportamiento por defecto de Spring Security cuando el login falla.
// Maneja 3 casos distintos: cuenta bloqueada temporalmente, cuenta inactiva (primera vez,
// dispara el codigo de activacion), y cualquier otro fallo (contraseña incorrecta, etc.),
// donde ademas llevamos la cuenta de intentos fallidos para bloquear tras varios intentos.
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_INTENTOS_LOGIN = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // comportamiento por defecto de Spring Security (redirigir a /login?error), reusado
    // para todos los casos que no necesitan tratamiento especial
    private final AuthenticationFailureHandler handlerPorDefecto = new SimpleUrlAuthenticationFailureHandler("/login?error");

    public LoginFailureHandler(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {

        // Spring Security envuelve lo que loadUserByUsername() lanza dentro de
        // InternalAuthenticationServiceException, guardando la excepcion real en getCause()
        Throwable causaReal = exception.getCause() != null ? exception.getCause() : exception;

        if (causaReal instanceof LockedException) {
            response.sendRedirect("/login?bloqueado");
            return;
        }

        if (causaReal instanceof DisabledException) {
            manejarCuentaInactiva(request, response);
            return;
        }

        // cualquier otro caso: contraseña incorrecta (cuenta activa) o usuario inexistente
        registrarIntentoFallido(request);
        handlerPorDefecto.onAuthenticationFailure(request, response, exception);
    }

    private void manejarCuentaInactiva(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("username");
        String contrasenaIngresada = request.getParameter("password");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // verificamos manualmente que la contraseña sea correcta antes de mandar el codigo.
        // sin este chequeo, cualquiera podria escribir un email ajeno y disparar el envio igual
        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(contrasenaIngresada, usuarioOpt.get().getContrasena())) {
            response.sendRedirect("/login?error");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        String codigo = generarCodigoNumerico();

        usuario.setTokenActivacion(codigo);
        usuario.setTokenExpiracion(LocalDateTime.now().plusMinutes(5));
        usuario.setIntentosCodigo(0); // codigo nuevo, el contador de intentos arranca de cero
        usuarioRepository.save(usuario);

        try {
            emailService.enviarCorreoCodigoActivacion(usuario.getEmail(), codigo);
        } catch (MailException e) {
            // el correo no se pudo enviar (Gmail caido, mal configurado, etc.)
            // evitamos que esto termine en un error 500 generico
            response.sendRedirect("/login?errorCorreo");
            return;
        }

        response.sendRedirect("/verificar-codigo?email=" + email);
    }

    private void registrarIntentoFallido(HttpServletRequest request) {
        String email = request.getParameter("username");
        if (email == null) return;

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) return;

        Usuario usuario = usuarioOpt.get();

        // solo aplica el conteo a cuentas activas (las inactivas se manejan aparte, arriba)
        if (!Boolean.TRUE.equals(usuario.getActivo())) return;

        int intentos = usuario.getIntentosLogin() == null ? 0 : usuario.getIntentosLogin();
        intentos++;

        if (intentos >= MAX_INTENTOS_LOGIN) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            usuario.setIntentosLogin(0); // reinicia el contador para el proximo ciclo
        } else {
            usuario.setIntentosLogin(intentos);
        }

        usuarioRepository.save(usuario);
    }

    private String generarCodigoNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000); // siempre 6 digitos, entre 100000 y 999999
        return String.valueOf(numero);
    }
}
