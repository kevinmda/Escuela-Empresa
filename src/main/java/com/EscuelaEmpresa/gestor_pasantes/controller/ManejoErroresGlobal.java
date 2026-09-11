package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Unico lugar donde una excepcion que se escapa de un controlador se convierte en una
// respuesta. Antes no habia ninguno: cualquier throw terminaba en la "Whitelabel Error
// Page" de Spring, con el stack trace en consola y un 500 aunque el problema fuera
// "esa planilla no existe" o "no tenes permiso".
//
// Cada handler decide dos cosas: el codigo HTTP (404 / 403 / 400 / 413 / 500) y el
// texto que ve el usuario. La pagina es siempre templates/error.html, la misma que
// Spring Boot usa para los errores que ocurren ANTES de llegar a un controlador (una
// URL inexistente, un 403 del filtro de seguridad), asi todas se ven iguales.
//
// Las llamadas AJAX del filtro de alumnos (/admin/api/**) reciben JSON en vez de
// HTML, para que el fetch() del navegador pueda leer el mensaje.
@ControllerAdvice
public class ManejoErroresGlobal {

    private static final Logger log = LoggerFactory.getLogger(ManejoErroresGlobal.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public Object noEncontrado(RecursoNoEncontradoException e, HttpServletRequest request, HttpServletResponse response) {
        return responder(HttpStatus.NOT_FOUND, "No encontramos lo que buscás", e.getMessage(), request, response);
    }

    // Spring 6.1+ lanza esto para URLs que no coinciden con ningun mapping (antes
    // devolvia 404 en silencio). Sin este handler, el generico de abajo lo
    // convertiria en un 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public Object rutaInexistente(NoResourceFoundException e, HttpServletRequest request, HttpServletResponse response) {
        return responder(HttpStatus.NOT_FOUND, "Esta página no existe",
                "Revisá la dirección o volvé al inicio.", request, response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object accesoDenegado(AccessDeniedException e, HttpServletRequest request, HttpServletResponse response) {
        return responder(HttpStatus.FORBIDDEN, "No tenés permiso para esto", e.getMessage(), request, response);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public Object reglaNegocio(ReglaNegocioException e, HttpServletRequest request, HttpServletResponse response) {
        return responder(HttpStatus.BAD_REQUEST, "No se pudo completar la acción", e.getMessage(), request, response);
    }

    // La lanza el resolver de multipart antes de entrar al controlador, asi que el
    // mensaje amigable de SubirController nunca llegaba a ejecutarse.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object archivoDemasiadoGrande(MaxUploadSizeExceededException e, HttpServletRequest request, HttpServletResponse response) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo es demasiado grande",
                "El archivo supera el tamaño máximo permitido. Probá con un PDF más liviano.", request, response);
    }

    @ExceptionHandler(Exception.class)
    public Object inesperado(Exception e, HttpServletRequest request, HttpServletResponse response) {
        // Este si se loguea completo: es el unico caso que no esperabamos y que
        // alguien tiene que ir a mirar. Los de arriba son parte del uso normal.
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Algo salió mal",
                "Ocurrió un error inesperado. Si se repite, avisale a coordinación.", request, response);
    }

    private Object responder(HttpStatus estado, String titulo, String detalle,
                             HttpServletRequest request, HttpServletResponse response) {

        // Las descargas (PDF, ZIP, DOCX) escriben directo en el OutputStream. Si el
        // error salta a mitad de camino la respuesta ya se fue: no hay nada que
        // renderizar y devolver una vista solo agregaria una segunda excepcion.
        if (response.isCommitted()) {
            return null;
        }

        if (esLlamadaApi(request)) {
            return ResponseEntity.status(estado)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", estado.value(), "titulo", titulo, "mensaje", detalle == null ? "" : detalle));
        }

        ModelAndView vista = new ModelAndView("error", estado);
        vista.addObject("status", estado.value());
        vista.addObject("titulo", titulo);
        vista.addObject("detalle", detalle);
        return vista;
    }

    private boolean esLlamadaApi(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        String accept = request.getHeader("Accept");
        return ruta.startsWith("/admin/api/")
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE) && !accept.contains(MediaType.TEXT_HTML_VALUE));
    }
}
