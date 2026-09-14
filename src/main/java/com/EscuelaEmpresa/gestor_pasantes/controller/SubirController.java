package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
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
import com.EscuelaEmpresa.gestor_pasantes.service.DocumentoAlmacenamientoService;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class SubirController {

    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final ValidacionDocumentoService validacionDocumentoService;
    private final LimitesDocumentoService limitesDocumentoService;
    private final DocumentoAlmacenamientoService documentoAlmacenamientoService;

    // El tope sale de application.properties, el mismo valor con el que Spring corta
    // la subida. Antes habia dos numeros distintos: aca decia 50 MB y las properties
    // 10 MB. Ganaba el de Spring, que corta el request ANTES de llegar hasta aca, asi
    // que un archivo de 12 MB daba un error 500 en vez del mensaje que dice justamente
    // que el archivo es muy grande -- y la validacion de 50 MB no se ejecutaba nunca.
    private final DataSize tamanioMaximo;

    public SubirController(AlumnoRepository alumnoRepository, UsuarioRepository usuarioRepository,
                            DocumentoSubidoRepository documentoSubidoRepository,
                            ValidacionDocumentoService validacionDocumentoService,
                            LimitesDocumentoService limitesDocumentoService,
                            DocumentoAlmacenamientoService documentoAlmacenamientoService,
                            @Value("${spring.servlet.multipart.max-file-size}") DataSize tamanioMaximo) {
        this.tamanioMaximo = tamanioMaximo;
        this.alumnoRepository = alumnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.validacionDocumentoService = validacionDocumentoService;
        this.limitesDocumentoService = limitesDocumentoService;
        this.documentoAlmacenamientoService = documentoAlmacenamientoService;
    }

    @GetMapping("/alumno/subir")
    public String mostrarSubir(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());
        model.addAttribute("documentos", documentos);

        // Los cinco tipos del expediente van en la grilla principal.
        // DOCUMENTOS_ADJUNTOS no entra ahí: tiene su propio apartado, más abajo.
        List<TipoDocumento> tiposExpediente = limitesDocumentoService.obtenerTiposExpediente();
        model.addAttribute("tiposDocumento", tiposExpediente);

        // Pasar información de límites para cada tipo, como mapas indexados por
        // el nombre del enum (tipo.name()), para poder leerlos en el template
        // con ${limites[tipo.name()]} sin depender de nombres de atributo dinámicos
        Map<String, Integer> limites = new HashMap<>();
        Map<String, Long> subidosPorTipo = new HashMap<>();
        for (TipoDocumento tipo : tiposExpediente) {
            subidosPorTipo.put(tipo.name(), limitesDocumentoService.contarDocumentosSubidos(alumno.getIdAl(), tipo));
            limites.put(tipo.name(), limitesDocumentoService.obtenerLimitePorTipo(tipo, alumno.getIdAl()));
        }
        model.addAttribute("limites", limites);
        model.addAttribute("subidosPorTipo", subidosPorTipo);

        // El expediente completo son 9 o 10 comprobantes (según cuántas
        // plantillas semanales le hicieron falta a este alumno) y uno de cada
        // uno de los otros cuatro tipos. La pantalla no lo decia en ningun
        // lado; los limites vivian escondidos en atributos data del <select>.
        model.addAttribute("totalLimite", limitesDocumentoService.obtenerLimiteTotal(alumno.getIdAl()));
        model.addAttribute("totalSubidos", limitesDocumentoService.contarTotalSubidos(alumno.getIdAl()));

        // Apartado exclusivo de "Documentos adjuntos": el expediente ya combinado
        // (ver /alumno/imprimir) firmado y escaneado de vuelta. Solo se habilita
        // una vez completos los diez comprobantes, y admite un solo archivo.
        boolean expedienteCompleto = limitesDocumentoService.expedienteCompleto(alumno.getIdAl());
        DocumentoSubido documentoAdjuntos = documentos.stream()
                .filter(documento -> documento.getTipoDocumento() == TipoDocumento.DOCUMENTOS_ADJUNTOS)
                .findFirst()
                .orElse(null);
        model.addAttribute("expedienteCompleto", expedienteCompleto);
        model.addAttribute("documentoAdjuntos", documentoAdjuntos);
        model.addAttribute("puedeSubirAdjuntos", expedienteCompleto && documentoAdjuntos == null);

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

        // DOCUMENTOS_ADJUNTOS es el expediente ya combinado (los otros diez
        // comprobantes): no tiene sentido subirlo antes de que esos diez estén
        // completos. La pantalla ya lo deja deshabilitado hasta entonces; esto
        // es el mismo chequeo del lado del servidor.
        if (tipoDocumento == TipoDocumento.DOCUMENTOS_ADJUNTOS
                && !limitesDocumentoService.expedienteCompleto(alumno.getIdAl())) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no completaste los " + limitesDocumentoService.obtenerLimiteTotal(alumno.getIdAl())
                    + " comprobantes del expediente. " +
                    "Documentos Adjuntos se habilita recién cuando esa entrega esté completa.");
            return "redirect:/alumno/subir";
        }

        // VALIDACIÓN DE LÍMITES: Verificar si ya alcanzó el máximo de subidas para este tipo
        if (!limitesDocumentoService.puedeSubirDocumento(alumno.getIdAl(), tipoDocumento)) {
            String mensaje = limitesDocumentoService.obtenerMensajeError(tipoDocumento, alumno.getIdAl());
            redirectAttributes.addFlashAttribute("error", mensaje);
            return "redirect:/alumno/subir";
        }
        ValidacionResultado validacion = validacionDocumentoService.validarDocumentoCompleto(archivo, tamanioMaximo.toBytes());
        
        if (!validacion.isValido() || validacion.tieneErrores()) {
            redirectAttributes.addFlashAttribute("error", 
                "El documento no es válido: " + validacion.getErrores());
            return "redirect:/alumno/subir";
        }

        documentoAlmacenamientoService.guardar(archivo, tipoDocumento, alumno);

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
        // de PDF, en vez de forzar la descarga. El nombre lo eligio el alumno al subir
        // el archivo, asi que va sanitizado y codificado: pegado crudo, uno con comillas,
        // punto y coma o acentos partia la cabecera o llegaba mal al navegador.
        response.setHeader("Content-Disposition",
                Descarga.inline(documento.getNombreArchivo(), "documento_" + documento.getIdDs() + ".pdf"));

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