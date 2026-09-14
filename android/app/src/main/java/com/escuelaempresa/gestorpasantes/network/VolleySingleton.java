package com.escuelaempresa.gestorpasantes.network;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

// Una sola RequestQueue para toda la app: crear una por pantalla desperdicia
// conexiones y threads sin necesidad.
public final class VolleySingleton {

    private static VolleySingleton instancia;
    private final RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized VolleySingleton getInstancia(Context context) {
        if (instancia == null) {
            instancia = new VolleySingleton(context);
        }
        return instancia;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }
}
