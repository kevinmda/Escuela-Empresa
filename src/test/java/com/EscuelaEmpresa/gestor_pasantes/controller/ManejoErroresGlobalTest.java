package com.EscuelaEmpresa.gestor_pasantes.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;

// Prueba ManejoErroresGlobal + templates/error.html sin levantar Spring Boot ni la
// base de datos: un controlador de juguete que lanza cada excepcion, MockMvc en modo
// standalone, y Thymeleaf leyendo las plantillas reales del classpath.
class ManejoErroresGlobalTest {

    @Controller
    static class ControladorDePrueba {
        @GetMapping("/prueba/no-encontrado")
        String noEncontrado() { throw new RecursoNoEncontradoException("Planilla no encontrada"); }

        @GetMapping("/prueba/prohibido")
        String prohibido() { throw new AccessDeniedException("No tenés permiso para ver esta planilla"); }

        @GetMapping("/prueba/regla")
        String regla() { throw new ReglaNegocioException("Ya cargaste las 6 semanas de planilla."); }

        @GetMapping("/prueba/explota")
        String explota() { throw new IllegalStateException("null pointer disfrazado"); }

        @GetMapping("/admin/api/prueba")
        String api() { throw new RecursoNoEncontradoException("Alumno no encontrado"); }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void armar() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolver);

        ThymeleafViewResolver vistas = new ThymeleafViewResolver();
        vistas.setTemplateEngine(motor);
        vistas.setCharacterEncoding("UTF-8");

        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejoErroresGlobal())
                .setViewResolvers(vistas)
                .build();
    }

    @Test
    void recursoNoEncontradoDa404ConSuMensaje() throws Exception {
        mockMvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("404")))
                .andExpect(content().string(containsString("Planilla no encontrada")));
    }

    @Test
    void accesoDenegadoDa403() throws Exception {
        mockMvc.perform(get("/prueba/prohibido"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("No tenés permiso para esto")))
                .andExpect(content().string(containsString("No tenés permiso para ver esta planilla")));
    }

    @Test
    void reglaDeNegocioDa400ConElTextoParaElUsuario() throws Exception {
        mockMvc.perform(get("/prueba/regla"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ya cargaste las 6 semanas de planilla.")));
    }

    @Test
    void errorInesperadoDa500SinFiltrarElDetalleTecnico() throws Exception {
        mockMvc.perform(get("/prueba/explota"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Algo salió mal")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("null pointer disfrazado"))));
    }

    @Test
    void lasRutasApiRecibenJson() throws Exception {
        mockMvc.perform(get("/admin/api/prueba"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Alumno no encontrado"));
    }
}
