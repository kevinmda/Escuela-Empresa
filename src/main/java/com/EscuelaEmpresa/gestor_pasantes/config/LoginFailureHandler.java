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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

// Este handler reemplaza el comportamiento por defecto de Spring Security cuando el login falla.
// Maneja 3 casos: cuenta bloqueada temporalmente, cuenta inactiva (dispara el codigo de activacion),
// y cualquier otro fallo (contraseña incorrecta, etc.), donde ademas contamos los intentos fallidos.
//
// Como ahora el login es de 2 pasos (primero el email, despues la contraseña en login.html o
// login-primera-vez.html), todos los redirects incluyen el email como parametro, para que
// GET /login sepa que pantalla volver a mostrar sin que el usuario tenga que re-escribir el email.
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_INTENTOS_LOGIN = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    // clave de sesion donde se guarda el email de la cuenta que se esta activando,
    // para que /verificar-codigo no lo tome de un parametro manipulable
    public static final String ATRIBUTO_SESION_EMAIL_ACTIVACION = "email_activacion";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public LoginFailureHandler(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {

        String email = request.getParameter("username");

        // Spring Security envuelve lo que loadUserByUsername() lanza dentro de
        // InternalAuthenticationServiceException, guardando la excepcion real en getCause()
        Throwable causaReal = exception.getCause() != null ? exception.getCause() : exception;

        if (causaReal instanceof LockedException) {
            response.sendRedirect(urlLogin("bloqueado", email));
            return;
        }

        if (causaReal instanceof DisabledException) {
            manejarCuentaInactiva(request, response, email);
            return;
        }

        // cualquier otro caso: contraseña incorrecta (cuenta activa) o usuario inexistente
        registrarIntentoFallido(email);
        response.sendRedirect(urlLogin("error", email));
    }

    private void manejarCuentaInactiva(HttpServletRequest request, HttpServletResponse response, String email) throws IOException {
        String contrasenaIngresada = request.getParameter("password");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // verificamos manualmente que la contraseña sea correcta antes de mandar el codigo.
        // sin este chequeo, cualquiera podria escribir un email ajeno y disparar el envio igual
        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(contrasenaIngresada, usuarioOpt.get().getContrasena())) {
            response.sendRedirect(urlLogin("error", email));
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
            response.sendRedirect(urlLogin("errorCorreo", email));
            return;
        }

        // El email va a la sesion del servidor, no como parametro de la URL: asi la
        // pantalla de verificacion no puede ser apuntada a la cuenta de otro (mismo
        // criterio que el flujo de recuperacion de contraseña).
        request.getSession().setAttribute(ATRIBUTO_SESION_EMAIL_ACTIVACION, email);
        response.sendRedirect("/verificar-codigo");
    }

    private void registrarIntentoFallido(String email) {
        if (email == null) return;

        // dos sentencias atómicas: sube el contador (solo si la cuenta está activa)
        // y, si con eso llegó al máximo, la bloquea y reinicia el contador. Sin
        // leer-modificar-guardar, así intentos simultáneos no pierden incrementos.
        usuarioRepository.incrementarIntentosLogin(email);
        usuarioRepository.bloquearSiSuperaIntentos(
                email,
                LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO),
                MAX_INTENTOS_LOGIN);
    }

    private String urlLogin(String parametro, String email) {
        if (email == null) {
            return "/login?" + parametro;
        }
        return "/login?" + parametro + "&email=" + codificar(email);
    }

    private String codificar(String valor) {
        try {
            return URLEncoder.encode(valor, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return valor; // UTF-8 siempre esta disponible, esto no deberia pasar nunca
        }
    }

    private String generarCodigoNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000); // siempre 6 digitos, entre 100000 y 999999
        return String.valueOf(numero);
    }
}
