package com.EscuelaEmpresa.gestor_pasantes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Habilita @Async (usado en EmailService.enviarCorreoRecuperacion): sin esto la
// anotacion no hace nada y Spring ejecuta el metodo igual, de forma sincronica,
// en el mismo hilo del pedido HTTP.
@Configuration
@EnableAsync
public class AsyncConfig {
}
