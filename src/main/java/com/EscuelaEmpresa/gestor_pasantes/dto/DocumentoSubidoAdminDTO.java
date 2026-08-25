package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.time.LocalDateTime;

public class DocumentoSubidoAdminDTO {

    private final Integer idDs;
    private final String nombreArchivo;
    private final LocalDateTime fechaSubida;
    private final String url;

    public DocumentoSubidoAdminDTO(Integer idDs, String nombreArchivo, LocalDateTime fechaSubida, String url) {
        this.idDs = idDs;
        this.nombreArchivo = nombreArchivo;
        this.fechaSubida = fechaSubida;
        this.url = url;
    }

    public Integer getIdDs() {
        return idDs;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public String getUrl() {
        return url;
    }
}
