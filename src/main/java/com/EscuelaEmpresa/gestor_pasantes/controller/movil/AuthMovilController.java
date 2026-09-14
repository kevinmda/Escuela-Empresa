package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.EscuelaEmpresa.gestor_pasantes.config.JwtService;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.CambiarContrasenaRequest;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.LoginRequest;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.LoginResponse;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.PerfilAlumnoDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

// Login exclusivo de la app Android. La verificacion por codigo de email de la
// primera vez es un flujo de varias pantallas atado a sesion de servidor -- fuera de
// alcance para movil: la cuenta tiene que estar activada desde la web de antemano.
@RestController
@RequestMapping("/api/movil/auth")
public class AuthMovilController {

    private static final int MAX_INTENTOS_LOGIN = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    // El mismo minimo que pide CambiarContrasenaController en la web.
    private static final int LARGO_MINIMO_CONTRASENA = 6;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthMovilController(AuthenticationManager authenticationManager, JwtService jwtService,
                                UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            // Igual que LoginFailureHandler: loadUserByUsername() de CustomUserDetailsService
            // lanza DisabledException/LockedException, pero AbstractUserDetailsAuthenticationProvider
            // las envuelve en InternalAuthenticationServiceException, con la excepcion real en getCause()
            Throwable causaReal = e.getCause() != null ? e.getCause() : e;

            if (causaReal instanceof LockedException) {
                throw new LockedException("Tu cuenta está bloqueada temporalmente por intentos fallidos. Probá de nuevo más tarde.");
            }
            if (causaReal instanceof DisabledException) {
                throw new DisabledException("Tu cuenta todavía no está activada. Activala desde la web antes de usar la app.");
            }

            registrarIntentoFallido(request.getEmail());
            throw new BadCredentialsException("Email o contraseña incorrectos.");
        }

        registrarIntentoExitoso(request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta cuenta no corresponde a un alumno"));

        String token = jwtService.generarToken(usuario.getEmail());
        return new LoginResponse(token, new PerfilAlumnoDTO(alumno));
    }

    private void registrarIntentoFallido(String email) {
        if (email == null) return;
        usuarioRepository.incrementarIntentosLogin(email);
        usuarioRepository.bloquearSiSuperaIntentos(
                email, LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO), MAX_INTENTOS_LOGIN);
    }

    private void registrarIntentoExitoso(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setIntentosLogin(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        });
    }

    // Mismas reglas que /cambiar-contrasena en la web (largo minimo, contrasena
    // actual correcta, nueva distinta de la actual), pero sin las sesiones
    // pararelas: la API movil es stateless, no hay nada que cerrar aca.
    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<Void> cambiarContrasena(@RequestBody CambiarContrasenaRequest request,
                                                   Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String nueva = request.getNueva();
        if (nueva == null || nueva.length() < LARGO_MINIMO_CONTRASENA) {
            throw new ReglaNegocioException(
                    "La contraseña nueva tiene que tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
        if (!passwordEncoder.matches(request.getActual(), usuario.getContrasena())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(nueva, usuario.getContrasena())) {
            throw new ReglaNegocioException("La contraseña nueva tiene que ser distinta de la actual.");
        }

        usuario.setContrasena(passwordEncoder.encode(nueva));
        usuario.setContrasenaPorDefecto(false);
        usuarioRepository.save(usuario);

        return ResponseEntity.noContent().build();
    }
}
