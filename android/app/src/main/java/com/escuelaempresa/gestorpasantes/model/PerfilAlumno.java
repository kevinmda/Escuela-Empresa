package com.escuelaempresa.gestorpasantes.model;

import org.json.JSONException;
import org.json.JSONObject;

public class PerfilAlumno {

    public final int idAl;
    public final String nombres;
    public final String apellidos;
    public final String email;
    public final String telefono;
    public final String curso;
    public final String seccion;
    public final String especialidad;
    public final String empresa;
    public final String supervisor;

    private PerfilAlumno(int idAl, String nombres, String apellidos, String email, String telefono,
                          String curso, String seccion, String especialidad, String empresa, String supervisor) {
        this.idAl = idAl;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.curso = curso;
        this.seccion = seccion;
        this.especialidad = especialidad;
        this.empresa = empresa;
        this.supervisor = supervisor;
    }

    public static PerfilAlumno desdeJson(JSONObject json) throws JSONException {
        return new PerfilAlumno(
                json.getInt("idAl"),
                json.optString("nombres", ""),
                json.optString("apellidos", ""),
                json.optString("email", ""),
                json.optString("telefono", ""),
                json.optString("curso", ""),
                json.optString("seccion", ""),
                json.isNull("especialidad") ? null : json.optString("especialidad"),
                json.isNull("empresa") ? null : json.optString("empresa"),
                json.isNull("supervisor") ? null : json.optString("supervisor"));
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
