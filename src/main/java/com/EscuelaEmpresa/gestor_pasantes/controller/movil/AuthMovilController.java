package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.OlvideContrasenaRequest;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.PerfilAlumnoDTO;
import com.EscuelaEmpresa.gestor_pasantes.dto.movil.RestablecerContrasenaRequest;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.EmailService;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitadorEnvioCodigosService;

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

    // Mismos valores que OlvideContrasenaController/VerificarCodigoController en
    // la web: los dos flujos comparten las columnas token_activacion/token_expiracion/
    // intentos_codigo de Usuario, asi que tienen que compartir tambien estas reglas.
    private static final int MAX_INTENTOS_CODIGO = 5;
    private static final int MINUTOS_VIGENCIA_CODIGO = 5;
    private static final int COOLDOWN_REENVIO_SEGUNDOS = 60;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LimitadorEnvioCodigosService limitadorEnvioCodigos;

    public AuthMovilController(AuthenticationManager authenticationManager, JwtService jwtService,
                                UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                                PasswordEncoder passwordEncoder, EmailService emailService,
                                LimitadorEnvioCodigosService limitadorEnvioCodigos) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.limitadorEnvioCodigos = limitadorEnvioCodigos;
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

    // Mismo flujo que /olvide-contrasena en la web, pero sin sesion de servidor: la
    // app manda el email de vuelta en cada paso (no hay nada raro en que la app se
    // lo pida dos veces al propio usuario, a diferencia del campo oculto del
    // formulario web que preocupaba a OlvideContrasenaController). Siempre responde
    // 204 exista o no la cuenta, para no revelar que emails estan registrados.
    @PostMapping("/olvide-contrasena")
    public ResponseEntity<Void> olvideContrasena(@RequestBody OlvideContrasenaRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

        if (usuarioOpt.isPresent() && limitadorEnvioCodigos.registrarEnvioSiHayCupo(request.getEmail())) {
            Usuario usuario = usuarioOpt.get();

            LocalDateTime ultimoEnvio = usuario.getTokenExpiracion() == null
                    ? null
                    : usuario.getTokenExpiracion().minusMinutes(MINUTOS_VIGENCIA_CODIGO);

            boolean todaviaEnCooldown = ultimoEnvio != null
                    && ultimoEnvio.plusSeconds(COOLDOWN_REENVIO_SEGUNDOS).isAfter(LocalDateTime.now());

            if (!todaviaEnCooldown) {
                String codigo = codigoTodaviaVigente(usuario);
                if (codigo == null) {
                    codigo = generarCodigoNumerico();
                    usuario.setTokenActivacion(codigo);
                    usuario.setTokenExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA_CODIGO));
                    usuario.setIntentosCodigo(0);
                    usuarioRepository.save(usuario);
                }

                try {
                    emailService.enviarCorreoRecuperacion(usuario.getEmail(), codigo);
                } catch (MailException e) {
                    limitadorEnvioCodigos.devolverCupo(request.getEmail());
                }
            }
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Void> restablecerContrasena(@RequestBody RestablecerContrasenaRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new ReglaNegocioException("Código incorrecto o vencido."));

        int intentos = usuario.getIntentosCodigo() == null ? 0 : usuario.getIntentosCodigo();
        if (intentos >= MAX_INTENTOS_CODIGO) {
            throw new ReglaNegocioException(
                    "Superaste el máximo de intentos con este código. Esperá a que venza y pedí uno nuevo.");
        }

        boolean codigoCorrecto = request.getCodigo() != null && request.getCodigo().equals(usuario.getTokenActivacion());
        boolean noVencido = usuario.getTokenExpiracion() != null
                && usuario.getTokenExpiracion().isAfter(LocalDateTime.now());

        if (!codigoCorrecto || !noVencido) {
            usuario.setIntentosCodigo(intentos + 1);
            usuarioRepository.save(usuario);
            throw new ReglaNegocioException(
                    "Código incorrecto o vencido. Intentos restantes: " + (MAX_INTENTOS_CODIGO - (intentos + 1)));
        }

        String nueva = request.getNuevaContrasena();
        if (nueva == null || nueva.length() < LARGO_MINIMO_CONTRASENA) {
            throw new ReglaNegocioException(
                    "La contraseña tiene que tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }

        usuario.setContrasena(passwordEncoder.encode(nueva));
        usuario.setContrasenaPorDefecto(false);
        usuario.setTokenActivacion(null);
        usuario.setTokenExpiracion(null);
        usuario.setIntentosCodigo(0);
        usuario.setIntentosLogin(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        return ResponseEntity.noContent().build();
    }

    // El codigo que el usuario ya tiene, si todavia no vencio. null significa que
    // hay que emitir uno nuevo. Identico a OlvideContrasenaController en la web.
    private String codigoTodaviaVigente(Usuario usuario) {
        boolean vigente = usuario.getTokenActivacion() != null
                && usuario.getTokenExpiracion() != null
                && usuario.getTokenExpiracion().isAfter(LocalDateTime.now());

        return vigente ? usuario.getTokenActivacion() : null;
    }

    private String generarCodigoNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000);
        return String.valueOf(numero);
    }
}
