package com.escuelaempresa.gestorpasantes.network;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.HashMap;
import java.util.Map;

// Para descargar el PDF de un documento: la respuesta no es JSON, son los bytes
// crudos del archivo.
public class ApiBytesRequest extends Request<byte[]> {

    private final String token;
    private final Response.Listener<byte[]> listener;

    public ApiBytesRequest(String url, String token, Response.Listener<byte[]> listener,
                            Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.token = token;
        this.listener = listener;
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
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
        listener.onResponse(response);
    }
}
