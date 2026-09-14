package com.escuelaempresa.gestorpasantes;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.VolleyMultipartRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SubirDocumentoActivity extends AppCompatActivity {

    // Mismos codigos que el enum TipoDocumento del backend -- se muestra la
    // descripcion, se manda el codigo.
    private static final String[] CODIGOS_TIPO = {
            "CONTRATO", "PLANTILLA_SEMANAL", "AUTORIZACION", "FICHA_FINAL_ALUMNO", "FICHA_FINAL_EVALUATIVA"
    };
    private static final String[] DESCRIPCIONES_TIPO = {
            "Contrato de Pasantía", "Plantilla Semanal", "Autorización",
            "Ficha Final del Alumno", "Ficha Final Evaluativa"
    };

    private Spinner spinnerTipo;
    private TextView textoArchivoElegido;
    private MaterialButton botonSubir;
    private ProgressBar progreso;

    private SessionManager sessionManager;
    private Uri archivoElegido;
    private String nombreArchivoElegido;

    private ActivityResultLauncher<String[]> lanzadorSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subir_documento);

        sessionManager = new SessionManager(this);

        Toolbar barra = findViewById(R.id.barraSubir);
        setSupportActionBar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        barra.setNavigationOnClickListener(v -> finish());

        spinnerTipo = findViewById(R.id.spinnerTipoDocumento);
        textoArchivoElegido = findViewById(R.id.textoArchivoElegido);
        MaterialButton botonElegirArchivo = findViewById(R.id.botonElegirArchivo);
        botonSubir = findViewById(R.id.botonSubirDocumento);
        progreso = findViewById(R.id.progresoSubida);
        AnimacionResorte.feedbackToque(botonElegirArchivo);
        AnimacionResorte.feedbackToque(botonSubir);

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DESCRIPCIONES_TIPO);
        spinnerTipo.setAdapter(adaptador);

        lanzadorSelector = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                archivoElegido = uri;
                nombreArchivoElegido = obtenerNombreArchivo(uri);
                textoArchivoElegido.setText(nombreArchivoElegido);
            }
        });

        botonElegirArchivo.setOnClickListener(v -> lanzadorSelector.launch(new String[]{"application/pdf"}));
        botonSubir.setOnClickListener(v -> subir());
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = "documento.pdf";
        ContentResolver resolver = getContentResolver();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (indice >= 0) {
                    nombre = cursor.getString(indice);
                }
            }
        }
        return nombre;
    }

    private void subir() {
        if (archivoElegido == null) {
            mostrarError(getString(R.string.ningun_archivo_elegido));
            return;
        }

        int posicionTipo = spinnerTipo.getSelectedItemPosition();
        String codigoTipo = CODIGOS_TIPO[posicionTipo];

        byte[] contenido;
        try (InputStream entrada = getContentResolver().openInputStream(archivoElegido)) {
            contenido = leerBytes(entrada);
        } catch (IOException e) {
            mostrarError(getString(R.string.error_red));
            return;
        }

        mostrarCargando(true);
        String token = sessionManager.obtenerToken();

        VolleyMultipartRequest pedido = new VolleyMultipartRequest(
                ApiConfig.BASE_URL + "/documentos", token,
                this::onSubidaExitosa,
                this::onSubidaFallida) {

            @Override
            protected Map<String, String> getStringParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tipoDocumento", codigoTipo);
                return params;
            }

            @Override
            protected Map<String, DatosArchivo> getFileParams() {
                Map<String, DatosArchivo> archivos = new HashMap<>();
                archivos.put("archivo", new DatosArchivo(nombreArchivoElegido, "application/pdf", contenido));
                return archivos;
            }
        };

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private byte[] leerBytes(InputStream entrada) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            salida.write(buffer, 0, leidos);
        }
        return salida.toByteArray();
    }

    private void onSubidaExitosa(NetworkResponse respuesta) {
        mostrarCargando(false);
        Snackbar.make(botonSubir, R.string.documento_subido_ok, Snackbar.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void onSubidaFallida(VolleyError error) {
        mostrarCargando(false);
        mostrarError(extraerMensaje(error));
    }

    private String extraerMensaje(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                org.json.JSONObject cuerpo = new org.json.JSONObject(new String(error.networkResponse.data, "UTF-8"));
                if (cuerpo.has("mensaje")) {
                    return cuerpo.getString("mensaje");
                }
            } catch (Exception ignorada) {
                // el body no era el JSON esperado -- se usa el mensaje generico de abajo
            }
        }
        return getString(R.string.error_red);
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonSubir.setEnabled(!cargando);
    }

    private void mostrarError(String mensaje) {
        Snackbar.make(botonSubir, mensaje, Snackbar.LENGTH_LONG).show();
    }
}
