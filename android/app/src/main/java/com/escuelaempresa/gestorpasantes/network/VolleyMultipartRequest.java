package com.escuelaempresa.gestorpasantes.network;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Volley no soporta multipart/form-data nativo (solo JSON o texto plano). Esta clase
// arma el cuerpo del pedido a mano -- boundary + partes -- para poder subir un
// archivo junto con un campo de texto, como pide POST /api/movil/documentos.
public abstract class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final String boundary = "apiClientBoundary" + System.currentTimeMillis();
    private final Response.Listener<NetworkResponse> listener;
    private final String token;

    public VolleyMultipartRequest(String url, String token,
                                   Response.Listener<NetworkResponse> listener,
                                   Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        this.listener = listener;
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
        return "multipart/form-data;boundary=" + boundary;
    }

    // Campos de texto del formulario (ej. "tipoDocumento").
    protected Map<String, String> getStringParams() {
        return new HashMap<>();
    }

    // El archivo a subir: nombre del campo del formulario -> datos del archivo.
    protected abstract Map<String, DatosArchivo> getFileParams();

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            for (Map.Entry<String, String> entry : getStringParams().entrySet()) {
                escribirCampoTexto(salida, entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, DatosArchivo> entry : getFileParams().entrySet()) {
                escribirCampoArchivo(salida, entry.getKey(), entry.getValue());
            }
            salida.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AuthFailureError("No se pudo armar el cuerpo del pedido");
        }
        return salida.toByteArray();
    }

    private void escribirCampoTexto(ByteArrayOutputStream salida, String nombre, String valor) throws IOException {
        salida.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        salida.write(("Content-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        salida.write(valor.getBytes(StandardCharsets.UTF_8));
        salida.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void escribirCampoArchivo(ByteArrayOutputStream salida, String nombre, DatosArchivo archivo) throws IOException {
        salida.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        salida.write(("Content-Disposition: form-data; name=\"" + nombre + "\"; filename=\""
                + archivo.nombreArchivo + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        salida.write(("Content-Type: " + archivo.tipoContenido + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        salida.write(archivo.contenido);
        salida.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        listener.onResponse(response);
    }

    public static class DatosArchivo {
        public final String nombreArchivo;
        public final String tipoContenido;
        public final byte[] contenido;

        public DatosArchivo(String nombreArchivo, String tipoContenido, byte[] contenido) {
            this.nombreArchivo = nombreArchivo;
            this.tipoContenido = tipoContenido;
            this.contenido = contenido;
        }
    }
}
