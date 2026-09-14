package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiPostAccionRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

// Segundo paso de "olvidé mi contraseña": el codigo que llego por email mas la
// contraseña nueva. El email viene de RecuperarContrasenaActivity, que ya lo
// mando al servidor en el primer paso.
public class RestablecerContrasenaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    private TextInputLayout campoCodigo;
    private TextInputLayout campoNueva;
    private TextInputLayout campoConfirmar;
    private TextInputEditText inputCodigo;
    private TextInputEditText inputNueva;
    private TextInputEditText inputConfirmar;
    private MaterialButton botonRestablecer;
    private CircularProgressIndicator progreso;

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restablecer_contrasena);

        email = getIntent().getStringExtra(EXTRA_EMAIL);

        Toolbar barra = findViewById(R.id.barraRestablecer);
        setSupportActionBar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        barra.setNavigationOnClickListener(v -> finish());

        campoCodigo = findViewById(R.id.campoCodigo);
        campoNueva = findViewById(R.id.campoNueva);
        campoConfirmar = findViewById(R.id.campoConfirmar);
        inputCodigo = findViewById(R.id.inputCodigo);
        inputNueva = findViewById(R.id.inputNueva);
        inputConfirmar = findViewById(R.id.inputConfirmar);
        botonRestablecer = findViewById(R.id.botonRestablecer);
        progreso = findViewById(R.id.progresoRestablecer);

        AnimacionResorte.feedbackToque(botonRestablecer);
        botonRestablecer.setOnClickListener(v -> intentarRestablecer());
    }

    private void intentarRestablecer() {
        String codigo = inputCodigo.getText().toString().trim();
        String nueva = inputNueva.getText().toString();
        String confirmar = inputConfirmar.getText().toString();

        campoCodigo.setError(null);
        campoNueva.setError(null);
        campoConfirmar.setError(null);

        boolean valido = true;
        if (TextUtils.isEmpty(codigo)) {
            campoCodigo.setError(getString(R.string.error_password_requerido));
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
            cuerpo.put("email", email);
            cuerpo.put("codigo", codigo);
            cuerpo.put("nuevaContrasena", nueva);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ApiPostAccionRequest pedido = new ApiPostAccionRequest(
                ApiConfig.BASE_URL + "/auth/restablecer-contrasena",
                cuerpo,
                null,
                respuesta -> onRestablecidaExitosa(),
                this::onRestablecerFallido);

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private void onRestablecidaExitosa() {
        mostrarCargando(false);
        Toast.makeText(this, R.string.contrasena_restablecida_ok, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void onRestablecerFallido(VolleyError error) {
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
        Snackbar.make(botonRestablecer, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonRestablecer.setEnabled(!cargando);
    }
}
