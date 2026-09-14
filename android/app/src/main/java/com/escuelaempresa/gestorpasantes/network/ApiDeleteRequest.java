package com.escuelaempresa.gestorpasantes.network;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

// Los DELETE del backend responden 204 sin cuerpo. JsonObjectRequest fallaria al
// intentar parsear un body vacio como JSON, por eso este usa StringRequest (que
// acepta un body vacio sin problema) y solo le interesa si el pedido tuvo exito.
public class ApiDeleteRequest extends StringRequest {

    private final String token;

    public ApiDeleteRequest(String url, String token,
                             Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Method.DELETE, url, listener, errorListener);
        this.token = token;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }
}
