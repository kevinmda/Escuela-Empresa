package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class CambiarContrasenaController {

    // El mismo minimo que pide /restablecer-contrasena. Son las dos puertas de
    // entrada a una contraseña nueva y no tienen por que exigir cosas distintas.
    private static final int LARGO_MINIMO = 6;

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final SessionRegistry sessionRegistry;

    CambiarContrasenaController(PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository,
            SessionRegistry sessionRegistry) {
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/cambiar-contrasena")
    public String mostrarFormulario() {
        return "cambiar-contrasena";
    }

    @PostMapping("/cambiar-contrasena")
    public String cambiarContrasena(Authentication authentication,
            @RequestParam String actual,
            @RequestParam String nueva,
            @RequestParam(required = false) String confirmarNueva,
            HttpServletRequest request) { //el objeto authentication guarda internamente quien esta autentificado (logueado)

        // La confirmacion se compara ACA, no solo en el navegador. theme.js ya compara
        // los dos campos, pero eso desaparece con el JS caido o con un POST armado a
        // mano, y entonces un error de tipeo cambia la contraseña por una que el
        // usuario no sabe cual es: queda afuera de su propia cuenta.
        if (confirmarNueva == null || !confirmarNueva.equals(nueva)) {
            return "redirect:/cambiar-contrasena?errorConfirmacion";
        }

        // Se mide la contraseña tal cual se va a guardar. Antes se validaba
        // nueva.trim() pero se guardaba nueva sin recortar, asi que "     x" pasaba
        // el minimo de 6 y quedaba guardada con los espacios adentro.
        if (nueva.length() < LARGO_MINIMO || nueva.isBlank()) {
            return "redirect:/cambiar-contrasena?errorContrasena";
        }

        String email = authentication.getName(); //primero hay que saber quien esta logueado

        Usuario usuario = usuarioRepository.findByEmail(email)            //luego se busca al usuario que esta logueado por su email en la base de datos
            .orElse(null);

        // La sesion sigue viva pero el usuario ya no existe en la base (lo borraron
        // mientras estaba adentro). No es un error del servidor: lo mandamos a
        // loguearse de nuevo, que es lo unico que puede hacer.
        if (usuario == null) {
            return "redirect:/logout";
        }

        if (!passwordEncoder.matches(actual, usuario.getContrasena())) {    //se verifica que la contrasena actual que coloco sea la correcta (devuelve true o false)
            return "redirect:/cambiar-contrasena?error";                    //sino te manda a ?error
        }

        if (passwordEncoder.matches(nueva, usuario.getContrasena())) {     //se verifica que la nueva contrasena no sea la misma que ya tenia
            return "redirect:/cambiar-contrasena?errorMismaContrasena";
        }

        boolean veniaForzado = Boolean.TRUE.equals(usuario.getContrasenaPorDefecto());

        usuario.setContrasena(passwordEncoder.encode(nueva));
        usuario.setContrasenaPorDefecto(false); // ya cambio la contraseña por una propia, no mostramos mas el aviso
        usuarioRepository.save(usuario);

        cerrarLasOtrasSesiones(authentication, request);

        // Si el cambio era obligatorio (venía de la contraseña por defecto), lo
        // llevamos directo a su inicio en vez de dejarlo en esta pantalla.
        if (veniaForzado) {
            return "redirect:/home";
        }
        return "redirect:/cambiar-contrasena?exito";
    }

    // Cambiar la contraseña tiene que echar a quien haya entrado con la anterior.
    // La sesion desde la que se hizo el cambio se conserva -- si no, el usuario
    // terminaria deslogueandose a si mismo cada vez que cambia su contraseña.
    //
    // Las cookies de "Recordarme" no hace falta tocarlas: el token que arma
    // Spring Security incluye un hash de la contraseña, asi que los emitidos con
    // la anterior dejan de validar solos.
    private void cerrarLasOtrasSesiones(Authentication authentication, HttpServletRequest request) {
        HttpSession sesionEnCurso = request.getSession(false);
        String idSesionEnCurso = sesionEnCurso == null ? null : sesionEnCurso.getId();

        List<SessionInformation> sesiones =
                sessionRegistry.getAllSessions(authentication.getPrincipal(), false);

        for (SessionInformation sesion : sesiones) {
            if (!sesion.getSessionId().equals(idSesionEnCurso)) {
                sesion.expireNow();
            }
        }
    }
}
