package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.util.List;

import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;

/**
 * Resumen de entregas de UN tipo de documento para un alumno puntual.
 *
 * Reemplaza al viejo indicador booleano "entregó esta semana" por un
 * contador (subidos/limite) más la lista de documentos concretos (con
 * fecha exacta) que componen ese contador, para el menú desplegable de
 * Reportes.
 */
public class ResumenDocumentoTipoDTO {

    private final String codigo;
    private final String descripcion;
    private final int limite;
    private final long subidos;
    private final List<DocumentoSubidoAdminDTO> documentos;

    public ResumenDocumentoTipoDTO(TipoDocumento tipo, int limite, List<DocumentoSubidoAdminDTO> documentos) {
        this.codigo = tipo.name();
        this.descripcion = tipo.getDescripcion();
        this.limite = limite;
        this.documentos = documentos;
        this.subidos = documentos.size();
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getLimite() {
        return limite;
    }

    public long getSubidos() {
        return subidos;
    }

    public List<DocumentoSubidoAdminDTO> getDocumentos() {
        return documentos;
    }
}
