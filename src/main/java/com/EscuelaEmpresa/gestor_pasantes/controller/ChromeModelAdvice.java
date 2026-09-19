package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import com.EscuelaEmpresa.gestor_pasantes.dto.ChromeContext;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.AvisoLeido;
import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AvisoLeidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Contrato único para el cromo autenticado. Así las once vistas no tienen que
 * reconstruir por su cuenta el rol, la identidad y la sección activa.
 */
@ControllerAdvice(annotations = Controller.class)
public class ChromeModelAdvice {

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final AdministradorRepository administradorRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final AvisoLeidoRepository avisoLeidoRepository;

    public ChromeModelAdvice(UsuarioRepository usuarioRepository,
                             AlumnoRepository alumnoRepository,
                             AdministradorRepository administradorRepository,
                             PlanillaSemanalRepository planillaSemanalRepository,
                             AvisoLeidoRepository avisoLeidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.administradorRepository = administradorRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.avisoLeidoRepository = avisoLeidoRepository;
    }

    @ModelAttribute("chrome")
    public ChromeContext cargarChrome() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return null;
        }

        Administrador administrador = administradorRepository.findByUsuario_IdUsr(usuario.getIdUsr()).orElse(null);
        if (administrador != null) {
            String rol = "coordinador".equalsIgnoreCase(administrador.getCargo())
                    ? "coordinador"
                    : "administrador";
            // Administración y coordinación trabajan sobre conjuntos de especialidades;
            // el cromo queda deliberadamente neutro en vez de fingir una sola identidad.
            // Sin avisos: son solo del alumno (constructor de 5 args, sin lista).
            return new ChromeContext(
                    rol,
                    nombreCompleto(administrador.getNombres(), administrador.getApellidos(), usuario.getEmail()),
                    usuario.getEmail(),
                    null,
                    null);
        }

        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr()).orElse(null);
        if (alumno != null) {
            Especialidad especialidad = alumno.getEspecialidad();
            List<PlanillaSemanal> planillas =
                    planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
            return new ChromeContext(
                    "alumno",
                    nombreCompleto(alumno.getNombres(), alumno.getApellidos(), usuario.getEmail()),
                    usuario.getEmail(),
                    especialidad != null ? especialidad.getIdEsp() : null,
                    especialidad != null ? especialidad.getNombre() : null,
                    avisosDelAlumno(alumno, planillas));
        }

        return new ChromeContext("cuenta", usuario.getEmail(), usuario.getEmail(), null, null);
    }

    // Los dos avisos posibles hoy son mutuamente excluyentes (uno pide menos de
    // 6 planillas, el otro 6 o más), así que nunca hay dos a la vez -- pero se
    // arma como lista igual, no como par de booleanos, para no tener que
    // rehacer esto si mañana se agrega un tercer aviso que sí pueda convivir
    // con otro.
    private List<ChromeContext.Aviso> avisosDelAlumno(Alumno alumno, List<PlanillaSemanal> planillas) {
        if (planillas.isEmpty()) {
            return List.of(); // todavía no arrancó, nada que avisarle todavía
        }

        if (planillas.size() >= 6) {
            // findByAlumno_IdAlOrderByFechaDesdeDesc: la más nueva (la sexta) va
            // primera. Su fecha_hasta es la referencia de "cuándo pasó esto" y
            // también la clave de esta ocurrencia (ver AvisoLeido): una vez que
            // el alumno tiene sus 6 planillas esta fecha ya no cambia, así que
            // "leído" queda para siempre a menos que borre y vuelva a cargar
            // planillas -- que no es un caso que este aviso necesite cubrir.
            LocalDate fechaSextaPlanilla = planillas.get(0).getFechaHasta();
            String clave = fechaSextaPlanilla.toString();
            return List.of(new ChromeContext.Aviso(
                    "Ya se habilitaron tus documentos finales",
                    "/alumno/documentos/al-terminar",
                    "listo",
                    clave,
                    fechaSextaPlanilla.atStartOfDay(),
                    yaLeido(alumno, "listo", clave)));
        }

        // findByAlumno_IdAlOrderByFechaDesdeDesc: la más nueva va primera.
        LocalDate finUltimaSemana = planillas.get(0).getFechaHasta();

        // El sábado de la semana de fecha_hasta (puede ser la propia fecha_hasta,
        // si ya era sábado, o el sábado que le sigue si la semana terminó antes,
        // p.ej. un viernes) más una semana: el sábado de la semana SIGUIENTE a
        // la que ya cargó. Así el aviso llega el mismo día sin importar si esa
        // semana particular se trabajó hasta el viernes o hasta el sábado.
        LocalDate sabadoProximaSemana = finUltimaSemana
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                .plusWeeks(1);

        if (LocalDate.now().isBefore(sabadoProximaSemana)) {
            return List.of(); // todavía no llegó el sábado que lo activa
        }

        // La clave es esa misma fecha: si la semana que viene sigue sin cargar
        // la planilla, sabadoProximaSemana avanza siete días y ya no coincide
        // con lo que se guardó como leído -- vuelve a contar como sin leer solo,
        // sin que haga falta borrar ni tocar la fila vieja.
        String clave = sabadoProximaSemana.toString();
        return List.of(new ChromeContext.Aviso(
                "Falta cargar la planilla",
                "/alumno/planilla",
                "pendiente",
                clave,
                sabadoProximaSemana.atStartOfDay(),
                yaLeido(alumno, "pendiente", clave)));
    }

    private boolean yaLeido(Alumno alumno, String tipo, String clave) {
        return avisoLeidoRepository.findByIdAlAndTipo(alumno.getIdAl(), tipo)
                .map(leido -> clave.equals(leido.getClave()))
                .orElse(false);
    }

    // usado por fragments/auth.html para armar og:image con URL absoluta: las
    // vistas previas de redes sociales no resuelven rutas relativas como "/img/..."
    @ModelAttribute("baseUrl")
    public String cargarBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    @ModelAttribute("paginaActual")
    public String cargarPaginaActual(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return switch (ruta) {
            case "/home" -> "inicio";
            case "/alumno/documentos/antes-de-empezar" -> "documentos-antes";
            case "/alumno/documentos/al-terminar" -> "documentos-final";
            case "/alumno/documentos/informe" -> "documentos-informe";
            case "/alumno/documentos/adjuntos" -> "documentos-adjuntos";
            case "/alumno/planilla" -> "planilla";
            case "/alumno/subir" -> "subir";
            case "/admin/alumnos" -> "alumnos";
            case "/admin/reportes" -> "reportes";
            case "/admin/supervisores" -> "supervisores";
            case "/admin/empresas" -> "empresas";
            case "/cambiar-contrasena" -> "contrasena";
            default -> "";
        };
    }

    private String nombreCompleto(String nombres, String apellidos, String respaldo) {
        String nombre = ((nombres == null ? "" : nombres) + " "
                + (apellidos == null ? "" : apellidos)).trim().replaceAll("\\s+", " ");
        return nombre.isEmpty() ? respaldo : nombre;
    }
}
