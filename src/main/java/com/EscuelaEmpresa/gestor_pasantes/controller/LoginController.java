package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.EscuelaEmpresa.gestor_pasantes.service.LimitadorPeticionesPorIpService;

import jakarta.servlet.http.HttpServletRequest;

@Controller //le dice a Spring que esta clase maneja peticiones HTTP y devuelve vistas (HTML) para renderizar
public class LoginController {

    // Este paso no toca la base de datos (por eso mismo es imposible que filtre
    // si una cuenta existe: siempre hace el mismo trabajo, sin importar el
    // email). El limite de aca no es para tapar una fuga, es para que barrer
    // miles de emails por minuto contra este endpoint tenga un costo.
    private static final String CLAVE_LIMITADOR = "login-check";

    private final LimitadorPeticionesPorIpService limitadorPeticiones;

    public LoginController(LimitadorPeticionesPorIpService limitadorPeticiones) {
        this.limitadorPeticiones = limitadorPeticiones;
    }

    // "email" es opcional: sin el, mostramos el primer paso (pedir el email).
    // con el (viene de un redirect de LoginFailureHandler, por ejemplo tras un error),
    // mostramos directo la pantalla que corresponda, con el email ya cargado.
    // "volver" fuerza mostrar el primer paso (email) de nuevo, con el valor precargado
    // -- se usa desde el link "Usar otro email" en login.html/login-primera-vez.html
    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String email,
                                @RequestParam(required = false) Boolean volver,
                                Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // si ya hay alguien logueado (no es anonimo), lo mandamos directo a /home
        // en vez de mostrarle el formulario de login de nuevo
        boolean yaAutenticado = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (yaAutenticado) {
            return "redirect:/home";
        }

        if (Boolean.TRUE.equals(volver)) {
            model.addAttribute("email", email);
            return "login-email"; // primer paso, pero con el email ya cargado en el campo
        }

        if (email == null) {
            return "login-email"; // primer paso: solo pide el email
        }

        return resolverVistaLogin(email, model);
    }

    // segundo paso: segun el email que puso, decide que pantalla de login mostrar
    @PostMapping("/login-check")
    public String verificarEmail(@RequestParam String email, Model model, HttpServletRequest request) {
        if (!limitadorPeticiones.permitir(CLAVE_LIMITADOR + ":" + request.getRemoteAddr())) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Demasiados intentos. Esperá un momento y volvé a intentar.");
            return "login-email";
        }

        return resolverVistaLogin(email, model);
    }

    // Siempre la misma pantalla, exista o no la cuenta y este activa o no. Antes
    // se devolvia "login-primera-vez" solo para cuentas inactivas, lo que permitia
    // enumerar desde afuera cuales cuentas nunca ingresaron (justo las que siguen
    // con la contraseña compartida). La explicacion de "primer ingreso" ahora
    // aparece recien en /verificar-codigo, despues de un intento con la contraseña
    // correcta, cuando ya no revela nada que el atacante no supiera.
    private String resolverVistaLogin(String email, Model model) {
        model.addAttribute("email", email);
        return "login";
    }

    // pantalla publica, no requiere sesion (ver SecurityConfig)
    @GetMapping("/terminos-y-condiciones")
    public String mostrarTerminos() {
        return "terminos-y-condiciones";
    }
}
//HTTP tiene varios verbos (o metodos) que indican la intencion de la peticion:
//GET           Pedir/leer informacion (ej: cargar una pagina)          @GetMapping
//POST          Enviar datos (ej: enviar el formulario del login)       @PostMapping
//PUT           Actualizar un recurso completo                          @PutMapping
//Delete        Eliminar un recurso                                     @DeleteMapping
