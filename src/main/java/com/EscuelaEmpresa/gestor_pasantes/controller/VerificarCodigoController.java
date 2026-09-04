package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.config.LoginFailureHandler;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class VerificarCodigoController {

    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final String ATRIBUTO_SESION_EMAIL = LoginFailureHandler.ATRIBUTO_SESION_EMAIL_ACTIVACION;

    private final UsuarioRepository usuarioRepository;

    public VerificarCodigoController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/verificar-codigo")
    public String mostrarFormulario(HttpSession session, Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean yaAutenticado = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (yaAutenticado) {
            return "redirect:/home";
        }

        String email = (String) session.getAttribute(ATRIBUTO_SESION_EMAIL);

        // sin un pedido de activacion pendiente en esta sesion, no hay nada que verificar
        if (email == null) {
            return "redirect:/login";
        }

        model.addAttribute("email", email);
        return "verificar-codigo";
    }

    @PostMapping("/verificar-codigo")
    public String verificarCodigo(@RequestParam String codigo,
                                   HttpSession session,
                                   Model model) {

        String email = (String) session.getAttribute(ATRIBUTO_SESION_EMAIL);

        if (email == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "No se encontró el usuario.");
            return "verificar-codigo";
        }

        Usuario usuario = usuarioOpt.get();

        int intentos = usuario.getIntentosCodigo() == null ? 0 : usuario.getIntentosCodigo();
        if (intentos >= MAX_INTENTOS_CODIGO) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Superaste el máximo de intentos. Iniciá sesión de nuevo para recibir un código nuevo.");
            return "verificar-codigo";
        }

        boolean codigoCorrecto = codigo != null && codigo.equals(usuario.getTokenActivacion());
        boolean noVencido = usuario.getTokenExpiracion() != null
                && usuario.getTokenExpiracion().isAfter(LocalDateTime.now());

        if (!codigoCorrecto || !noVencido) {
            usuario.setIntentosCodigo(intentos + 1);
            usuarioRepository.save(usuario);

            model.addAttribute("email", email);
            model.addAttribute("error", "El código es incorrecto o venció. Intentos restantes: " + (MAX_INTENTOS_CODIGO - (intentos + 1)));
            return "verificar-codigo";
        }

        // codigo correcto: activamos la cuenta y "quemamos" el codigo para que no se reuse
        usuario.setActivo(true);
        usuario.setTokenActivacion(null);
        usuario.setTokenExpiracion(null);
        usuario.setIntentosCodigo(0);
        usuarioRepository.save(usuario);

        session.removeAttribute(ATRIBUTO_SESION_EMAIL); // ya cumplio su proposito

        return "redirect:/login?activado";
    }
}
