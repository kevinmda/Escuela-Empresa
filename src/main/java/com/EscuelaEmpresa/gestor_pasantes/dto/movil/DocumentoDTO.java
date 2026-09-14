package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

import java.time.LocalDateTime;

import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;

public class DocumentoDTO {

    private final Integer idDs;
    private final String nombreArchivo;
    private final String tipoDocumento;
    private final String tipoDocumentoDescripcion;
    private final LocalDateTime fechaSubida;
    private final Boolean validado;

    public DocumentoDTO(DocumentoSubido documento) {
        this.idDs = documento.getIdDs();
        this.nombreArchivo = documento.getNombreArchivo();
        this.tipoDocumento = documento.getTipoDocumento() != null ? documento.getTipoDocumento().name() : null;
        this.tipoDocumentoDescripcion = documento.getTipoDocumento() != null
                ? documento.getTipoDocumento().getDescripcion() : null;
        this.fechaSubida = documento.getFechaSubida();
        this.validado = documento.getValidado();
    }

    public Integer getIdDs() { return idDs; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getTipoDocumentoDescripcion() { return tipoDocumentoDescripcion; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public Boolean getValidado() { return validado; }
}
