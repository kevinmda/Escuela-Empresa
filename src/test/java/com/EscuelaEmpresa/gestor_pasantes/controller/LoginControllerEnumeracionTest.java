package com.EscuelaEmpresa.gestor_pasantes.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.EscuelaEmpresa.gestor_pasantes.service.LimitadorPeticionesPorIpService;

// Bloquea la garantia central de /login-check: la respuesta no puede depender
// de si el email existe en la base, porque el controlador ni siquiera consulta
// la base para este paso. Si algun dia alguien le agrega esa consulta (para
// mostrar "este email no esta registrado", por ejemplo), este test se rompe.
class LoginControllerEnumeracionTest {

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

        LoginController controller = new LoginController(new LimitadorPeticionesPorIpService());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(vistas)
                .build();
    }

    @Test
    void testLoginCheck_neverRevealsAccountExistence() throws Exception {
        // "existente" y "noExiste" son solo nombres para el lector del test: el
        // controlador no tiene forma de saber cual es cual (no inyecta ningun
        // repositorio), asi que la unica manera de que este test falle es que
        // alguien le agregue esa distincion mas adelante.
        String existente = "alumno.real@ejemplo.com";
        String noExiste = "nadie-se-registro-con-este@ejemplo.com";

        mockMvc.perform(post("/login-check").param("email", existente))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("email", existente))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeDoesNotExist("bloqueado"))
                .andExpect(model().size(1));

        mockMvc.perform(post("/login-check").param("email", noExiste))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("email", noExiste))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeDoesNotExist("bloqueado"))
                .andExpect(model().size(1));
    }

    @Test
    void limiteDePeticionesCortaElBarridoMasivoDesdeElMismoOrigen() throws Exception {
        // MockMvc usa la misma IP simulada (127.0.0.1) en todos los pedidos de
        // este metodo, asi que las primeras 20 (el tope de
        // LimitadorPeticionesPorIpService) tienen que pasar igual que siempre.
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/login-check").param("email", "quien-sea-" + i + "@ejemplo.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        // El pedido numero 21 desde el mismo origen, en la misma ventana, ya no
        // pasa: se corta antes de llegar a resolverVistaLogin, sin importar que
        // email se haya mandado.
        mockMvc.perform(post("/login-check").param("email", "cualquiera@ejemplo.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("login-email"))
                .andExpect(model().attributeExists("error"));
    }
}
