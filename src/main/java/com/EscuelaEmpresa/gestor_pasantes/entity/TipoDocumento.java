package com.EscuelaEmpresa.gestor_pasantes.entity;

public enum TipoDocumento {
    CONTRATO("Contrato de Pasantía"),
    PLANTILLA_SEMANAL("Plantilla Semanal"),
    AUTORIZACION("Autorización"),
    FICHA_FINAL_ALUMNO("Ficha Final del Alumno"),
    FICHA_FINAL_EVALUATIVA("Ficha Final Evaluativa");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
