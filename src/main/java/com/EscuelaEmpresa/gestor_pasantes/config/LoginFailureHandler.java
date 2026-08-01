package com.EscuelaEmpresa.gestor_pasantes.config;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
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
// Por defecto, CUALQUIER fallo (contraseña mal, usuario no existe, cuenta deshabilitada) redirige
// a /login?error con el mismo mensaje generico. Nosotros necesitamos un comportamiento especial
// SOLO cuando el fallo es por cuenta deshabilitada (DisabledException).
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // este es el comportamiento por defecto de Spring Security (redirigir a /login?error).
    // lo reutilizamos para todos los casos que NO sean "cuenta deshabilitada"
    private final AuthenticationFailureHandler handlerPorDefecto = new SimpleUrlAuthenticationFailureHandler("/login?error");

    public LoginFailureHandler(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {

        // si el fallo no es por cuenta deshabilitada, usamos el comportamiento normal y listo
        if (!(exception instanceof DisabledException)) {
            handlerPorDefecto.onAuthenticationFailure(request, response, exception);
            return;
        }

        // recuperamos lo que el usuario tipeo en el formulario (Spring Security todavia
        // tiene estos parametros disponibles en el request en este punto)
        String email = request.getParameter("username");
        String contrasenaIngresada = request.getParameter("password");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // verificamos manualmente que la contraseña sea correcta antes de mandar el codigo.
        // esto es IMPORTANTE: sin este chequeo, cualquiera podria escribir un email ajeno
        // (sin saber la contraseña) y disparar el envio de un codigo igual
        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(contrasenaIngresada, usuarioOpt.get().getContrasena())) {
            handlerPorDefecto.onAuthenticationFailure(request, response, exception);
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // generamos un codigo numerico de 6 digitos (mas facil de tipear a mano que un UUID largo)
        String codigo = generarCodigoNumerico();

        usuario.setTokenActivacion(codigo);
        usuario.setTokenExpiracion(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        emailService.enviarCorreoCodigoActivacion(usuario.getEmail(), codigo);

        // redirigimos a la pantalla donde el usuario va a ingresar el codigo,
        // pasando el email como parametro para no tener que pedirselo de nuevo
        response.sendRedirect("/verificar-codigo?email=" + email);
    }

    private String generarCodigoNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000); // siempre 6 digitos, entre 100000 y 999999
        return String.valueOf(numero);
    }
}
