package com.EscuelaEmpresa.gestor_pasantes.controller.movil;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;

import jakarta.servlet.http.HttpServletRequest;

// Manejo de errores exclusivo de /api/movil/** (acotado por basePackages a este
// paquete). Ya existen dos @ControllerAdvice en controller/ (ManejoErroresGlobal y
// ManejadorDeErrores) manejando las mismas excepciones sin @Order entre ellos -- en
// vez de heredar esa ambiguedad, este advice es propio, con prioridad maxima, y
// responde siempre JSON con el mismo shape {status, titulo, mensaje} que ya usa
// ManejoErroresGlobal para sus llamadas API.
@RestControllerAdvice(basePackages = "com.EscuelaEmpresa.gestor_pasantes.controller.movil")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejoErroresMovil {

    private static final Logger log = LoggerFactory.getLogger(ManejoErroresMovil.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "No encontramos lo que buscás", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> accesoDenegado(AccessDeniedException e) {
        return responder(HttpStatus.FORBIDDEN, "No tenés permiso para esto", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Object> reglaNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.BAD_REQUEST, "No se pudo completar la acción", e.getMessage());
    }

    // La lanza el resolver de multipart antes de entrar al controlador.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> archivoDemasiadoGrande(MaxUploadSizeExceededException e) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo es demasiado grande",
                "El archivo supera el tamaño máximo permitido. Probá con un PDF más liviano.");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> cuentaInactiva(DisabledException e) {
        return responder(HttpStatus.FORBIDDEN, "Cuenta no activada", e.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Object> cuentaBloqueada(LockedException e) {
        return responder(HttpStatus.FORBIDDEN, "Cuenta bloqueada", e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> credencialesInvalidas(BadCredentialsException e) {
        return responder(HttpStatus.UNAUTHORIZED, "No se pudo iniciar sesión", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception e, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Algo salió mal",
                "Ocurrió un error inesperado. Si se repite, avisale a coordinación.");
    }

    private ResponseEntity<Object> responder(HttpStatus estado, String titulo, String detalle) {
        return ResponseEntity.status(estado)
                .body(Map.of("status", estado.value(), "titulo", titulo, "mensaje", detalle == null ? "" : detalle));
    }
}
