package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletResponse;

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

        // OJO: no usamos archivo.transferTo(rutaCompleta.toFile()) porque, con una ruta
        // relativa, Tomcat la resuelve contra SU PROPIO directorio temporal interno
        // (no contra el directorio de trabajo de la app), lo que tira FileNotFoundException.
        // Usando Files.copy() con el InputStream, escribimos nosotros mismos con la ruta
        // exacta que calculamos, sin depender de esa resolucion ambigua de Tomcat.
        try (java.io.InputStream inputStream = archivo.getInputStream()) {
            Files.copy(inputStream, rutaCompleta, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            DocumentoSubido documento = new DocumentoSubido();
            String nombreOriginal = archivo.getOriginalFilename();
            if (nombreOriginal != null && nombreOriginal.length() > 100) {
                nombreOriginal = nombreOriginal.substring(0, 100); // el campo "nombre" en la BD es VARCHAR(100)
            }
            documento.setNombreArchivo(nombreOriginal);
            documento.setRutaArchivo(rutaCompleta.toString());
            documento.setFechaSubida(LocalDateTime.now());
            documento.setAlumno(alumno);
            try {
                documentoSubidoRepository.save(documento);
            } catch (RuntimeException exception) {
                Files.deleteIfExists(rutaCompleta);
                throw exception;
            }
        }

        redirectAttributes.addFlashAttribute("exito", "Documento subido correctamente.");
        return "redirect:/alumno/subir";
    }

    @GetMapping("/alumno/subir/{idDs}/ver")
    public void verDocumento(@PathVariable Integer idDs,
                              Authentication authentication,
                              HttpServletResponse response) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        DocumentoSubido documento = documentoSubidoRepository.findById(idDs)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        // seguridad: que el alumno no pueda ver documentos ajenos cambiando el idDs en la URL
        if (!documento.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para ver este documento");
        }

        File archivo = new File(documento.getRutaArchivo());
        if (!archivo.exists()) {
            throw new RuntimeException("El archivo ya no está disponible en el servidor");
        }

        response.setContentType("application/pdf");
        // "inline" (no "attachment") para que el navegador lo abra en la misma pestaña/visor
        // de PDF, en vez de forzar la descarga
        response.setHeader("Content-Disposition", "inline; filename=" + documento.getNombreArchivo());

        Files.copy(archivo.toPath(), response.getOutputStream());
    }

    @PostMapping("/alumno/subir/{idDs}/eliminar")
    public String eliminarDocumento(@PathVariable Integer idDs,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        DocumentoSubido documento = documentoSubidoRepository.findById(idDs)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        // seguridad: que el alumno no pueda borrar documentos ajenos cambiando el idDs en la URL
        if (!documento.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para eliminar este documento");
        }

        // borramos el archivo fisico primero; si no existiera (por algun motivo ya se perdio),
        // igual seguimos y limpiamos el registro de la BD para no dejar basura ahi
        File archivo = new File(documento.getRutaArchivo());
        if (archivo.exists()) {
            Files.delete(archivo.toPath());
        }

        documentoSubidoRepository.delete(documento);

        redirectAttributes.addFlashAttribute("exito", "Documento eliminado correctamente.");
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
