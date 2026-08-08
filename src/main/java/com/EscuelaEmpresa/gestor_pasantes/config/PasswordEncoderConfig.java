package com.EscuelaEmpresa.gestor_pasantes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Este bean estaba antes dentro de SecurityConfig, pero eso generaba una dependencia circular:
// SecurityConfig necesita LoginFailureHandler (para el .failureHandler(...)), y LoginFailureHandler
// necesita PasswordEncoder. Si PasswordEncoder vive DENTRO de SecurityConfig, Spring no puede
// decidir a cual construir primero. Sacandolo a su propia clase, ambos pueden pedirlo sin ciclo.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
