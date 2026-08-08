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

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public OlvideContrasenaController(UsuarioRepository usuarioRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/olvide-contrasena")
    public String mostrarFormularioEmail(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "olvide-contrasena";
    }

    @PostMapping("/olvide-contrasena")
    public String enviarCodigo(@RequestParam String email, HttpSession session, Model model) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // solo generamos y mandamos el codigo si el usuario existe, pero lo que ve la
        // persona en pantalla es lo mismo en ambos casos, para no revelar si el email esta registrado
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
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

        // guardamos el email en la sesion del servidor: de aca en adelante, la pantalla de
        // restablecer-contrasena va a operar SIEMPRE sobre este email, sin importar nada
        // que llegue en el formulario
        session.setAttribute(ATRIBUTO_SESION_EMAIL, email);

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
