package com.EscuelaEmpresa.gestor_pasantes.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Lo que pasa cuando algo se rompe.
 *
 * Sin esto, cada AccessDeniedException y cada RuntimeException("Alumno no
 * encontrado") de los controladores terminaba en la pantalla blanca de Whitelabel:
 * el usuario veía una tabla con la traza y ningún camino de vuelta, y en el
 * servidor no quedaba anotado nada que sirviera para entender qué había fallado.
 */
@ControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    /**
     * Pidió algo que no le corresponde: documentos de otra especialidad, la planilla
     * de otro alumno, la pantalla de Supervisores siendo administrativo.
     *
     * No se registra como error del sistema porque no lo es: es el control de acceso
     * haciendo su trabajo. Se anota en nivel warn porque un usuario que choca seguido
     * contra esto sí es algo para mirar.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String sinPermiso(AccessDeniedException excepcion, HttpServletRequest peticion) {
        log.warn("Acceso denegado a {} ({})", peticion.getRequestURI(), excepcion.getMessage());
        return "error/403";
    }

    /**
     * El archivo pasó el tope de spring.servlet.multipart.max-file-size. Este corte lo
     * hace Spring ANTES de llegar al controlador, así que la validación de tamaño que
     * vive en SubirController nunca llega a ejecutarse y el usuario veía un error 500
     * en lugar del mensaje que justamente explica que el archivo es muy grande.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande() {
        return "redirect:/alumno/subir?errorTamanio";
    }

    /**
     * Cualquier otra cosa. Se registra entera —con traza— porque acá sí hay algo roto
     * que alguien tiene que poder leer después; al usuario se le muestra solo que el
     * problema es nuestro.
     *
     * Los 404 no pasan por acá: los resuelve Spring por su cuenta contra
     * templates/error/404.html.
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String errorInesperado(RuntimeException excepcion, HttpServletRequest peticion) {
        log.error("Error sin manejar en {}", peticion.getRequestURI(), excepcion);
        return "error/500";
    }
}
