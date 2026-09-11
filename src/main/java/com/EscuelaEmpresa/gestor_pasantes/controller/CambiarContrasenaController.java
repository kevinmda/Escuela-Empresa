package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

@Controller
public class CambiarContrasenaController {

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    CambiarContrasenaController(PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository) {
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/cambiar-contrasena")
    public String mostrarFormulario() {
        return "cambiar-contrasena";
    }

    @PostMapping("/cambiar-contrasena")
    public String cambiarContrasena(Authentication authentication, @RequestParam String actual, @RequestParam String nueva) { //el objeto authentication guarda internamente quien esta autentificado (logueado)
        if (nueva == null || nueva.trim().length() < 6) {
            return "redirect:/cambiar-contrasena?errorContrasena";
        }

        String email = authentication.getName(); //primero hay que saber quien esta logueado

        Usuario usuario = usuarioRepository.findByEmail(email)            //luego se busca al usuario que esta logueado por su email en la base de datos
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no Encontrado"));    //sino se encuentra entonces da el mensaje

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

        // Si el cambio era obligatorio (venía de la contraseña por defecto), lo
        // llevamos directo a su inicio en vez de dejarlo en esta pantalla.
        if (veniaForzado) {
            return "redirect:/home";
        }
        return "redirect:/cambiar-contrasena?exito";
    }
}