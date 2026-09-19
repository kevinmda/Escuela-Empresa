package com.EscuelaEmpresa.gestor_pasantes.entity;

public enum TipoDocumento {
    CONTRATO("Contrato de Pasantía"),
    PLANTILLA_SEMANAL("Planilla Semanal"),
    AUTORIZACION("Autorización"),
    FICHA_FINAL_ALUMNO("Ficha Final del Alumno"),
    FICHA_FINAL_EVALUATIVA("Ficha Final Evaluativa"),
    // El expediente completo (autorización + contrato + planillas + las dos fichas,
    // ya combinado por el sistema en /alumno/imprimir) firmado/sellado y subido de
    // vuelta. No cuenta para el total de 10 comprobantes: solo existe una vez que
    // esos 10 ya están completos.
    DOCUMENTOS_ADJUNTOS("Documentos Adjuntos");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
