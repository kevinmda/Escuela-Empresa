package com.escuelaempresa.gestorpasantes.network;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

// Para un POST sin cuerpo (los datos van como query params en la propia URL,
// ej: /avisos/marcar-leido?codigo=X&clave=Y) que responde sin contenido.
// Mismo motivo que ApiPostAccionRequest para usar StringRequest en vez de
// JsonObjectRequest, pero sin body -- este endpoint no recibe uno.
public class ApiPostSimpleRequest extends StringRequest {

    private final String token;

    public ApiPostSimpleRequest(String url, @Nullable String token,
                                 Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, listener, errorListener);
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
