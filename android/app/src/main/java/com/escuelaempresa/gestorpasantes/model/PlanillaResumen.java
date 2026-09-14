package com.escuelaempresa.gestorpasantes.model;

import org.json.JSONException;
import org.json.JSONObject;

public class PlanillaResumen {

    public final int idPs;
    public final String fechaDesde;
    public final String fechaHasta;
    public final String totalHoras;

    private PlanillaResumen(int idPs, String fechaDesde, String fechaHasta, String totalHoras) {
        this.idPs = idPs;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.totalHoras = totalHoras;
    }

    public static PlanillaResumen desdeJson(JSONObject json) throws JSONException {
        return new PlanillaResumen(
                json.getInt("idPs"),
                json.optString("fechaDesde", ""),
                json.optString("fechaHasta", ""),
                json.optString("totalHoras", "0"));
    }
}
