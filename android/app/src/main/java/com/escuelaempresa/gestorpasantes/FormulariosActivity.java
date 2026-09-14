package com.escuelaempresa.gestorpasantes;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiBytesRequest;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.VisorArchivos;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

// Lista fija de los 4 formularios oficiales prellenados con los datos del
// alumno (los mismos que ya se pueden imprimir desde la web en /alumno/imprimir).
// No hay adapter porque la lista nunca cambia: siempre son estos cuatro.
public class FormulariosActivity extends AppCompatActivity {

    private CircularProgressIndicator progreso;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formularios);

        sessionManager = new SessionManager(this);

        Toolbar barra = findViewById(R.id.barraFormularios);
        setSupportActionBar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        barra.setNavigationOnClickListener(v -> finish());

        progreso = findViewById(R.id.progresoFormularios);

        configurarFila(R.id.filaContrato, R.string.formulario_contrato, "contrato", "Contrato.pdf");
        configurarFila(R.id.filaAutorizacion, R.string.formulario_autorizacion, "autorizacion", "Autorizacion.pdf");
        configurarFila(R.id.filaFichaFinal, R.string.formulario_ficha_final, "ficha-final", "Ficha_Final.pdf");
        configurarFila(R.id.filaFichaFinalEvaluativa, R.string.formulario_ficha_final_evaluativa,
                "ficha-final-evaluativa", "Ficha_Final_Eval.pdf");
    }

    private void configurarFila(int idFila, int idTitulo, String endpoint, String nombreArchivo) {
        View fila = findViewById(idFila);
        TextView titulo = fila.findViewById(R.id.textoTituloFormulario);
        titulo.setText(idTitulo);
        fila.setOnClickListener(v -> descargar(endpoint, nombreArchivo));
    }

    private void descargar(String endpoint, String nombreArchivo) {
        mostrarCargando(true);
        String token = sessionManager.obtenerToken();

        ApiBytesRequest pedido = new ApiBytesRequest(
                ApiConfig.BASE_URL + "/formularios/" + endpoint,
                token,
                bytes -> onDescargado(bytes, nombreArchivo),
                this::onDescargaFallida);

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private void onDescargado(byte[] bytes, String nombreArchivo) {
        mostrarCargando(false);
        try {
            VisorArchivos.abrir(this, bytes, nombreArchivo, "application/pdf");
        } catch (Exception e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    private void onDescargaFallida(VolleyError error) {
        mostrarCargando(false);
        mostrarError(extraerMensaje(error));
    }

    private String extraerMensaje(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                JSONObject cuerpo = new JSONObject(new String(error.networkResponse.data, "UTF-8"));
                if (cuerpo.has("mensaje")) {
                    return cuerpo.getString("mensaje");
                }
            } catch (Exception ignorada) {
                // el body no era el JSON esperado -- se usa el mensaje generico de abajo
            }
        }
        return getString(R.string.error_red);
    }

    private void mostrarError(String mensaje) {
        Snackbar.make(progreso, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
    }
}
