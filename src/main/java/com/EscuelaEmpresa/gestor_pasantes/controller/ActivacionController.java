package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class ActivacionController {

    private final UsuarioRepository usuarioRepository;

    public ActivacionController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/activar-cuenta")
    public String activarCuenta(@RequestParam String token, Model model) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenActivacion(token);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("mensaje", "El enlace de activación no es válido.");
            return "activacion-resultado";
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getTokenExpiracion() == null || usuario.getTokenExpiracion().isBefore(LocalDateTime.now())) {
            model.addAttribute("mensaje", "El enlace de activación venció. Contactá a la coordinación para que te reenvíen uno nuevo.");
            return "activacion-resultado";
        }

        // todo en orden: activamos la cuenta y "quemamos" el token para que no se pueda volver a usar
        usuario.setActivo(true);
        usuario.setTokenActivacion(null);
        usuario.setTokenExpiracion(null);
        usuarioRepository.save(usuario);

        model.addAttribute("mensaje", "¡Tu cuenta fue activada correctamente! Ya podés iniciar sesión.");
        return "activacion-resultado";
    }
}
