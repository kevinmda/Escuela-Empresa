package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

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

// Primer paso de "olvidé mi contraseña": pide el email y le manda un codigo por
// correo (POST /auth/olvide-contrasena siempre responde igual, exista o no la
// cuenta, para no revelar que emails estan registrados). El segundo paso
// (codigo + contraseña nueva) es RestablecerContrasenaActivity.
public class RecuperarContrasenaActivity extends AppCompatActivity {

    private TextInputLayout campoEmail;
    private TextInputEditText inputEmail;
    private MaterialButton botonEnviar;
    private CircularProgressIndicator progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_contrasena);

        Toolbar barra = findViewById(R.id.barraRecuperar);
        setSupportActionBar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        barra.setNavigationOnClickListener(v -> finish());

        campoEmail = findViewById(R.id.campoEmail);
        inputEmail = findViewById(R.id.inputEmail);
        botonEnviar = findViewById(R.id.botonEnviarCodigo);
        progreso = findViewById(R.id.progresoRecuperar);

        AnimacionResorte.feedbackToque(botonEnviar);
        botonEnviar.setOnClickListener(v -> intentarEnviar());
    }

    private void intentarEnviar() {
        String email = inputEmail.getText().toString().trim();
        campoEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            campoEmail.setError(getString(R.string.error_email_requerido));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError(getString(R.string.error_email_invalido));
            return;
        }

        mostrarCargando(true);

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("email", email);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ApiPostAccionRequest pedido = new ApiPostAccionRequest(
                ApiConfig.BASE_URL + "/auth/olvide-contrasena",
                cuerpo,
                null,
                respuesta -> onCodigoEnviado(email),
                this::onEnvioFallido);

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private void onCodigoEnviado(String email) {
        mostrarCargando(false);
        Intent intent = new Intent(this, RestablecerContrasenaActivity.class);
        intent.putExtra(RestablecerContrasenaActivity.EXTRA_EMAIL, email);
        startActivity(intent);
        Snackbar.make(botonEnviar, R.string.aviso_codigo_enviado, Snackbar.LENGTH_LONG).show();
    }

    private void onEnvioFallido(VolleyError error) {
        mostrarCargando(false);
        mostrarError(getString(R.string.error_red));
    }

    private void mostrarError(String mensaje) {
        Snackbar.make(botonEnviar, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonEnviar.setEnabled(!cargando);
    }
}
