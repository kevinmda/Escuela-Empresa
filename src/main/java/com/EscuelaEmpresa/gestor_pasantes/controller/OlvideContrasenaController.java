package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.EmailService;
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

@Controller
public class OlvideContrasenaController {

    private static final int MAX_INTENTOS_CODIGO = 5;

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
    public String enviarCodigo(@RequestParam String email, Model model) {
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

        return "redirect:/restablecer-contrasena?email=" + email;
    }

    @GetMapping("/restablecer-contrasena")
    public String mostrarFormularioReset(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "restablecer-contrasena";
    }

    @PostMapping("/restablecer-contrasena")
    public String restablecerContrasena(@RequestParam String email,
                                         @RequestParam String codigo,
                                         @RequestParam String nuevaContrasena,
                                         @RequestParam String confirmarContrasena,
                                         Model model) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Código incorrecto o vencido.");
            return "restablecer-contrasena";
        }

        Usuario usuario = usuarioOpt.get();

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

        return "redirect:/login?restablecida";
    }

    private String generarCodigoNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000);
        return String.valueOf(numero);
    }
}
