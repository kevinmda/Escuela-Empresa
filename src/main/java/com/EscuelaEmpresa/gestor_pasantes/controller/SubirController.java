package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SubirController {

    @GetMapping("/alumno/subir")
    public String mostrarSubir() {
        return "alumno/subir";
    }
} 
