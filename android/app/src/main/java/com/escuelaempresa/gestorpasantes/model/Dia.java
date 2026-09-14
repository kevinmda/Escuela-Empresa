package com.escuelaempresa.gestorpasantes.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

// Un dia de la planilla semanal (Lunes..Sabado). Espeja dto/DiaForm.java del backend:
// si se completa uno de los tres campos, hay que completar los tres (la validacion
// real vive del lado del servidor; ver PlanillaDetalleActivity para el chequeo rapido
// en el cliente).
public class Dia {

    public final String nombreDia;
    public String fecha;        // yyyy-MM-dd, o null si el dia quedo vacio
    public String descripcion;
    public String horas;        // como texto, tal cual lo escribe el alumno

    public Dia(String nombreDia) {
        this.nombreDia = nombreDia;
    }

    public boolean estaVacio() {
        return (fecha == null || fecha.isEmpty())
                && (descripcion == null || descripcion.isEmpty())
                && (horas == null || horas.isEmpty());
    }

    public boolean estaCompleto() {
        return fecha != null && !fecha.isEmpty()
                && descripcion != null && !descripcion.isEmpty()
                && horas != null && !horas.isEmpty();
    }

    public JSONObject aJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("nombreDia", nombreDia);
        json.put("fecha", (fecha == null || fecha.isEmpty()) ? JSONObject.NULL : fecha);
        json.put("descripcion", (descripcion == null || descripcion.isEmpty()) ? JSONObject.NULL : descripcion);
        json.put("horas", (horas == null || horas.isEmpty()) ? JSONObject.NULL : new BigDecimal(horas));
        return json;
    }

    public static Dia desdeJson(JSONObject json) throws JSONException {
        Dia dia = new Dia(json.optString("nombreDia", ""));
        dia.fecha = json.isNull("fecha") ? null : json.optString("fecha");
        dia.descripcion = json.isNull("descripcion") ? null : json.optString("descripcion");
        dia.horas = json.isNull("horas") ? null : json.opt("horas").toString();
        return dia;
    }
}
