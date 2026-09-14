package com.escuelaempresa.gestorpasantes.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Documento {

    public final int idDs;
    public final String nombreArchivo;
    public final String tipoDocumento;
    public final String tipoDocumentoDescripcion;
    public final String fechaSubida;
    public final boolean validado;

    private Documento(int idDs, String nombreArchivo, String tipoDocumento,
                       String tipoDocumentoDescripcion, String fechaSubida, boolean validado) {
        this.idDs = idDs;
        this.nombreArchivo = nombreArchivo;
        this.tipoDocumento = tipoDocumento;
        this.tipoDocumentoDescripcion = tipoDocumentoDescripcion;
        this.fechaSubida = fechaSubida;
        this.validado = validado;
    }

    public static Documento desdeJson(JSONObject json) throws JSONException {
        return new Documento(
                json.getInt("idDs"),
                json.optString("nombreArchivo", ""),
                json.optString("tipoDocumento", ""),
                json.optString("tipoDocumentoDescripcion", ""),
                json.optString("fechaSubida", ""),
                json.optBoolean("validado", false));
    }
}
