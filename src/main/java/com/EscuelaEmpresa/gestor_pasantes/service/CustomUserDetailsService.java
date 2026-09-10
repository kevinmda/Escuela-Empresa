package com.EscuelaEmpresa.gestor_pasantes.service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service //esta anotacion le dice a Spring que cree un objeto que sea administrado por el (un bean)
public class CustomUserDetailsService implements UserDetailsService { //UserDetailsService es una interfaz que define Spring Security y que se usa para indicarle que esta clase (CustomUserDetailsService) sea usada para la autentificacion de usuarios. Esta interfaz unicamente necesita el metodo UserDetails

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final AlumnoRepository alumnoRepository;

    CustomUserDetailsService(AdministradorRepository administradorRepository, UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository) {
        this.administradorRepository = administradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException { //el metodo loadUseByUsername dice "Username" porque internamente Spring Security siempre le llama Username, sin importar la informacion que se introduce. El metodo lo que hace es traer lo que el usuario rellena en el navegador que en nuestro caso seria el email
        Usuario usuario = usuarioRepository.findByEmail(email) //si devuelve algo despues de filtrar por email (usuarioRepository.findByEmail(email)) entonces el objeto usuario de la clase Usuario (Usuario usuario), lo agarra
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado")); //sino entonces lanza una excepcion diciendo "Usuario no encontrado"

        if (Boolean.FALSE.equals(usuario.getActivo())) { //esto sirve para verificar si el usuario se encuentra deshabilitado osea si en el campo activo de usuario tiene 0 o false. Se utiliza Boolean.FALSE.equals(usuario.getActivo()) en vez de solo (!usuario.getActivo()) para que en caso de que el valor no sea 0 o 1 sino null, entonces no de un error de NullPointerException
            throw new DisabledException("Usuario deshabilitado");
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(java.time.LocalDateTime.now())) {
            throw new LockedException("Cuenta bloqueada temporalmente por intentos fallidos");
        }
        //GrantedAuthority es para indicarle a Spring Security que se trata de permisos o mas bien roles
        List<GrantedAuthority> authorities = new ArrayList<>(); //se crea una lista vacia que luego se va a ir llenando de acuerdo a los roles que se le da al usuario. En nuestro caso no es tannn importante porque cada usuario va a tener un rol unico, pero en el dia de mañana si un usuario necesita tener mas de un rol entonces se va a poder incluir en la lista nms y ya

        administradorRepository.findByUsuario_IdUsr(usuario.getIdUsr()).ifPresent(administrador -> { //aca se pregunta si existe una fila en adminsitrador vinculada a este usuario
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")); //en caso de que si entonces se le asigna el rol de ADMIN (si o si tiene que ser con el "ROLE_" porque es una convencion de Spring Security)
                                                                       //por lo que entiendo new SimpleGrantedAuthority("ROLE_ADMIN") seria la manera de agregar un rol, al parecer es un objeto de clase "SimpleGrantedAuthority" que tambien viene de Spring Security

            // Coordinacion y Administracion comparten la tabla administrador y hasta
            // ahora compartian tambien el unico rol que habia, ROLE_ADMIN. La
            // diferencia entre los dos se hacia despues, adentro de cada metodo de
            // AdminController, leyendo el cargo a mano. Funcionaba, pero era una regla
            // que no sostenia nada: el primer metodo nuevo que se olvidara de mirar el
            // cargo le abria a un coordinador todas las especialidades.
            //
            // Con el cargo convertido en rol, SecurityConfig puede cerrar las pantallas
            // de Coordinacion antes de que la peticion llegue al controlador. El
            // chequeo por especialidad sigue siendo necesario -- esto es la segunda
            // barrera, no el reemplazo de la primera.
            if ("coordinador".equalsIgnoreCase(administrador.getCargo())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_COORDINADOR"));
            } else if ("administrativo".equalsIgnoreCase(administrador.getCargo())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATIVO"));
            }
        });
        if (alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr()).isPresent()) { //aca se pregunta si existe una fila en alumno vinculada a este usuario
            authorities.add(new SimpleGrantedAuthority("ROLE_ALUMNO")); //en caso de que si entonces se le asigna el rol de ALUMNO
        }
        //finalmente se empaqueta todo en un objeto User que utiliza una clase que viene de Spring Security
        return new org.springframework.security.core.userdetails.User( //el return devuelve lo que necesita el metodo de la interfaz UserDetailsService. Lo que se necesita es:
            usuario.getEmail(),         //necesita el "username" (el email)
            usuario.getContrasena(),    //la contrasena ya hasheada (osea encriptada). Que esto realmente tiene todo su proceso pero lq si es que en la base de datos obviamente por motivos de seguridad las contrasenas tienen que estar encriptadas, y esto se hace por medio de un archivo temporal que se crea en el proyecto, luego por detras de todo, el Spring Security ya se encarga de comparar la contrasena encriptada para verificar al usuario
            authorities                 //y por ultimo necesita la lista de roles del usuario que se arma arriba (en nuestro caso como ya dije por el momento solo va a ser ADMIN o ALUMNO)
        );
    }
}