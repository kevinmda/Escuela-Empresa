package com.escuelaempresa.gestorpasantes.network;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Para endpoints POST que reciben un cuerpo JSON pero responden 204 sin cuerpo
// (ej: cambiar contraseña). JsonObjectRequest fallaria al parsear una respuesta
// vacia, por eso -- igual que ApiDeleteRequest -- esto usa StringRequest y solo
// le interesa si el pedido tuvo exito.
public class ApiPostAccionRequest extends StringRequest {

    private final JSONObject cuerpo;
    private final String token;

    public ApiPostAccionRequest(String url, JSONObject cuerpo, @Nullable String token,
                                 Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, listener, errorListener);
        this.cuerpo = cuerpo;
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

    @Override
    public String getBodyContentType() {
        return "application/json; charset=utf-8";
    }

    @Override
    public byte[] getBody() {
        return cuerpo.toString().getBytes(StandardCharsets.UTF_8);
    }
}
