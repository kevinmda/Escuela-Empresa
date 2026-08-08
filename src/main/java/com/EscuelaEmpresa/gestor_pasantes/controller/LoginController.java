package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller //le dice a Spring que esta clase maneja peticiones HTTP y devuelve vistas (HTML) para renderizar
public class LoginController {

    @GetMapping("/login") //esta anotacion va sobre el metodo y no sobre la clase. Lo que dice es que este metodo responde a peticiones HTTP tipo Get especificamente cuando la URL solicitada sea "/login"
    public String mostrarLogin() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // si ya hay alguien logueado (no es anonimo), lo mandamos directo a /home
        // en vez de mostrarle el formulario de login de nuevo
        boolean yaAutenticado = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (yaAutenticado) {
            return "redirect:/home";
        }
        
        return "login"; //este String "login" es realmente el nombre logico de una vista. Gracias a la dependencia de Thymeleaf, Spring Boot interpreta como: "busca un archivo login.html dentro de src/main/resources/templates/"
    }
} 
//HTTP tiene varios verbos (o metodos) que indican la intencion de la peticion:
//GET           Pedir/leer informacion (ej: cargar una pagina)          @GetMapping
//POST          Enviar datos (ej: enviar el formulario del login)       @PostMapping
//PUT           Actualizar un recurso completo                          @PutMapping
//Delete        Eliminar un recurso                                     @DeleteMapping