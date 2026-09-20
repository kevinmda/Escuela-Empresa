package com.escuelaempresa.gestorpasantes.network;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

// Mismo header Authorization que ApiJsonRequest, pero para endpoints que
// devuelven un array JSON en vez de un objeto (como GET /api/movil/avisos).
public class ApiJsonArrayRequest extends JsonArrayRequest {

    private final String token;

    public ApiJsonArrayRequest(int method, String url, @Nullable JSONArray body, @Nullable String token,
                                Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, body, listener, errorListener);
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
