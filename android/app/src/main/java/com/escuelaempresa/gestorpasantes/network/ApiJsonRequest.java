package com.escuelaempresa.gestorpasantes.network;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// JsonObjectRequest que agrega el header Authorization con el JWT en cada pedido
// (GET perfil/documentos/planillas, POST login).
public class ApiJsonRequest extends JsonObjectRequest {

    private final String token;

    public ApiJsonRequest(int method, String url, @Nullable JSONObject body, @Nullable String token,
                           Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
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
