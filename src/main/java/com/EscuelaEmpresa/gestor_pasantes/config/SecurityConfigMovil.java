package com.EscuelaEmpresa.gestor_pasantes.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

// Cadena de seguridad exclusiva para la API que consume la app Android
// (/api/movil/**): sin sesion (stateless), sin CSRF (no hay formulario ni cookie de
// sesion que proteger), autenticacion por JWT en vez del form login de SecurityConfig.
//
// Va con @Order(1) porque .securityMatcher("/api/movil/**") la limita a esas rutas;
// SecurityConfig (@Order(2)) sigue cubriendo todo lo demas exactamente igual que antes.
@Configuration
public class SecurityConfigMovil {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfigMovil(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Lo pide AuthMovilController para validar email+password a mano (no hay
    // formLogin en esta cadena). Reutiliza el PasswordEncoder y el
    // CustomUserDetailsService ya cableados, con los mismos chequeos de
    // activo/bloqueado que el login web.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiMovilFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/movil/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/movil/auth/login").permitAll()
                .anyRequest().hasRole("ALUMNO")
            )
            // Estas dos excepciones ocurren en el filtro, ANTES de llegar a cualquier
            // controlador: ningun @RestControllerAdvice las puede interceptar, asi
            // que el JSON se escribe directo aca para que la app siempre reciba el
            // mismo shape {status, titulo, mensaje} sin importar donde haya fallado.
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) ->
                        escribirJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                "No autenticado", "Iniciá sesión nuevamente."))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        escribirJson(response, HttpServletResponse.SC_FORBIDDEN,
                                "No tenés permiso", "Tu cuenta no puede acceder a esto."))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Estos dos mensajes son fijos (no incluyen datos que vengan del usuario ni de la
    // base), asi que arma el JSON a mano en vez de traer una dependencia de Jackson
    // solo para esto -- el mismo shape {status, titulo, mensaje} que usa
    // ManejoErroresMovil para todo lo demas.
    private void escribirJson(HttpServletResponse response, int status, String titulo, String mensaje) throws IOException {
        response.setStatus(status);
        // sin charset explicito, la respuesta se escribe en ISO-8859-1 por defecto y
        // las tildes de "Iniciá sesión..." llegan corruptas
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":" + status + ",\"titulo\":\"" + titulo + "\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
