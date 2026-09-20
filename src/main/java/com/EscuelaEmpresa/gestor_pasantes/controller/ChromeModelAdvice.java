package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.EscuelaEmpresa.gestor_pasantes.dto.ChromeContext;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.AvisoLeido;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AvisoLeidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitesDocumentoService;
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
    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final LimitesDocumentoService limitesDocumentoService;
    private final AvisoLeidoRepository avisoLeidoRepository;

    public ChromeModelAdvice(UsuarioRepository usuarioRepository,
                             AlumnoRepository alumnoRepository,
                             AdministradorRepository administradorRepository,
                             PlanillaSemanalRepository planillaSemanalRepository,
                             DocumentoSubidoRepository documentoSubidoRepository,
                             LimitesDocumentoService limitesDocumentoService,
                             AvisoLeidoRepository avisoLeidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.administradorRepository = administradorRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.limitesDocumentoService = limitesDocumentoService;
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

    // Los dos avisos posibles hoy pueden convivir (a diferencia de antes: uno
    // dependía de las planillas cargadas en el sistema, el otro de los
    // documentos subidos como PDF, son cosas independientes), por eso es una
    // lista real y no un par de casos sueltos. Las dos condiciones se vuelven
    // a evaluar en cada carga de página desde el estado actual (cuántas
    // planillas/documentos hay ahora mismo) -- si se borra una planilla o un
    // documento y la condición deja de cumplirse, el aviso correspondiente
    // deja de aparecer solo, sin que haga falta borrar nada de aviso_leido.
    private List<ChromeContext.Aviso> avisosDelAlumno(Alumno alumno, List<PlanillaSemanal> planillas) {
        List<ChromeContext.Aviso> avisos = new ArrayList<>();

        if (planillas.size() >= 6) {
            // findByAlumno_IdAlOrderByFechaDesdeDesc: la más nueva (la sexta) va
            // primera.
            PlanillaSemanal sextaPlanilla = planillas.get(0);
            LocalDate fechaSextaPlanilla = sextaPlanilla.getFechaHasta();
            // La clave es el id de la planilla, no su fecha_hasta: si se borra
            // la sexta y se vuelve a cargar con las MISMAS fechas de semana
            // (lo más probable al reintentar, sobre todo probando), fecha_hasta
            // da igual que antes y el aviso seguía saliendo como ya leído --
            // exactamente el mismo problema que ya se corrigió para
            // documentos_adjuntos, pero con el id de planilla en vez del id de
            // documento. El id, en cambio, es autoincremental: la planilla
            // nueva SIEMPRE tiene uno distinto al de la que se borró, aunque
            // elijas la semana con las mismas fechas.
            String clave = String.valueOf(sextaPlanilla.getIdPs());
            // fecha_hasta la elige el alumno al cargar la planilla, y puede caer
            // después de hoy (una semana de prueba con fecha futura, por
            // ejemplo) -- sin este límite, ese aviso terminaba ordenándose como
            // "más reciente" que una subida real de hoy en documentos_adjuntos,
            // aunque en el mundo real no haya pasado todavía. min() lo acota a
            // hoy como máximo, nunca antes de tiempo. Zona horaria explícita,
            // mismo motivo que en ChromeContext.etiquetaDia().
            LocalDate hoyEnParaguay = LocalDate.now(ZoneId.of("America/Asuncion"));
            LocalDateTime momento = fechaSextaPlanilla.isAfter(hoyEnParaguay)
                    ? hoyEnParaguay.atStartOfDay()
                    : fechaSextaPlanilla.atStartOfDay();
            avisos.add(new ChromeContext.Aviso(
                    "Ya se habilitaron tus documentos finales",
                    "/alumno/documentos/al-terminar",
                    "listo",
                    "documentos_finales",
                    clave,
                    momento,
                    yaLeido(alumno, "documentos_finales", clave)));
        }

        // Documentos Adjuntos (el expediente combinado, en /alumno/documentos/adjuntos)
        // y su caja de subida en Subir se habilitan con los 10 comprobantes ya
        // subidos como PDF -- algo aparte e independiente de las planillas
        // cargadas en el sistema arriba. Una vez que el propio combinado ya se
        // subió de vuelta, el aviso deja de tener sentido (puedeSubirAdjuntos en
        // SubirController usa exactamente esta misma condición para ocultar esa
        // caja), así que tampoco se muestra en ese caso.
        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());
        boolean yaSubioAdjuntos = documentos.stream()
                .anyMatch(d -> d.getTipoDocumento() == TipoDocumento.DOCUMENTOS_ADJUNTOS);
        long totalSubidos = limitesDocumentoService.contarTotalSubidos(alumno.getIdAl());
        int totalLimite = limitesDocumentoService.obtenerLimiteTotal();

        if (totalSubidos >= totalLimite && !yaSubioAdjuntos) {
            // El de mayor id (no el de fecha_subida más reciente) es, por
            // construcción, el que completó el umbral: fecha_subida es un
            // DATETIME sin fracción de segundo en la base, así que dos
            // documentos subidos dentro del mismo segundo (por ejemplo,
            // eliminar uno y subir el reemplazo enseguida, como al probar)
            // pueden guardar la MISMA fecha exacta -- "el más reciente por
            // fecha" queda ambiguo justo en ese caso. El id, en cambio, es
            // autoincremental y siempre estrictamente creciente, así que no
            // tiene ese problema.
            documentos.stream()
                    .filter(d -> d.getTipoDocumento() != TipoDocumento.DOCUMENTOS_ADJUNTOS)
                    .max(Comparator.comparing(DocumentoSubido::getIdDs))
                    .ifPresent(masReciente -> {
                        String clave = String.valueOf(masReciente.getIdDs());
                        avisos.add(new ChromeContext.Aviso(
                                "Ya se habilitó el expediente combinado en Documentos adjuntos y su caja de subida en Subir",
                                "/alumno/documentos/adjuntos",
                                "listo",
                                "documentos_adjuntos",
                                clave,
                                masReciente.getFechaSubida(),
                                yaLeido(alumno, "documentos_adjuntos", clave)));
                    });
        }

        return avisos;
    }

    private boolean yaLeido(Alumno alumno, String codigo, String clave) {
        return avisoLeidoRepository.findByIdAlAndCodigo(alumno.getIdAl(), codigo)
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
