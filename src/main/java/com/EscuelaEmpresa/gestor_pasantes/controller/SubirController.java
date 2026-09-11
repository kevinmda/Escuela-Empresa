package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.ValidacionDocumentoService;
import com.EscuelaEmpresa.gestor_pasantes.service.ValidacionDocumentoService.ValidacionResultado;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitesDocumentoService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class SubirController {

    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final ValidacionDocumentoService validacionDocumentoService;
    private final LimitesDocumentoService limitesDocumentoService;

    // ruta configurable desde application.properties, para poder cambiarla entre
    // desarrollo (compu local) y produccion (VPS) sin tocar codigo
    @Value("${app.uploads.directorio}")
    private String directorioUploads;

    // Tamaño máximo de archivo en bytes (50 MB)
    private static final long TAMANIO_MAXIMO = 50 * 1024 * 1024;

    public SubirController(AlumnoRepository alumnoRepository, UsuarioRepository usuarioRepository,
                            DocumentoSubidoRepository documentoSubidoRepository,
                            ValidacionDocumentoService validacionDocumentoService,
                            LimitesDocumentoService limitesDocumentoService) {
        this.alumnoRepository = alumnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.validacionDocumentoService = validacionDocumentoService;
        this.limitesDocumentoService = limitesDocumentoService;
    }

    @GetMapping("/alumno/subir")
    public String mostrarSubir(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());
        model.addAttribute("documentos", documentos);
        
        // Pasar los tipos de documento disponibles al modelo
        model.addAttribute("tiposDocumento", TipoDocumento.values());

        // Pasar información de límites para cada tipo, como mapas indexados por
        // el nombre del enum (tipo.name()), para poder leerlos en el template
        // con ${limites[tipo.name()]} sin depender de nombres de atributo dinámicos
        Map<String, Integer> limites = new HashMap<>();
        Map<String, Long> subidosPorTipo = new HashMap<>();
        for (TipoDocumento tipo : TipoDocumento.values()) {
            subidosPorTipo.put(tipo.name(), limitesDocumentoService.contarDocumentosSubidos(alumno.getIdAl(), tipo));
            limites.put(tipo.name(), limitesDocumentoService.obtenerLimitePorTipo(tipo));
        }
        model.addAttribute("limites", limites);
        model.addAttribute("subidosPorTipo", subidosPorTipo);

        // El expediente completo son 10 comprobantes: 6 plantillas semanales y uno
        // de cada uno de los otros cuatro tipos. La pantalla no lo decia en ningun
        // lado; los limites vivian escondidos en atributos data del <select>.
        int totalLimite = 0;
        long totalSubidos = 0;
        for (TipoDocumento tipo : TipoDocumento.values()) {
            totalLimite += limites.get(tipo.name());
            totalSubidos += subidosPorTipo.get(tipo.name());
        }
        model.addAttribute("totalLimite", totalLimite);
        model.addAttribute("totalSubidos", totalSubidos);

        // El color de la especialidad del alumno, igual que en su inicio y en la
        // planilla semanal: es su color y este es su expediente.
        model.addAttribute("idEspecialidad",
                alumno.getEspecialidad() != null ? alumno.getEspecialidad().getIdEsp() : null);

        return "alumno/subir";
    }

    @PostMapping("/alumno/subir")
    public String subirDocumento(@RequestParam("archivo") MultipartFile archivo,
                                  @RequestParam("tipoDocumento") String tipoDocumentoStr,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Seleccioná un archivo para subir.");
            return "redirect:/alumno/subir";
        }

        // Validar que se haya seleccionado un tipo de documento
        if (tipoDocumentoStr == null || tipoDocumentoStr.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Seleccioná un tipo de documento.");
            return "redirect:/alumno/subir";
        }

        TipoDocumento tipoDocumento;
        try {
            tipoDocumento = TipoDocumento.valueOf(tipoDocumentoStr);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Tipo de documento inválido.");
            return "redirect:/alumno/subir";
        }

        // VALIDACIÓN DE LÍMITES: Verificar si ya alcanzó el máximo de subidas para este tipo
        if (!limitesDocumentoService.puedeSubirDocumento(alumno.getIdAl(), tipoDocumento)) {
            String mensaje = limitesDocumentoService.obtenerMensajeError(tipoDocumento);
            redirectAttributes.addFlashAttribute("error", mensaje);
            return "redirect:/alumno/subir";
        }
        ValidacionResultado validacion = validacionDocumentoService.validarDocumentoCompleto(archivo, TAMANIO_MAXIMO);
        
        if (!validacion.isValido() || validacion.tieneErrores()) {
            redirectAttributes.addFlashAttribute("error", 
                "El documento no es válido: " + validacion.getErrores());
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

            // Calcular hash del archivo para verificar integridad posterior
            String hashIntegridad = validacionDocumentoService.calcularSHA256(rutaCompleta);

            DocumentoSubido documento = new DocumentoSubido();
            String nombreOriginal = archivo.getOriginalFilename();
            if (nombreOriginal != null && nombreOriginal.length() > 100) {
                nombreOriginal = nombreOriginal.substring(0, 100); // el campo "nombre" en la BD es VARCHAR(100)
            }
            documento.setNombreArchivo(nombreOriginal);
            documento.setRutaArchivo(rutaCompleta.toString());
            documento.setFechaSubida(LocalDateTime.now());
            documento.setAlumno(alumno);
            documento.setTipoDocumento(tipoDocumento);
            documento.setHashIntegridad(hashIntegridad);
            documento.setValidado(true); // Se pasó todas las validaciones
            
            try {
                documentoSubidoRepository.save(documento);
            } catch (RuntimeException exception) {
                Files.deleteIfExists(rutaCompleta);
                throw exception;
            }
        }

        redirectAttributes.addFlashAttribute("exito", 
            "Documento de tipo '" + tipoDocumento.getDescripcion() + "' subido correctamente.");
        return "redirect:/alumno/subir";
    }

    @GetMapping("/alumno/subir/{idDs}/ver")
    public void verDocumento(@PathVariable Integer idDs,
                              Authentication authentication,
                              HttpServletResponse response) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        DocumentoSubido documento = documentoSubidoRepository.findById(idDs)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));

        // seguridad: que el alumno no pueda ver documentos ajenos cambiando el idDs en la URL
        if (!documento.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para ver este documento");
        }

        File archivo = new File(documento.getRutaArchivo());
        if (!archivo.exists()) {
            throw new RecursoNoEncontradoException("El archivo ya no está disponible en el servidor");
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));

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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}