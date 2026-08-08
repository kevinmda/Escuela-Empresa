package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller //le dice a Spring que esta clase maneja peticiones HTTP y devuelve vistas (HTML) para renderizar
public class LoginController {

    private final UsuarioRepository usuarioRepository;

    public LoginController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // "email" es opcional: sin el, mostramos el primer paso (pedir el email).
    // con el (viene de un redirect de LoginFailureHandler, por ejemplo tras un error),
    // mostramos directo la pantalla que corresponda, con el email ya cargado
    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String email, Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // si ya hay alguien logueado (no es anonimo), lo mandamos directo a /home
        // en vez de mostrarle el formulario de login de nuevo
        boolean yaAutenticado = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (yaAutenticado) {
            return "redirect:/home";
        }

        if (email == null) {
            return "login-email"; // primer paso: solo pide el email
        }

        return resolverVistaLogin(email, model);
    }

    // segundo paso: segun el email que puso, decide que pantalla de login mostrar
    @PostMapping("/login-check")
    public String verificarEmail(@RequestParam String email, Model model) {
        return resolverVistaLogin(email, model);
    }

    // cuenta activa (o no existe) -> login normal (con Recordarme y olvide-contrasena)
    // cuenta inactiva -> login-primera-vez (sin esas opciones, con explicacion)
    // no distinguimos "no existe" de "esta activo" para no revelar si un email esta registrado
    private String resolverVistaLogin(String email, Model model) {
        model.addAttribute("email", email);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty() || Boolean.TRUE.equals(usuarioOpt.get().getActivo())) {
            return "login";
        }

        return "login-primera-vez";
    }
}
//HTTP tiene varios verbos (o metodos) que indican la intencion de la peticion:
//GET           Pedir/leer informacion (ej: cargar una pagina)          @GetMapping
//POST          Enviar datos (ej: enviar el formulario del login)       @PostMapping
//PUT           Actualizar un recurso completo                          @PutMapping
//Delete        Eliminar un recurso                                     @DeleteMapping
