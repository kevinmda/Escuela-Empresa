package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class VerificarCodigoController {

    private final UsuarioRepository usuarioRepository;

    public VerificarCodigoController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // se muestra automaticamente despues de un login fallido por cuenta inactiva
    // (el LoginFailureHandler redirige aca)
    @GetMapping("/verificar-codigo")
    public String mostrarFormulario(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verificar-codigo";
    }

    @PostMapping("/verificar-codigo")
    public String verificarCodigo(@RequestParam String email,
                                   @RequestParam String codigo,
                                   Model model) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "No se encontró el usuario.");
            return "verificar-codigo";
        }

        Usuario usuario = usuarioOpt.get();

        boolean codigoCorrecto = codigo != null && codigo.equals(usuario.getTokenActivacion());
        boolean noVencido = usuario.getTokenExpiracion() != null
                && usuario.getTokenExpiracion().isAfter(LocalDateTime.now());

        if (!codigoCorrecto || !noVencido) {
            model.addAttribute("email", email);
            model.addAttribute("error", "El código es incorrecto o venció. Intentá iniciar sesión de nuevo para recibir uno nuevo.");
            return "verificar-codigo";
        }

        // codigo correcto: activamos la cuenta y "quemamos" el codigo para que no se reuse
        usuario.setActivo(true);
        usuario.setTokenActivacion(null);
        usuario.setTokenExpiracion(null);
        usuarioRepository.save(usuario);

        return "redirect:/login?activado";
    }
}
