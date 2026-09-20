package com.escuelaempresa.gestorpasantes.model;

import org.json.JSONException;
import org.json.JSONObject;

// Mismo aviso que la campana del header web (ver AvisoDTO del lado del
// servidor), sin "destino" ni "tipo": son cosas propias de esa pantalla web
// (una ruta que la app no tiene, y el color de la campana) que acá no hacen
// falta.
public class Aviso {

    public final String texto;
    public final String codigo;
    public final String clave;
    public final boolean leido;

    private Aviso(String texto, String codigo, String clave, boolean leido) {
        this.texto = texto;
        this.codigo = codigo;
        this.clave = clave;
        this.leido = leido;
    }

    public static Aviso desdeJson(JSONObject json) throws JSONException {
        return new Aviso(
                json.getString("texto"),
                json.getString("codigo"),
                json.getString("clave"),
                json.getBoolean("leido"));
    }
}
