package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.EscuelaEmpresa.gestor_pasantes.dto.movil.DocumentoDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.DocumentoAlmacenamientoService;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitesDocumentoService;
import com.EscuelaEmpresa.gestor_pasantes.service.ValidacionDocumentoService;
import com.EscuelaEmpresa.gestor_pasantes.service.ValidacionDocumentoService.ValidacionResultado;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

// Mismos flujos que SubirController (web), pero en JSON: listar (paginado), subir,
// descargar y eliminar los documentos del alumno autenticado por JWT.
@RestController
@RequestMapping("/api/movil/documentos")
public class DocumentosMovilController {

    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final ValidacionDocumentoService validacionDocumentoService;
    private final LimitesDocumentoService limitesDocumentoService;
    private final DocumentoAlmacenamientoService documentoAlmacenamientoService;
    private final DataSize tamanioMaximo;

    public DocumentosMovilController(AlumnoRepository alumnoRepository, UsuarioRepository usuarioRepository,
                                      DocumentoSubidoRepository documentoSubidoRepository,
                                      ValidacionDocumentoService validacionDocumentoService,
                                      LimitesDocumentoService limitesDocumentoService,
                                      DocumentoAlmacenamientoService documentoAlmacenamientoService,
                                      @Value("${spring.servlet.multipart.max-file-size}") DataSize tamanioMaximo) {
        this.alumnoRepository = alumnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.validacionDocumentoService = validacionDocumentoService;
        this.limitesDocumentoService = limitesDocumentoService;
        this.documentoAlmacenamientoService = documentoAlmacenamientoService;
        this.tamanioMaximo = tamanioMaximo;
    }

    @GetMapping
    public Page<DocumentoDTO> listar(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        return documentoSubidoRepository
                .findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl(), PageRequest.of(page, size))
                .map(DocumentoDTO::new);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentoDTO subir(@RequestParam("archivo") MultipartFile archivo,
                               @RequestParam("tipoDocumento") String tipoDocumentoStr,
                               Authentication authentication) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        if (archivo.isEmpty()) {
            throw new ReglaNegocioException("Seleccioná un archivo para subir.");
        }

        TipoDocumento tipoDocumento;
        try {
            tipoDocumento = TipoDocumento.valueOf(tipoDocumentoStr);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Tipo de documento inválido.");
        }

        if (!limitesDocumentoService.puedeSubirDocumento(alumno.getIdAl(), tipoDocumento)) {
            throw new ReglaNegocioException(limitesDocumentoService.obtenerMensajeError(tipoDocumento, alumno.getIdAl()));
        }

        ValidacionResultado validacion =
                validacionDocumentoService.validarDocumentoCompleto(archivo, tamanioMaximo.toBytes());
        if (!validacion.isValido() || validacion.tieneErrores()) {
            throw new ReglaNegocioException("El documento no es válido: " + validacion.getErrores());
        }

        DocumentoSubido documento = documentoAlmacenamientoService.guardar(archivo, tipoDocumento, alumno);
        return new DocumentoDTO(documento);
    }

    @GetMapping("/{idDs}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer idDs, Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        DocumentoSubido documento = obtenerDocumentoPropio(idDs, alumno);

        File archivo = new File(documento.getRutaArchivo());
        if (!archivo.exists()) {
            throw new RecursoNoEncontradoException("El archivo ya no está disponible en el servidor");
        }

        byte[] contenido = Files.readAllBytes(archivo.toPath());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition",
                        Descarga.adjunto(documento.getNombreArchivo(), "documento_" + documento.getIdDs() + ".pdf"))
                .body(contenido);
    }

    @DeleteMapping("/{idDs}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idDs, Authentication authentication) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        DocumentoSubido documento = obtenerDocumentoPropio(idDs, alumno);

        File archivo = new File(documento.getRutaArchivo());
        if (archivo.exists()) {
            Files.delete(archivo.toPath());
        }
        documentoSubidoRepository.delete(documento);

        return ResponseEntity.noContent().build();
    }

    private DocumentoSubido obtenerDocumentoPropio(Integer idDs, Alumno alumno) {
        DocumentoSubido documento = documentoSubidoRepository.findById(idDs)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));

        if (!documento.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("No tenés permiso para acceder a este documento");
        }
        return documento;
    }

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
