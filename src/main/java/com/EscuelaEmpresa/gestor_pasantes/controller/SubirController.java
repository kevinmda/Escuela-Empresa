package com.EscuelaEmpresa.gestor_pasantes.controller;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
public class SubirController {

    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoSubidoRepository documentoSubidoRepository;

    // ruta configurable desde application.properties, para poder cambiarla entre
    // desarrollo (compu local) y produccion (VPS) sin tocar codigo
    @Value("${app.uploads.directorio}")
    private String directorioUploads;

    public SubirController(AlumnoRepository alumnoRepository, UsuarioRepository usuarioRepository,
                            DocumentoSubidoRepository documentoSubidoRepository) {
        this.alumnoRepository = alumnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
    }

    @GetMapping("/alumno/subir")
    public String mostrarSubir(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());
        model.addAttribute("documentos", documentos);

        return "alumno/subir";
    }

    @PostMapping("/alumno/subir")
    public String subirDocumento(@RequestParam("archivo") MultipartFile archivo,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Seleccioná un archivo para subir.");
            return "redirect:/alumno/subir";
        }

        // chequeo basico de que sea un PDF (se puede falsear cambiando la extension,
        // pero evita que alguien suba una imagen o un .docx por error de un click)
        boolean esPdf = "application/pdf".equals(archivo.getContentType())
                || (archivo.getOriginalFilename() != null && archivo.getOriginalFilename().toLowerCase().endsWith(".pdf"));

        if (!esPdf) {
            redirectAttributes.addFlashAttribute("error", "El archivo debe ser un PDF.");
            return "redirect:/alumno/subir";
        }

        // nombre unico en el filesystem para evitar que dos alumnos (o el mismo alumno dos veces)
        // pisen el archivo del otro si suben algo con el mismo nombre original
        String nombreUnico = UUID.randomUUID() + ".pdf";
        Path carpetaDestino = Paths.get(directorioUploads);
        Files.createDirectories(carpetaDestino);

        Path rutaCompleta = carpetaDestino.resolve(nombreUnico);
        archivo.transferTo(rutaCompleta.toFile());

        DocumentoSubido documento = new DocumentoSubido();
        documento.setNombreArchivo(archivo.getOriginalFilename());
        documento.setRutaArchivo(rutaCompleta.toString());
        documento.setFechaSubida(LocalDateTime.now());
        documento.setAlumno(alumno);
        documentoSubidoRepository.save(documento);

        redirectAttributes.addFlashAttribute("exito", "Documento subido correctamente.");
        return "redirect:/alumno/subir";
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }
}
