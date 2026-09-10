package com.EscuelaEmpresa.gestor_pasantes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration //con esta anotacion dice que esta clase se encarga de definir beans (objetos que Spring crea y gestiona por su cuenta) Sin esto Spring no sabria que hay metodos con anotacion Bean (@Bean)
@EnableWebSecurity //esto activa el modulo de seguridad de Spring (Spring Security) pero aclarandole que vas a personalizar la seguridad por tu cuenta
public class SecurityConfig {

    private final LoginFailureHandler loginFailureHandler; // detecta cuenta inactiva/bloqueada y actua en consecuencia
    private final LoginSuccessHandler loginSuccessHandler; // resetea contadores de intentos al loguear con exito
    private final String rememberMeKey;
    private final boolean cookiesSeguras;

    public SecurityConfig(LoginFailureHandler loginFailureHandler,
                          LoginSuccessHandler loginSuccessHandler,
                          @Value("${REMEMBER_ME_KEY:}") String rememberMeKey,
                          @Value("${server.servlet.session.cookie.secure:false}") boolean cookiesSeguras) {
        this.loginFailureHandler = loginFailureHandler;
        this.loginSuccessHandler = loginSuccessHandler;
        this.rememberMeKey = rememberMeKey.isBlank()
                ? java.util.UUID.randomUUID().toString()
                : rememberMeKey;
        // La misma decision que toma la cookie de sesion en application.properties:
        // asi las dos cookies no pueden quedar con criterios distintos.
        this.cookiesSeguras = cookiesSeguras;
    }

    // el PasswordEncoder ahora vive en PasswordEncoderConfig.java, para evitar dependencia circular
    // con LoginFailureHandler (que tambien lo necesita)

    // Lleva la cuenta de que sesiones tiene abiertas cada usuario. Lo necesita
    // /cambiar-contrasena para cerrar las demas cuando alguien cambia su contraseña:
    // sin este registro no hay forma de alcanzar a las sesiones abiertas en otros
    // navegadores, y la contraseña vieja seguiria sirviendo hasta que vencieran solas.
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // El registro de arriba se entera de que una sesion se destruyo solo si alguien
    // publica el evento del contenedor. Sin esto, las sesiones cerradas quedarian
    // figurando como abiertas para siempre.
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception { //esto define cuales son las cadenas de filtros de seguridad por las cuales pasa cada peticion HTTP antes de llegar al Controlador. http es un objeto de la clase "HttpSecurity" que te proporciona una API fluida (especificamente Build Pattern) con la cual vas a ir configurando las reglas
        http                                                                             //estos son una lista de reglas evaluadas en orden de arriba hacia abajo y se aplica la primera que coincida con la URL pedida. El orden y la complitud son muy importantes
            .authorizeHttpRequests(auth -> auth                                          //con .authorizeHttpRequests() lo que se hace es definir que rutas puede ver quien. auth es un objeto configurador de tipo "AuthorizeHttpRequestsConfigurer" que te da Spring Security para configurar el formulario, lo que esta despues de la flecha es lo que haces con ese form
                .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/login-check", "/error", "/verificar-codigo", "/olvide-contrasena", "/restablecer-contrasena").permitAll()    //esta regla dice que cualquiera puede entrar (logueado o no) si son las rutas /css/, /js/, /img/, /login, /login-check, /error, /verificar-codigo, /olvide-contrasena o /restablecer-contrasena
                // Supervisores y Empresas son pantallas de Coordinacion: un administrativo
                // que las abra recibe un 403 aca, sin llegar al controlador. Van ANTES que
                // la regla general de /admin/** porque se aplica la primera que coincide.
                .requestMatchers("/admin/supervisores", "/admin/supervisores/**",
                                 "/admin/empresas", "/admin/empresas/**").hasRole("COORDINADOR")
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
            // Esta cookie dura 14 dias y sirve para entrar sin contraseña, asi que
            // es la que MENOS puede viajar en claro. Acompaña a la cookie de sesion:
            // secure en produccion, no en desarrollo, donde sobre http el navegador
            // directamente no la mandaria.
            .useSecureCookie(cookiesSeguras)
            )
            .sessionManagement(session -> session
                .maximumSessions(-1)                    // -1: no limitamos cuantas sesiones puede tener un usuario,
                .sessionRegistry(sessionRegistry)       // solo queremos que queden anotadas para poder cerrarlas
            )
            .logout(logout -> logout.permitAll()); //esto activa la funcion de logout que viene de Spring Security en la URL "/logout". Permitiendo que cualquiera pueda acceder a esa URL para cerrar sesion

        return http.build(); //.build() aplica todas las configuraciones
    }
} //la forma en la que estan hace que se vea raro pero realmente solo son 3 metodos de http que tienen otros metodos dentro: http.authorizeHttpRequests().formLogin().logout();