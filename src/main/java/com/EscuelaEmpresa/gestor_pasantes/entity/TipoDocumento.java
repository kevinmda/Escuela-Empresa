package com.EscuelaEmpresa.gestor_pasantes.entity;

public enum TipoDocumento {
    CONTRATO("Contrato de Pasantía"),
    AUTORIZACION("Autorización"),
    PLANTILLA_SEMANAL("Planilla Semanal"),
    FICHA_FINAL_ALUMNO("Ficha Final del Alumno"),
    FICHA_FINAL_EVALUATIVA("Ficha Final Evaluativa"),
    // El expediente completo (autorización + contrato + planillas + las dos fichas,
    // ya combinado por el sistema en /alumno/imprimir) firmado/sellado y subido de
    // vuelta. No cuenta para el total de 10 comprobantes: solo existe una vez que
    // esos 10 ya están completos.
    DOCUMENTOS_ADJUNTOS("Documentos Adjuntos"),
    // El currículum del alumno. No es un comprobante de la pasantía: queda afuera
    // de los 10 del expediente (igual que DOCUMENTOS_ADJUNTOS), no se pide para
    // habilitar nada y lo pueden ver el coordinador y el administrativo en su
    // panel. Se guarda como texto en documento_scan.tipo_documento, así que
    // agregarlo no cambia la estructura de la base.
    CURRICULUM("Currículum"),
    // El informe final del alumno, en PDF (lo completa a partir de la plantilla
    // Word de /alumno/documentos/informe). Se sube en Documentos adjuntos, no en
    // Subir. No cuenta para los 10 comprobantes, pero sin él no se habilita el
    // expediente combinado, y va al final de ese PDF (ver ExpedientePdfService).
    INFORME_FINAL("Informe final");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
