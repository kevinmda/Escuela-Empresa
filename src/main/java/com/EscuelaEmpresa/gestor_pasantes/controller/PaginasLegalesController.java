package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La política de privacidad y las condiciones de uso.
 *
 * Son públicas a propósito: hay que poder leerlas ANTES de entrar, y un padre o
 * una madre que quiere saber qué guarda el sistema sobre su hijo no tiene cuenta
 * con la que iniciar sesión. Por eso las dos rutas van en la lista de permitAll
 * de SecurityConfig y en las que deja pasar ContrasenaPorDefectoInterceptor: si
 * no estuvieran en la segunda, un alumno con la contraseña inicial las pediría y
 * lo rebotaría a cambiar la contraseña sin haberlas podido leer.
 */
@Controller
public class PaginasLegalesController {

    @GetMapping("/privacidad")
    public String privacidad() {
        return "legal/privacidad";
    }

    @GetMapping("/terminos")
    public String terminos() {
        return "legal/terminos";
    }
}
