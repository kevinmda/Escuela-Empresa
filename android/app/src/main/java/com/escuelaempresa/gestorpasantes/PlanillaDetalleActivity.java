package com.escuelaempresa.gestorpasantes;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.model.Dia;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiJsonRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Muestra una planilla ya guardada (solo lectura, EXTRA_ID_PS presente) o el
// formulario para crear una semana nueva (sin EXTRA_ID_PS). No hay modo "editar":
// PlanillaSemanalService.guardarPlanilla del backend siempre hace INSERT, nunca
// UPDATE -- una planilla ya guardada no se puede modificar, ni aca ni en la web.
public class PlanillaDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_ID_PS = "idPs";

    private static final String[] NOMBRES_DIA = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    private final List<FilaDia> filas = new ArrayList<>();

    private TextInputEditText inputSupervisor;
    private TextInputEditText inputConocimientos;
    private TextInputEditText inputExperiencia;
    private TextInputEditText inputAprendizaje;
    private TextView textoError;
    private MaterialButton botonGuardar;
    private ProgressBar progreso;

    private SessionManager sessionManager;
    private boolean soloLectura;
    private int idPs = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planilla_detalle);

        sessionManager = new SessionManager(this);
        idPs = getIntent().getIntExtra(EXTRA_ID_PS, -1);
        soloLectura = idPs != -1;

        Toolbar barra = findViewById(R.id.barraPlanilla);
        setSupportActionBar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        barra.setNavigationOnClickListener(v -> finish());

        inputSupervisor = findViewById(R.id.inputSupervisor);
        inputConocimientos = findViewById(R.id.inputConocimientos);
        inputExperiencia = findViewById(R.id.inputExperiencia);
        inputAprendizaje = findViewById(R.id.inputAprendizaje);
        textoError = findViewById(R.id.textoErrorPlanilla);
        botonGuardar = findViewById(R.id.botonGuardarPlanilla);
        progreso = findViewById(R.id.progresoPlanilla);
        AnimacionResorte.feedbackToque(botonGuardar);

        armarFilasDeDias();

        if (soloLectura) {
            botonGuardar.setVisibility(View.GONE);
            deshabilitarCampos();
            cargarDetalle();
        } else {
            botonGuardar.setOnClickListener(v -> intentarGuardar());
        }
    }

    // El layout portrait tiene un solo contenedor (contenedorDias); el de landscape
    // tiene dos (contenedorDiasIzquierda/Derecha) para mostrar los 6 dias en dos
    // columnas de tres. Se arma la fila con el mismo item_dia_planilla en cualquiera
    // de los dos casos, distribuyendo los dias segun que contenedores existan.
    private void armarFilasDeDias() {
        View contenedorUnico = findViewById(R.id.contenedorDias);
        View contenedorIzquierda = findViewById(R.id.contenedorDiasIzquierda);
        View contenedorDerecha = findViewById(R.id.contenedorDiasDerecha);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < NOMBRES_DIA.length; i++) {
            android.view.ViewGroup contenedorDestino;
            if (contenedorIzquierda != null && contenedorDerecha != null) {
                contenedorDestino = (android.view.ViewGroup) (i < 3 ? contenedorIzquierda : contenedorDerecha);
            } else {
                contenedorDestino = (android.view.ViewGroup) contenedorUnico;
            }

            View fila = inflater.inflate(R.layout.item_dia_planilla, contenedorDestino, false);
            contenedorDestino.addView(fila);

            FilaDia filaDia = new FilaDia();
            filaDia.nombreDia = NOMBRES_DIA[i];
            filaDia.textoNombreDia = fila.findViewById(R.id.textoNombreDia);
            filaDia.botonFecha = fila.findViewById(R.id.botonElegirFecha);
            filaDia.textoFecha = fila.findViewById(R.id.textoFechaElegida);
            filaDia.inputDescripcion = fila.findViewById(R.id.inputDescripcionDia);
            filaDia.inputHoras = fila.findViewById(R.id.inputHorasDia);

            filaDia.textoNombreDia.setText(filaDia.nombreDia);
            filaDia.botonFecha.setOnClickListener(v -> mostrarSelectorFecha(filaDia));

            filas.add(filaDia);
        }
    }

    private void mostrarSelectorFecha(FilaDia fila) {
        Calendar hoy = Calendar.getInstance();
        new DatePickerDialog(this, (vista, anio, mesIndice, dia) -> {
            fila.fechaIso = String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mesIndice + 1, dia);
            fila.textoFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mesIndice + 1, anio));
        }, hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void deshabilitarCampos() {
        for (FilaDia fila : filas) {
            fila.botonFecha.setEnabled(false);
            fila.inputDescripcion.setEnabled(false);
            fila.inputHoras.setEnabled(false);
        }
        inputSupervisor.setEnabled(false);
        inputConocimientos.setEnabled(false);
        inputExperiencia.setEnabled(false);
        inputAprendizaje.setEnabled(false);
    }

    private void cargarDetalle() {
        mostrarCargando(true);
        String token = sessionManager.obtenerToken();

        ApiJsonRequest pedido = new ApiJsonRequest(
                Request.Method.GET,
                ApiConfig.BASE_URL + "/planillas/" + idPs,
                null, token,
                this::onDetalleCargado,
                error -> {
                    mostrarCargando(false);
                    mostrarErrorTexto(getString(R.string.error_red));
                });

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private void onDetalleCargado(JSONObject json) {
        mostrarCargando(false);
        try {
            inputSupervisor.setText(json.optString("supervisor", ""));
            inputConocimientos.setText(json.optString("conocimientos", ""));
            inputExperiencia.setText(json.optString("experiencia", ""));
            inputAprendizaje.setText(json.optString("aprendizaje", ""));

            JSONArray dias = json.getJSONArray("dias");
            for (int i = 0; i < dias.length(); i++) {
                Dia dia = Dia.desdeJson(dias.getJSONObject(i));
                FilaDia fila = buscarFilaPorNombre(dia.nombreDia);
                if (fila != null) {
                    fila.textoFecha.setText(formatearFechaVisible(dia.fecha));
                    fila.inputDescripcion.setText(dia.descripcion);
                    fila.inputHoras.setText(dia.horas);
                }
            }
        } catch (JSONException e) {
            mostrarErrorTexto(getString(R.string.error_red));
        }
    }

    private FilaDia buscarFilaPorNombre(String nombreDia) {
        for (FilaDia fila : filas) {
            if (fila.nombreDia.equalsIgnoreCase(nombreDia)) {
                return fila;
            }
        }
        return null;
    }

    private String formatearFechaVisible(String fechaIso) {
        if (fechaIso == null || fechaIso.length() != 10) {
            return "";
        }
        String[] partes = fechaIso.split("-");
        return partes[2] + "/" + partes[1] + "/" + partes[0];
    }

    private void intentarGuardar() {
        textoError.setVisibility(View.GONE);

        // Validacion rapida en el cliente, espejando PlanillaSemanalService: si un dia
        // tiene algun dato cargado, tiene que tener los tres. La validacion completa
        // (orden cronologico, superposicion con semanas existentes, limite de 6
        // semanas) la hace el servidor -- el mensaje de error que devuelva se muestra
        // tal cual en textoErrorPlanilla.
        boolean hayAlMenosUnDia = false;
        for (FilaDia fila : filas) {
            String descripcion = textoDe(fila.inputDescripcion);
            String horas = textoDe(fila.inputHoras);
            boolean tieneAlgo = fila.fechaIso != null || !descripcion.isEmpty() || !horas.isEmpty();
            boolean tieneTodo = fila.fechaIso != null && !descripcion.isEmpty() && !horas.isEmpty();

            if (tieneAlgo && !tieneTodo) {
                mostrarErrorTexto(getString(R.string.error_red_completa_el_dia, fila.nombreDia));
                return;
            }
            if (tieneTodo) {
                hayAlMenosUnDia = true;
            }
        }

        if (!hayAlMenosUnDia) {
            mostrarErrorTexto(getString(R.string.error_ningun_dia_cargado));
            return;
        }

        if (textoDe(inputSupervisor).isEmpty() || textoDe(inputConocimientos).isEmpty()
                || textoDe(inputExperiencia).isEmpty() || textoDe(inputAprendizaje).isEmpty()) {
            mostrarErrorTexto(getString(R.string.error_campos_obligatorios));
            return;
        }

        guardar();
    }

    private String textoDe(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void guardar() {
        mostrarCargando(true);

        try {
            JSONObject cuerpo = new JSONObject();
            JSONArray dias = new JSONArray();
            for (FilaDia fila : filas) {
                Dia dia = new Dia(fila.nombreDia);
                dia.fecha = fila.fechaIso;
                dia.descripcion = textoDe(fila.inputDescripcion);
                dia.horas = textoDe(fila.inputHoras);
                dias.put(dia.aJson());
            }
            cuerpo.put("dias", dias);
            cuerpo.put("supervisor", textoDe(inputSupervisor));
            cuerpo.put("conocimientos", textoDe(inputConocimientos));
            cuerpo.put("experiencia", textoDe(inputExperiencia));
            cuerpo.put("aprendizaje", textoDe(inputAprendizaje));

            String token = sessionManager.obtenerToken();
            ApiJsonRequest pedido = new ApiJsonRequest(
                    Request.Method.POST,
                    ApiConfig.BASE_URL + "/planillas",
                    cuerpo, token,
                    respuesta -> onGuardadoExitoso(),
                    this::onGuardadoFallido);

            VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
        } catch (JSONException e) {
            mostrarCargando(false);
            mostrarErrorTexto(getString(R.string.error_red));
        }
    }

    private void onGuardadoExitoso() {
        mostrarCargando(false);
        setResult(RESULT_OK);
        finish();
    }

    private void onGuardadoFallido(VolleyError error) {
        mostrarCargando(false);
        mostrarErrorTexto(extraerMensaje(error));
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

    private void mostrarErrorTexto(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonGuardar.setEnabled(!cargando);
    }

    private static class FilaDia {
        String nombreDia;
        String fechaIso;
        TextView textoNombreDia;
        Button botonFecha;
        TextView textoFecha;
        EditText inputDescripcion;
        EditText inputHoras;
    }
}
