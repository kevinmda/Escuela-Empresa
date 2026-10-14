package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.OpinionEmpresa;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.OpinionEmpresaRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

@Controller
public class OpinionController {

    // Cupo de opiniones "vivas" a la vez: borrar una libera lugar para otra.
    // No es un límite para siempre -- es cuántas puede tener cargadas en
    // simultáneo, para que esto no se convierta en un muro de texto infinito.
    public static final int MAXIMO_OPINIONES_VIVAS = 3;

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final OpinionEmpresaRepository opinionEmpresaRepository;

    public OpinionController(UsuarioRepository usuarioRepository,
                              AlumnoRepository alumnoRepository,
                              OpinionEmpresaRepository opinionEmpresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.opinionEmpresaRepository = opinionEmpresaRepository;
    }

    @GetMapping("/alumno/opinion")
    public String mostrarOpiniones(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        cargarModelo(model, alumno);
        return "alumno/opinion";
    }

    @PostMapping("/alumno/opinion/crear")
    public String crearOpinion(@RequestParam String comentario,
                                @RequestParam int calificacion,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        String error = validar(alumno, comentario, calificacion);
        if (error != null) {
            // Sin redirect: hace falta devolver lo que el alumno ya había
            // escrito para que no lo pierda si, por ejemplo, mandó la
            // calificación en 0 saltándose el JS del navegador.
            model.addAttribute("error", error);
            model.addAttribute("comentarioForm", comentario);
            model.addAttribute("calificacionForm", calificacion);
            cargarModelo(model, alumno);
            return "alumno/opinion";
        }

        OpinionEmpresa opinion = new OpinionEmpresa();
        opinion.setAlumno(alumno);
        opinion.setEmpresa(alumno.getEmpresa());
        opinion.setComentario(comentario.trim());
        opinion.setCalificacion(calificacion);
        opinion.setFechaCreacion(LocalDateTime.now());
        opinionEmpresaRepository.save(opinion);

        redirectAttributes.addFlashAttribute("exito", "Tu opinión se cargó correctamente.");
        return "redirect:/alumno/opinion";
    }

    @PostMapping("/alumno/opinion/{idOpi}/eliminar")
    public String eliminarOpinion(@PathVariable Integer idOpi,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        OpinionEmpresa opinion = opinionEmpresaRepository.findById(idOpi)
                .orElseThrow(() -> new RecursoNoEncontradoException("Opinión no encontrada"));

        // Seguridad: que el alumno no pueda borrar la opinión de otro cambiando el idOpi en la URL
        if (!opinion.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para eliminar esta opinión");
        }

        opinionEmpresaRepository.delete(opinion);

        redirectAttributes.addFlashAttribute("exito", "Opinión eliminada correctamente.");
        return "redirect:/alumno/opinion";
    }

    // Comparte las reglas (y los mensajes) entre el GET que pinta el botón
    // deshabilitado y el POST que vuelve a chequear del lado del servidor.
    private String validar(Alumno alumno, String comentario, int calificacion) {
        if (alumno.getEmpresa() == null) {
            return "Todavía no tenés una empresa asignada, no se puede dejar una opinión.";
        }
        if (comentario == null || comentario.isBlank()) {
            return "El comentario no puede estar vacío.";
        }
        if (comentario.trim().length() > 1000) {
            return "El comentario no puede superar los 1000 caracteres.";
        }
        if (calificacion < 1 || calificacion > 5) {
            return "Elegí una calificación de 1 a 5 estrellas.";
        }
        long opinionesVivas = opinionEmpresaRepository.countByAlumno_IdAl(alumno.getIdAl());
        if (opinionesVivas >= MAXIMO_OPINIONES_VIVAS) {
            return "Ya tenés " + MAXIMO_OPINIONES_VIVAS + " opiniones cargadas. "
                    + "Eliminá alguna para poder cargar una nueva.";
        }
        return null;
    }

    private void cargarModelo(Model model, Alumno alumno) {
        List<OpinionEmpresa> opiniones =
                opinionEmpresaRepository.findByAlumno_IdAlOrderByFechaCreacionDesc(alumno.getIdAl());

        model.addAttribute("opiniones", opiniones);
        model.addAttribute("idEspecialidad",
                alumno.getEspecialidad() != null ? alumno.getEspecialidad().getIdEsp() : null);
        model.addAttribute("tieneEmpresa", alumno.getEmpresa() != null);
        model.addAttribute("nombreEmpresa", alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : null);
        model.addAttribute("cantidadOpiniones", opiniones.size());
        model.addAttribute("maximoOpiniones", MAXIMO_OPINIONES_VIVAS);
        model.addAttribute("puedeOpinar", alumno.getEmpresa() != null && opiniones.size() < MAXIMO_OPINIONES_VIVAS);
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
