package com.EscuelaEmpresa.gestor_pasantes.config;

import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UsuarioRepository usuarioRepository;

    public WebConfig(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ContrasenaPorDefectoInterceptor(usuarioRepository))
                // los recursos estáticos no pasan por el chequeo: si no, un usuario
                // con contraseña por defecto tampoco cargaría el CSS de la propia
                // pantalla de cambio. La API de la app Android tampoco: es stateless
                // (SecurityConfigMovil), así que este interceptor le abriría una
                // HttpSession nueva en cada request y, peor, le devolvería un
                // sendRedirect HTML a una cuenta con contraseña por defecto en vez
                // del JSON que espera el cliente -- la app quedaría rota sin
                // ningún mensaje de error entendible.
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/pdf/**", "/error", "/api/movil/**");
    }
}
