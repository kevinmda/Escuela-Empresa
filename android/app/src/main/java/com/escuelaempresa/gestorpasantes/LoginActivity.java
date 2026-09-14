package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiJsonRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout campoEmail;
    private TextInputLayout campoPassword;
    private EditText inputEmail;
    private EditText inputPassword;
    private MaterialButton botonIngresar;
    private ProgressBar progresoLogin;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Si ya hay un token guardado, no tiene sentido pedir login de nuevo.
        if (sessionManager.haySesion()) {
            irAMain();
            return;
        }

        campoEmail = findViewById(R.id.campoEmail);
        campoPassword = findViewById(R.id.campoPassword);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        botonIngresar = findViewById(R.id.botonIngresar);
        progresoLogin = findViewById(R.id.progresoLogin);

        AnimacionResorte.feedbackToque(botonIngresar);
        botonIngresar.setOnClickListener(v -> intentarLogin());

        MaterialButton botonOlvideContrasena = findViewById(R.id.botonOlvideContrasena);
        botonOlvideContrasena.setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarContrasenaActivity.class)));
    }

    private void intentarLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        campoEmail.setError(null);
        campoPassword.setError(null);

        boolean valido = true;
        if (TextUtils.isEmpty(email)) {
            campoEmail.setError(getString(R.string.error_email_requerido));
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError(getString(R.string.error_email_invalido));
            valido = false;
        }
        if (TextUtils.isEmpty(password)) {
            campoPassword.setError(getString(R.string.error_password_requerido));
            valido = false;
        }
        if (!valido) {
            return;
        }

        mostrarCargando(true);

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("email", email);
            cuerpo.put("password", password);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ApiJsonRequest pedido = new ApiJsonRequest(
                Request.Method.POST,
                ApiConfig.BASE_URL + "/auth/login",
                cuerpo,
                null,
                this::onLoginExitoso,
                this::onLoginFallido);

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private void onLoginExitoso(JSONObject respuesta) {
        mostrarCargando(false);
        try {
            String token = respuesta.getString("token");
            sessionManager.guardarToken(token);

            // La API movil no pasa por ContrasenaPorDefectoInterceptor (ver WebConfig
            // en el backend): si la cuenta todavia tiene la contraseña compartida,
            // es la app la que tiene que mandar a cambiarla antes de dejar entrar.
            boolean contrasenaPorDefecto = respuesta.optJSONObject("alumno") != null
                    && respuesta.getJSONObject("alumno").optBoolean("contrasenaPorDefecto", false);

            if (contrasenaPorDefecto) {
                Intent intent = new Intent(this, CambiarContrasenaActivity.class);
                intent.putExtra(CambiarContrasenaActivity.EXTRA_FORZADO, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                irAMain();
            }
        } catch (JSONException e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    private void onLoginFallido(VolleyError error) {
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
        Snackbar.make(botonIngresar, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void mostrarCargando(boolean cargando) {
        progresoLogin.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonIngresar.setEnabled(!cargando);
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
