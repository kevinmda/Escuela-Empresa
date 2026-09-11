package com.EscuelaEmpresa.gestor_pasantes.exception;

// Se lanza cuando se pide algo por id (un alumno, una planilla, un documento...) y no
// existe, o el archivo fisico ya no esta en el disco. ManejoErroresGlobal la convierte
// en una pagina 404 con el mensaje, en vez del error 500 generico que daba
// RuntimeException.
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
