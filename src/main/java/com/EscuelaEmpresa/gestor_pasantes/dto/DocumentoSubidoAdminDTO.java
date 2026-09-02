package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.time.LocalDateTime;

import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;

public class DocumentoSubidoAdminDTO {

    private final Integer idDs;
    private final String nombreArchivo;
    private final LocalDateTime fechaSubida;
    private final String url;
    private final TipoDocumento tipoDocumento;
    private final Boolean validado;

    public DocumentoSubidoAdminDTO(Integer idDs, String nombreArchivo, LocalDateTime fechaSubida, String url) {
        this(idDs, nombreArchivo, fechaSubida, url, null, false);
    }

    public DocumentoSubidoAdminDTO(Integer idDs, String nombreArchivo, LocalDateTime fechaSubida, String url,
                                    TipoDocumento tipoDocumento, Boolean validado) {
        this.idDs = idDs;
        this.nombreArchivo = nombreArchivo;
        this.fechaSubida = fechaSubida;
        this.url = url;
        this.tipoDocumento = tipoDocumento;
        this.validado = validado != null ? validado : false;
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

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public Boolean getValidado() {
        return validado;
    }
}
