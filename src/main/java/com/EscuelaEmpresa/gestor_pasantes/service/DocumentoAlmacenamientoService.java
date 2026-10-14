package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;

// Extraido de SubirController.subirDocumento, donde antes vivia inline: asi tanto el
// formulario web (/alumno/subir) como la API movil (/api/movil/documentos) guardan un
// documento exactamente de la misma forma, sin duplicar el manejo de archivos.
@Service
public class DocumentoAlmacenamientoService {

    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final ValidacionDocumentoService validacionDocumentoService;
    private final AlumnoRepository alumnoRepository;

    // ruta configurable desde application.properties, para poder cambiarla entre
    // desarrollo (compu local) y produccion (VPS) sin tocar codigo
    @Value("${app.uploads.directorio}")
    private String directorioUploads;

    public DocumentoAlmacenamientoService(DocumentoSubidoRepository documentoSubidoRepository,
                                           ValidacionDocumentoService validacionDocumentoService,
                                           AlumnoRepository alumnoRepository) {
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.validacionDocumentoService = validacionDocumentoService;
        this.alumnoRepository = alumnoRepository;
    }

    public DocumentoSubido guardar(MultipartFile archivo, TipoDocumento tipoDocumento, Alumno alumno) throws IOException {
        // nombre unico en el filesystem para evitar que dos alumnos (o el mismo alumno
        // dos veces) pisen el archivo del otro si suben algo con el mismo nombre original
        String nombreUnico = UUID.randomUUID() + ".pdf";
        Path carpetaDestino = Paths.get(directorioUploads);
        Files.createDirectories(carpetaDestino);

        Path rutaCompleta = carpetaDestino.resolve(nombreUnico);

        // OJO: no usamos archivo.transferTo(rutaCompleta.toFile()) porque, con una ruta
        // relativa, Tomcat la resuelve contra SU PROPIO directorio temporal interno
        // (no contra el directorio de trabajo de la app), lo que tira FileNotFoundException.
        // Usando Files.copy() con el InputStream, escribimos nosotros mismos con la ruta
        // exacta que calculamos, sin depender de esa resolucion ambigua de Tomcat.
        try (InputStream inputStream = archivo.getInputStream()) {
            Files.copy(inputStream, rutaCompleta, StandardCopyOption.REPLACE_EXISTING);

            String hashIntegridad = validacionDocumentoService.calcularSHA256(rutaCompleta);

            DocumentoSubido documento = new DocumentoSubido();
            String nombreOriginal = archivo.getOriginalFilename();
            if (nombreOriginal != null && nombreOriginal.length() > 100) {
                nombreOriginal = nombreOriginal.substring(0, 100); // el campo "nombre" en la BD es VARCHAR(100)
            }
            documento.setNombreArchivo(nombreOriginal);
            documento.setRutaArchivo(rutaCompleta.toString());
            // Zona horaria explícita: LocalDateTime.now() a secas usa la del
            // propio servidor, que puede no estar en hora de Paraguay.
            documento.setFechaSubida(LocalDateTime.now(ZoneId.of("America/Asuncion")));
            documento.setAlumno(alumno);
            documento.setTipoDocumento(tipoDocumento);
            documento.setHashIntegridad(hashIntegridad);
            documento.setValidado(true); // se paso todas las validaciones

            DocumentoSubido guardado;
            try {
                guardado = documentoSubidoRepository.save(documento);
            } catch (RuntimeException exception) {
                Files.deleteIfExists(rutaCompleta);
                throw exception;
            }

            // Documentos Adjuntos es el expediente final combinado y firmado:
            // el último paso del proceso (ver LimitesDocumentoService, solo se
            // puede generar con el expediente completo). Si el alumno ya dio su
            // autorización, es el momento de pasarlo a egresado sin que nadie
            // tenga que marcarlo a mano.
            if (tipoDocumento == TipoDocumento.DOCUMENTOS_ADJUNTOS) {
                marcarEgresadoSiCorresponde(alumno);
            }

            return guardado;
        }
    }

    // También la usa el camino inverso: cuando el alumno marca la casilla de
    // autorización DESPUÉS de haber subido ya sus Documentos Adjuntos (en vez
    // de antes, que es el camino de arriba). Las dos puntas convergen en la
    // misma condición, así que viven en un solo lugar.
    public void marcarEgresadoSiCorresponde(Alumno alumno) {
        if (alumno.isEgresado() || !alumno.isAutorizaConservarDatos()) {
            return;
        }

        boolean subioDocumentosAdjuntos = documentoSubidoRepository
                .countByAlumno_IdAlAndTipoDocumento(alumno.getIdAl(), TipoDocumento.DOCUMENTOS_ADJUNTOS) > 0;

        if (subioDocumentosAdjuntos) {
            alumno.setEgresado(true);
            alumnoRepository.save(alumno);
        }
    }
}
