package com.EscuelaEmpresa.gestor_pasantes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration //con esta anotacion dice que esta clase se encarga de definir beans (objetos que Spring crea y gestiona por su cuenta) Sin esto Spring no sabria que hay metodos con anotacion Bean (@Bean)
@EnableWebSecurity //esto activa el modulo de seguridad de Spring (Spring Security) pero aclarandole que vas a personalizar la seguridad por tu cuenta
public class SecurityConfig {

    private final LoginFailureHandler loginFailureHandler; // detecta cuenta inactiva/bloqueada y actua en consecuencia
    private final LoginSuccessHandler loginSuccessHandler; // resetea contadores de intentos al loguear con exito
    private final String rememberMeKey;

    public SecurityConfig(LoginFailureHandler loginFailureHandler,
                          LoginSuccessHandler loginSuccessHandler,
                          @Value("${REMEMBER_ME_KEY:}") String rememberMeKey) {
        this.loginFailureHandler = loginFailureHandler;
        this.loginSuccessHandler = loginSuccessHandler;
        this.rememberMeKey = rememberMeKey.isBlank()
                ? java.util.UUID.randomUUID().toString()
                : rememberMeKey;
    }

    // el PasswordEncoder ahora vive en PasswordEncoderConfig.java, para evitar dependencia circular
    // con LoginFailureHandler (que tambien lo necesita)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { //esto define cuales son las cadenas de filtros de seguridad por las cuales pasa cada peticion HTTP antes de llegar al Controlador. http es un objeto de la clase "HttpSecurity" que te proporciona una API fluida (especificamente Build Pattern) con la cual vas a ir configurando las reglas
        http                                                                             //estos son una lista de reglas evaluadas en orden de arriba hacia abajo y se aplica la primera que coincida con la URL pedida. El orden y la complitud son muy importantes
            .authorizeHttpRequests(auth -> auth                                          //con .authorizeHttpRequests() lo que se hace es definir que rutas puede ver quien. auth es un objeto configurador de tipo "AuthorizeHttpRequestsConfigurer" que te da Spring Security para configurar el formulario, lo que esta despues de la flecha es lo que haces con ese form
                .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/login-check", "/error", "/verificar-codigo", "/olvide-contrasena", "/restablecer-contrasena", "/terminos-y-condiciones").permitAll()    //esta regla dice que cualquiera puede entrar (logueado o no) si son las rutas /css/, /js/, /img/, /login, /login-check, /error, /verificar-codigo, /olvide-contrasena o /restablecer-contrasena
                .requestMatchers("/admin/**").hasRole("ADMIN")                           //esta regla dice que cualquiera con el rol de ADMIN puede entrar si es la ruta /admin/
                .requestMatchers("/alumno/**").hasRole("ALUMNO")                         //esta regla dice que cualquiera con el rol de ALUMNO puede entrar si es la ruta /alumno/
                .anyRequest().authenticated()                                            //por ultimo esto dice que cualquier otra cosa que no matcheo ninguna de las anteriores tiene que estar autentificado (logueado) sin importar el rol
            )
            .formLogin(form -> form                                                      //.formLogin() le dice a Spring Security que se va a usar un formulario html tradicional. El primer form es un Objeto de tipo "FormLoginConfigurer" que te da Spring Security para configurar el formulario, lo que esta despues de la flecha es lo que haces con ese form
                .loginPage("/login")                                                     //esto le dice que siempre que alguien quiere loguearse use esta URL "/login"
                .successHandler(loginSuccessHandler)                                     //reemplaza defaultSuccessUrl: redirige a /home igual que antes, pero ademas resetea los contadores de intentos fallidos
                .failureHandler(loginFailureHandler)                                     //reemplaza el comportamiento por defecto: cuenta bloqueada, cuenta inactiva (dispara el codigo), o contraseña incorrecta (cuenta el intento)
                .permitAll()                                                             //este que se encuentra dentro de ".formLogin()" es el que permite especificamente el procesamiento del login (de la peticion POST/login que llega una vez que se envia el formulario desde el navegador)
                // por defecto: .logoutSuccessUrl("/login?logout")
                // por defecto: .usernameParameter("username")
                // por defecto: .passwordParameter("password")
                // esto esta bien tener en cuenta para entender un poco lo de login.html
            )
            .rememberMe(remember -> remember
            .key(rememberMeKey)
            .tokenValiditySeconds(1209600) // 14 dias en segundos
            )
            .logout(logout -> logout.permitAll()); //esto activa la funcion de logout que viene de Spring Security en la URL "/logout". Permitiendo que cualquiera pueda acceder a esa URL para cerrar sesion

        return http.build(); //.build() aplica todas las configuraciones
    }
} //la forma en la que estan hace que se vea raro pero realmente solo son 3 metodos de http que tienen otros metodos dentro: http.authorizeHttpRequests().formLogin().logout();