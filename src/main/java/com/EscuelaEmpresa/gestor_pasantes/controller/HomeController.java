package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.DocumentoAlmacenamientoService;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitesDocumentoService;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final LimitesDocumentoService limitesDocumentoService;
    private final DocumentoAlmacenamientoService documentoAlmacenamientoService;

    public HomeController(UsuarioRepository usuarioRepository, AdministradorRepository administradorRepository,
            AlumnoRepository alumnoRepository, PlanillaSemanalRepository planillaSemanalRepository,
            LimitesDocumentoService limitesDocumentoService,
            DocumentoAlmacenamientoService documentoAlmacenamientoService) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.limitesDocumentoService = limitesDocumentoService;
        this.documentoAlmacenamientoService = documentoAlmacenamientoService;
    }

    // la raiz del sitio no tenia handler, asi que entrar a http://localhost:8080/
    // daba 404: /login no esta en la lista de permitAll de SecurityConfig, cae en
    // .anyRequest().authenticated(), y con sesion activa (rememberMe dura 14 dias)
    // Security deja pasar la peticion hasta un mapping que no existia.
    // /login se encarga del resto: si ya hay alguien logueado, rebota a /home.
    @GetMapping("/")
    public String raiz() {
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no Encontrado"));

        Optional<Administrador> adminOpt = administradorRepository.findByUsuario_IdUsr(usuario.getIdUsr());

        if (adminOpt.isPresent()) {
            Administrador admin = adminOpt.get();
            model.addAttribute("admin", admin);
            cargarEstadisticas(model, admin);

            // Administrativo y Coordinador comparten la misma tabla/rol, pero cada uno
            // tiene su propia pantalla de inicio con su propio menú
            if ("coordinador".equalsIgnoreCase(admin.getCargo())) {
                return "coordinador/index";
            }

            return "administrador/index";
        }

        Optional<Alumno> alumnoOpt = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr());

        if (alumnoOpt.isPresent()) {
            Alumno alumno = alumnoOpt.get();
            model.addAttribute("alumno", alumno);
            model.addAttribute("avisoCambiarContrasena", Boolean.TRUE.equals(usuario.getContrasenaPorDefecto()));

            // Datos en vivo para las tarjetas de "Tus pantallas": antes eran
            // solo un link con una descripción fija, y la de Planilla Semanal
            // sobre todo se presta a decir dónde está parado el alumno sin
            // que tenga que entrar a mirar.
            List<PlanillaSemanal> planillas =
                    planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
            model.addAttribute("cantidadPlanillas", planillas.size());

            BigDecimal totalHorasPasantia = planillas.stream()
                    .map(PlanillaSemanal::getTotalHoras)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // stripTrailingZeros + toPlainString: "48.00" queda "48", "48.50"
            // queda "48.5" -- mismo criterio que ya usa el total en vivo de
            // planillaSemanal.js, para no mostrar decimales de sobra cuando
            // el total da redondo (toPlainString y no toString: evita que un
            // valor como "100" salga en notación científica).
            model.addAttribute("totalHorasPasantia", totalHorasPasantia.stripTrailingZeros().toPlainString());

            model.addAttribute("totalLimiteDocumentos", limitesDocumentoService.obtenerLimiteTotal(alumno.getIdAl()));
            model.addAttribute("totalSubidosDocumentos", limitesDocumentoService.contarTotalSubidos(alumno.getIdAl()));

            return "alumno/index";
        }

        throw new ReglaNegocioException("El usuario no está asociado a ningún rol");
    }

    // Casilla de "Conservar mis datos al terminar" en el Inicio del alumno. No
    // marca egresado directamente (eso lo decide el sistema solo, ver
    // DocumentoAlmacenamientoService.marcarEgresadoSiCorresponde): si el
    // alumno la marca y ya tiene subidos sus Documentos Adjuntos, recién ahí
    // se dispara. Desmarcarla es inofensivo -- si el alumno ya está egresado,
    // ni siquiera puede volver a entrar para tocarla.
    @PostMapping("/alumno/autoriza-conservar-datos")
    @ResponseBody
    public void autorizarConservarDatos(@RequestParam boolean autoriza, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // Es de un solo sentido: una vez activada, queda fija. El toggle del
        // lado del cliente ya queda disabled así que en el uso normal esto
        // nunca llega, pero esta es la regla de verdad -- la del cliente es
        // solo para que no haga falta tocarla a mano para notarlo.
        if (alumno.isAutorizaConservarDatos()) {
            return;
        }

        alumno.setAutorizaConservarDatos(autoriza);
        alumnoRepository.save(alumno);
        documentoAlmacenamientoService.marcarEgresadoSiCorresponde(alumno);
    }

    // CI, sexo, fecha de nacimiento y teléfono en el Inicio del alumno: los
    // alumnos cargados antes de pedir estos datos pueden tener cualquiera de
    // los cuatro vacío, y en vez de dejarlos en blanco para siempre, el
    // propio alumno los completa ahí mismo. Cada parámetro es opcional
    // porque el formulario de cada campo solo manda el suyo (ver
    // alumno/index.html) -- y aunque llegaran los cuatro juntos, cada uno se
    // aplica por separado.
    //
    // Completa, no corrige: solo pisa el campo si en la base todavía está
    // vacío. Así alguien no puede mandar este POST a mano para cambiarse el
    // CI o el teléfono una vez que ya los tiene cargados -- para eso hay que
    // pedírselo a la coordinación, igual que con cualquier otro dato ya
    // asentado del alumno.
    @PostMapping("/alumno/completar-datos")
    public String completarDatos(
            @RequestParam(required = false) String ci,
            @RequestParam(required = false) String sexo,
            @RequestParam(required = false) String fechaNac,
            @RequestParam(required = false) String telefono,
            Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        if (ci != null && !ci.isBlank() && esVacio(alumno.getCi())) {
            alumno.setCi(ci.trim());
        }
        if (sexo != null && !sexo.isBlank() && esVacio(alumno.getSexo())) {
            alumno.setSexo(sexo.trim());
        }
        if (fechaNac != null && !fechaNac.isBlank() && alumno.getFechaNac() == null) {
            // El input es type="date": si llega algo que no es una fecha
            // válida es porque alguien saltó el formulario, no un typo del
            // alumno. Se ignora en vez de romper la pantalla con un 500.
            try {
                alumno.setFechaNac(LocalDate.parse(fechaNac));
            } catch (DateTimeParseException ignorado) {
                // nada que guardar
            }
        }
        if (telefono != null && !telefono.isBlank() && esVacio(alumno.getTelefono())) {
            alumno.setTelefono(telefono.trim());
        }

        alumnoRepository.save(alumno);
        return "redirect:/home";
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no Encontrado"));
        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }

    private void cargarEstadisticas(Model model, Administrador admin) {
        List<Integer> idsEspecialidad = admin.getEspecialidad() == null
                ? List.of()
                : List.of(admin.getEspecialidad().getIdEsp());

        if ("administrativo".equalsIgnoreCase(admin.getCargo())) {
            idsEspecialidad = null;
        }

        long alumnos = idsEspecialidad == null
                ? alumnoRepository.countByEgresadoFalse()
                : alumnoRepository.countByEspecialidad_IdEspInAndEgresadoFalse(idsEspecialidad);
        long sinSupervisor = idsEspecialidad == null
                ? alumnoRepository.countBySupervisorIsNullAndEgresadoFalse()
                : alumnoRepository.countByEspecialidad_IdEspInAndSupervisorIsNullAndEgresadoFalse(idsEspecialidad);

        // % de alumnos que ya entregaron su planilla de la semana actual (lunes a sábado)
        LocalDate lunesSemanaActual = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sabadoSemanaActual = lunesSemanaActual.plusDays(5);

        long entregaron = idsEspecialidad == null
                ? planillaSemanalRepository.countAlumnosConEntregaEnSemanaGlobal(lunesSemanaActual, sabadoSemanaActual)
                : planillaSemanalRepository.countAlumnosConEntregaEnSemana(idsEspecialidad, lunesSemanaActual, sabadoSemanaActual);

        long porcentajeCumplimiento = alumnos == 0 ? 0 : Math.round((entregaron * 100.0) / alumnos);

        model.addAttribute("cantidadAlumnos", alumnos);
        model.addAttribute("cantidadSinSupervisor", sinSupervisor);
        model.addAttribute("porcentajeCumplimiento", porcentajeCumplimiento);
    }
}