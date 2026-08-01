package com.EscuelaEmpresa.gestor_pasantes;

import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// ⚠️ RECORDATORIO: esto es TEMPORAL, solo para pruebas.
// Una vez que confirmes que el admin se creó bien, borrá este archivo o comentá el @Component
// para que no se vuelva a ejecutar en cada arranque de la app.
@Component
public class TemporalCrearAdmin implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    public TemporalCrearAdmin(UsuarioRepository usuarioRepository,
                               AdministradorRepository administradorRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // evita crear el admin de nuevo si ya existe (por si corrés la app varias veces)
        if (usuarioRepository.findByEmail("admin@gmail.com").isPresent()) {
            System.out.println("El usuario admin de prueba ya existe, no se crea de nuevo.");
            return;
        }

        // 1. Crear el Usuario (con activo = false, para probar el flujo de activacion por correo)
        Usuario usuario = new Usuario();
        usuario.setCi("1234567");
        usuario.setEmail("admin@gmail.com"); // <-- cambiá esto por un correo real al que tengas acceso
        usuario.setContrasena(passwordEncoder.encode("admin123")); // contraseña en texto plano -> BCrypt
        usuario.setActivo(false); // arranca inactivo a proposito, para que el scheduler le mande el correo

        usuario = usuarioRepository.save(usuario);

        // 2. Crear el Administrador asociado a ese Usuario
        Administrador admin = new Administrador();
        admin.setNombres("Admin");
        admin.setApellidos("Prueba");
        admin.setCi("1234567");
        admin.setTelefono("0981000000");
        admin.setEmail("admin@gmail.com");
        admin.setCargo("administrativo"); // o "coordinador", segun lo que quieras probar
        admin.setUsuario(usuario);

        administradorRepository.save(admin);

        System.out.println("Usuario admin de prueba creado con exito. Revisa tu correo en un minuto.");
    }
}
