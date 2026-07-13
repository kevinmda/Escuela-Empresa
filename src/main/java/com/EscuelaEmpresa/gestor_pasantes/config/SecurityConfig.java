package com.EscuelaEmpresa.gestor_pasantes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration //con esta anotacion dice que esta clase se encarga de definir beans (objetos que Spring crea y gestiona por su cuenta) Sin esto Spring no sabria que hay metodos con anotacion Bean (@Bean)
@EnableWebSecurity //esto activa el modulo de seguridad de Spring (Spring Security) pero aclarandole que vas a personalizar la seguridad por tu cuenta
public class SecurityConfig {

    @Bean //con esto le dice que el metodo crea un bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //le dice que hasheo o encriptacion usar (en este caso BCrypt) Al declararlo como @bean, cualquier otra parte de la app (o el mismo Spring internamente) pueden llamarlo. Este objeto que se crea es el que se utiliza para el hasheado
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { //esto define cuales son las cadenas de filtros de seguridad por las cuales pasa cada peticion HTTP antes de llegar al Controlador. http es un objeto de la clase "HttpSecurity" que te proporciona una API fluida (especificamente Build Pattern) con la cual vas a ir configurando las reglas
        http                                                                             //estos son una lista de reglas evaluadas en orden de arriba hacia abajo y se aplica la primera que coincida con la URL pedida. El orden y la complitud son muy importantes
            .authorizeHttpRequests(auth -> auth                                          //con .authorizeHttpRequests() lo que se hace es definir que rutas puede ver quien. auth es un objeto configurador de tipo "AuthorizeHttpRequestsConfigurer" que te da Spring Security para configurar el formulario, lo que esta despues de la flecha es lo que haces con ese form
                .requestMatchers("/css/**", "/js/**", "/login", "/error").permitAll()    //esta regla dice que cualquiera puede entrar (logueado o no) si son las rutas /css/, /js/, /login, o /error
                .requestMatchers("/admin/**").hasRole("ADMIN")                           //esta regla dice que cualquiera con el rol de ADMIN puede entrar si es la ruta /admin/
                .requestMatchers("/alumno/**").hasRole("ALUMNO")                         //esta regla dice que cualquiera con el rol de ALUMNO puede entrar si es la ruta /alumno/
                .anyRequest().authenticated()                                            //por ultimo esto dice que cualquier otra cosa que no matcheo ninguna de las anteriores tiene que estar autentificado (logueado) sin importar el rol
            )
            .formLogin(form -> form                                                      //.formLogin() le dice a Spring Security que se va a usar un formulario html tradicional. El primer form es un Objeto de tipo "FormLoginConfigurer" que te da Spring Security para configurar el formulario, lo que esta despues de la flecha es lo que haces con ese form
                .loginPage("/login")                                                     //esto le dice que siempre que alguien quiere loguearse use esta URL "/login"
                .defaultSuccessUrl("/", true)                                            //esto le dice a donde mandar al usuario luego de que se haya logueado exitosamente. En este caso se le manda a la raiz (src/main/resources/templates/index.html) el parametro true es para indicarle que siempre se le mande ahi, sin importar donde trataba de acceder el usuario antes de mandarle a loguearse
                .permitAll()                                                             //este que se encuentra dentro de ".formLogin()" es el que permite especificamente el procesamiento del login (de la peticion POST/login que llega una vez que se envia el formulario desde el navegador)
                // por defecto: .failureUrl("/login?error")
                // por defecto: .logoutSuccessUrl("/login?logout")
                // por defecto: .usernameParameter("username")
                // por defecto: .passwordParameter("password")
                // esto esta bien tener en cuenta para entender un poco lo de login.html
            )
            .logout(logout -> logout.permitAll()); //esto activa la funcion de logout que viene de Spring Security en la URL "/logout". Permitiendo que cualquiera pueda acceder a esa URL para cerrar sesion

        return http.build(); //.build() aplica todas las configuraciones
    }
} //la forma en la que estan hace que se vea raro pero realmente solo son 3 metodos de http que tienen otros metodos dentro: http.authorizeHttpRequests().formLogin().logout();