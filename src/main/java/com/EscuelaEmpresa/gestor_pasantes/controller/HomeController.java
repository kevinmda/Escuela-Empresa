package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final AlumnoRepository alumnoRepository;

    public HomeController(UsuarioRepository usuarioRepository, AdministradorRepository administradorRepository,
            AlumnoRepository alumnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.alumnoRepository = alumnoRepository;
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no Encontrado"));

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

            return "alumno/index";
        }

        throw new RuntimeException("El usuario no está asociado a ningún rol");
    }

    private void cargarEstadisticas(Model model, Administrador admin) {
        List<Integer> idsEspecialidad = admin.getEspecialidades() == null
                ? List.of()
                : admin.getEspecialidades().stream()
                        .map(especialidad -> especialidad.getIdEsp())
                        .collect(Collectors.toList());

        if ("administrativo".equalsIgnoreCase(admin.getCargo())) {
            idsEspecialidad = null;
        }

        long alumnos = idsEspecialidad == null
                ? alumnoRepository.count()
                : alumnoRepository.countByEspecialidad_IdEspIn(idsEspecialidad);
        long sinSupervisor = idsEspecialidad == null
                ? alumnoRepository.countBySupervisorIsNull()
                : alumnoRepository.countByEspecialidad_IdEspInAndSupervisorIsNull(idsEspecialidad);

        model.addAttribute("cantidadAlumnos", alumnos);
        model.addAttribute("cantidadSinSupervisor", sinSupervisor);
    }
}