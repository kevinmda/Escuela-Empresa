package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiPostAccionRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

// Cubre dos casos con la misma pantalla: el cambio voluntario (se abre desde
// Perfil, EXTRA_FORZADO=false, "Atras" funciona normal) y el obligatorio que
// dispara LoginActivity cuando el alumno todavia tiene la contraseña por
// defecto (EXTRA_FORZADO=true: sin flecha de "Atras" ni boton fisico, para
// que no pueda saltearse el cambio).
public class CambiarContrasenaActivity extends AppCompatActivity {

    public static final String EXTRA_FORZADO = "forzado";

    private TextInputLayout campoActual;
    private TextInputLayout campoNueva;
    private TextInputLayout campoConfirmar;
    private TextInputEditText inputActual;
    private TextInputEditText inputNueva;
    private TextInputEditText inputConfirmar;
    private MaterialButton botonGuardar;
    private CircularProgressIndicator progreso;

    private SessionManager sessionManager;
    private boolean forzado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_contrasena);

        sessionManager = new SessionManager(this);
        forzado = getIntent().getBooleanExtra(EXTRA_FORZADO, false);

        Toolbar barra = findViewById(R.id.barraCambiarContrasena);
        setSupportActionBar(barra);
        if (!forzado) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            barra.setNavigationOnClickListener(v -> finish());
        } else {
            findViewById(R.id.textoAvisoForzado).setVisibility(View.VISIBLE);
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // no hace nada: el cambio es obligatorio, no hay a donde volver
                }
            });
        }

        campoActual = findViewById(R.id.campoActual);
        campoNueva = findViewById(R.id.campoNueva);
        campoConfirmar = findViewById(R.id.campoConfirmar);
        inputActual = findViewById(R.id.inputActual);
        inputNueva = findViewById(R.id.inputNueva);
        inputConfirmar = findViewById(R.id.inputConfirmar);
        botonGuardar = findViewById(R.id.botonGuardarContrasena);
        progreso = findViewById(R.id.progresoCambiarContrasena);

        AnimacionResorte.feedbackToque(botonGuardar);
        botonGuardar.setOnClickListener(v -> intentarGuardar());
    }

    private void intentarGuardar() {
        String actual = inputActual.getText().toString();
        String nueva = inputNueva.getText().toString();
        String confirmar = inputConfirmar.getText().toString();

        campoActual.setError(null);
        campoNueva.setError(null);
        campoConfirmar.setError(null);

        boolean valido = true;
        if (TextUtils.isEmpty(actual)) {
            campoActual.setError(getString(R.string.error_password_requerido));
            valido = false;
        }
        if (nueva.length() < 6) {
            campoNueva.setError(getString(R.string.error_contrasena_muy_corta));
            valido = false;
        }
        if (!nueva.equals(confirmar)) {
            campoConfirmar.setError(getString(R.string.error_confirmacion_no_coincide));
            valido = false;
        }
        if (!valido) {
            return;
        }

        mostrarCargando(true);

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("actual", actual);
            cuerpo.put("nueva", nueva);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ApiPostAccionRequest pedido = new ApiPostAccionRequest(
                ApiConfig.BASE_URL + "/auth/cambiar-contrasena",
                cuerpo,
                sessionManager.obtenerToken(),
                respuesta -> onGuardadoExitoso(),
                this::onGuardadoFallido);

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private void onGuardadoExitoso() {
        mostrarCargando(false);
        if (forzado) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Snackbar.make(botonGuardar, R.string.contrasena_cambiada_ok, Snackbar.LENGTH_SHORT).show();
            finish();
        }
    }

    private void onGuardadoFallido(VolleyError error) {
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
        Snackbar.make(botonGuardar, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonGuardar.setEnabled(!cargando);
    }
}
