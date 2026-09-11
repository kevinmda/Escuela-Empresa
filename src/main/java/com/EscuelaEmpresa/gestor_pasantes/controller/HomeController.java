package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;

    public HomeController(UsuarioRepository usuarioRepository, AdministradorRepository administradorRepository,
            AlumnoRepository alumnoRepository, PlanillaSemanalRepository planillaSemanalRepository) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
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

            return "alumno/index";
        }

        throw new ReglaNegocioException("El usuario no está asociado a ningún rol");
    }

    private void cargarEstadisticas(Model model, Administrador admin) {
        List<Integer> idsEspecialidad = admin.getEspecialidad() == null
                ? List.of()
                : List.of(admin.getEspecialidad().getIdEsp());

        if ("administrativo".equalsIgnoreCase(admin.getCargo())) {
            idsEspecialidad = null;
        }

        long alumnos = idsEspecialidad == null
                ? alumnoRepository.count()
                : alumnoRepository.countByEspecialidad_IdEspIn(idsEspecialidad);
        long sinSupervisor = idsEspecialidad == null
                ? alumnoRepository.countBySupervisorIsNull()
                : alumnoRepository.countByEspecialidad_IdEspInAndSupervisorIsNull(idsEspecialidad);

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