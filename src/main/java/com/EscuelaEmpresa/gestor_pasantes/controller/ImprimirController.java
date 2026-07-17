package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImprimirController {

    @GetMapping("/imprimir")
    public String imprimir() {
        return "alumno/imprimir";
    }
    
}
