package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

// IMPORTANTE: el email de la cuenta que se esta recuperando se guarda en la SESION del servidor
// (session.setAttribute), no se manda de vuelta y vuelta como parametro del formulario.
// Esto es a proposito: un campo "hidden" en el HTML sigue siendo editable desde las herramientas
// de desarrollador del navegador. Si confiaramos en ese valor, alguien podria pedir un codigo
// para SU PROPIO email, y despues cambiar el campo oculto para que apunte al email de otra
// persona antes de enviar el formulario. Guardando el email en la sesion del servidor, el navegador
// nunca tiene la posibilidad de decidir "para que cuenta es esto" — esa decision la toma el servidor.
@Controller
public class OlvideContrasenaController {

    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final String ATRIBUTO_SESION_EMAIL = "email_recuperacion";

    // anti-spam: sin esto, un POST repetido a /olvide-contrasena manda un correo
    // nuevo cada vez, sin límite. 60 segundos alcanza para que alguien real no
    // note la espera, pero corta un loop automatizado.
    private static final int COOLDOWN_REENVIO_SEGUNDOS = 60;

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public OlvideContrasenaController(UsuarioRepository usuarioRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/olvide-contrasena")
    public String mostrarFormularioEmail(@RequestParam(required = false) String email, HttpSession session, Model model) {
        // si llega el email por la URL (desde el link de login.html), lo guardamos en la sesion.
        // si no llega (ej: alguien recarga la pagina), usamos el que ya estuviera guardado de antes
        if (email != null) {
            session.setAttribute(ATRIBUTO_SESION_EMAIL, email);
        } else {
            email = (String) session.getAttribute(ATRIBUTO_SESION_EMAIL);
        }

        // sin ningun email conocido (ni por URL ni por sesion), no tiene sentido mostrar esta pantalla
        if (email == null) {
            return "redirect:/login";
        }

        model.addAttribute("email", email);
        return "olvide-contrasena";
    }

    @PostMapping("/olvide-contrasena")
    public String enviarCodigo(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute(ATRIBUTO_SESION_EMAIL);

        if (email == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // tokenExpiracion siempre queda en "ahora + 5 minutos" al generar un
            // código (ver más abajo), así que restando 5 minutos se reconstruye
            // cuándo se generó el último sin necesitar una columna nueva solo
            // para esto.
            LocalDateTime ultimoEnvio = usuario.getTokenExpiracion() == null
                    ? null
                    : usuario.getTokenExpiracion().minusMinutes(5);

            if (ultimoEnvio != null && ultimoEnvio.plusSeconds(COOLDOWN_REENVIO_SEGUNDOS).isAfter(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error",
                        "Ya te mandamos un código. Revisá tu correo (también la carpeta de spam) antes de pedir otro.");
                return "redirect:/restablecer-contrasena";
            }

            String codigo = generarCodigoNumerico();

            usuario.setTokenActivacion(codigo);
            usuario.setTokenExpiracion(LocalDateTime.now().plusMinutes(5));
            usuario.setIntentosCodigo(0);
            usuarioRepository.save(usuario);

            try {
                emailService.enviarCorreoRecuperacion(usuario.getEmail(), codigo);
            } catch (MailException e) {
                model.addAttribute("email", email);
                model.addAttribute("error", "No pudimos enviar el correo. Intentá de nuevo en unos minutos.");
                return "olvide-contrasena";
            }
        }

        return "redirect:/restablecer-contrasena";
    }

    @GetMapping("/restablecer-contrasena")
    public String mostrarFormularioReset(HttpSession session, Model model) {
        String email = (String) session.getAttribute(ATRIBUTO_SESION_EMAIL);

        // si no hay ningun pedido de recuperacion pendiente en esta sesion, no tiene
        // sentido mostrar esta pantalla: lo mandamos a pedir un codigo primero
        if (email == null) {
            return "redirect:/olvide-contrasena";
        }

        model.addAttribute("email", email);
        return "restablecer-contrasena";
    }

    @PostMapping("/restablecer-contrasena")
    public String restablecerContrasena(@RequestParam String codigo,
                                         @RequestParam String nuevaContrasena,
                                         @RequestParam String confirmarContrasena,
                                         HttpSession session,
                                         Model model) {

        String email = (String) session.getAttribute(ATRIBUTO_SESION_EMAIL);

        if (email == null) {
            return "redirect:/olvide-contrasena";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Código incorrecto o vencido.");
            return "restablecer-contrasena";
        }

        Usuario usuario = usuarioOpt.get();

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Código incorrecto o vencido.");
            return "restablecer-contrasena";
        }

        int intentos = usuario.getIntentosCodigo() == null ? 0 : usuario.getIntentosCodigo();
        if (intentos >= MAX_INTENTOS_CODIGO) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Superaste el máximo de intentos. Pedí un código nuevo.");
            return "restablecer-contrasena";
        }

        boolean codigoCorrecto = codigo != null && codigo.equals(usuario.getTokenActivacion());
        boolean noVencido = usuario.getTokenExpiracion() != null
                && usuario.getTokenExpiracion().isAfter(LocalDateTime.now());

        if (!codigoCorrecto || !noVencido) {
            usuario.setIntentosCodigo(intentos + 1);
            usuarioRepository.save(usuario);

            model.addAttribute("email", email);
            model.addAttribute("error", "Código incorrecto o vencido. Intentos restantes: " + (MAX_INTENTOS_CODIGO - (intentos + 1)));
            return "restablecer-contrasena";
        }

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "restablecer-contrasena";
        }

        if (nuevaContrasena.length() < 6) {
            model.addAttribute("email", email);
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "restablecer-contrasena";
        }

        // todo correcto: guardamos la nueva contraseña y limpiamos todo rastro del proceso
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuario.setTokenActivacion(null);
        usuario.setTokenExpiracion(null);
        usuario.setIntentosCodigo(0);
        usuario.setIntentosLogin(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        session.removeAttribute(ATRIBUTO_SESION_EMAIL); // ya cumplio su proposito, la borramos

        return "redirect:/login?restablecida";
    }

    private String generarCodigoNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000);
        return String.valueOf(numero);
    }
}
