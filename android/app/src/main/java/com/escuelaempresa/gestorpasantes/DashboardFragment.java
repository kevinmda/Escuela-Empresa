package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.model.Aviso;
import com.escuelaempresa.gestorpasantes.model.PerfilAlumno;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiJsonArrayRequest;
import com.escuelaempresa.gestorpasantes.network.ApiJsonRequest;
import com.escuelaempresa.gestorpasantes.network.ApiPostSimpleRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

// Pantalla de inicio: resume en una tarjeta tipo "dossier" (mismo concepto que
// la credencial de alumno/index.html en la web) y 4 tarjetas lo que antes había
// que ir a buscar a tres pestañas distintas. No agrega ningún dato que las
// otras pantallas no tuvieran ya -- solo lo junta y lo cuenta.
public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout refrescar;
    private ShimmerFrameLayout shimmer;
    private View grilla;
    private View estadoError;
    private TextView textoSaludo;
    private TextView textoEspecialidadCurso;
    private TextView textoCedula;
    private TextView textoExpediente;

    private MaterialCardView tarjetaSemanas;
    private MaterialCardView tarjetaHoras;
    private MaterialCardView tarjetaDocumentos;
    private MaterialCardView tarjetaEmpresa;
    private LinearLayout contenedorAvisos;

    private SessionManager sessionManager;

    // Los 3 pedidos van en paralelo; solo cuando responden los 3 (con éxito o
    // error) se apaga el shimmer y se decide si se ve la grilla o el error.
    private int pedidosPendientes;
    private boolean perfilFallo;
    private boolean planillasFallo;
    private boolean documentosFallo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        refrescar = view.findViewById(R.id.refrescarDashboard);
        shimmer = view.findViewById(R.id.shimmerDashboard);
        grilla = view.findViewById(R.id.grillaDashboard);
        estadoError = view.findViewById(R.id.estadoErrorDashboard);
        textoSaludo = view.findViewById(R.id.textoSaludo);
        textoEspecialidadCurso = view.findViewById(R.id.textoEspecialidadCurso);
        textoCedula = view.findViewById(R.id.textoCedula);
        textoExpediente = view.findViewById(R.id.textoExpediente);

        tarjetaSemanas = view.findViewById(R.id.tarjetaSemanas);
        tarjetaHoras = view.findViewById(R.id.tarjetaHoras);
        tarjetaDocumentos = view.findViewById(R.id.tarjetaDocumentos);
        tarjetaEmpresa = view.findViewById(R.id.tarjetaEmpresa);
        contenedorAvisos = view.findViewById(R.id.contenedorAvisos);

        // Mismos 4 roles de color que usa la web para distinguir bloques: dorado
        // (acento de marca), verde (progreso/éxito), azul (info) y navy (marca
        // principal) -- no los 4 iguales, que es lo que hacía sentir todo plano.
        prepararTarjeta(tarjetaSemanas, R.drawable.ic_calendario, getString(R.string.dashboard_semanas),
                R.color.acento_contenedor, R.color.on_acento_contenedor);
        prepararTarjeta(tarjetaHoras, R.drawable.ic_reloj, getString(R.string.dashboard_horas),
                R.color.exito_contenedor, R.color.on_exito_contenedor);
        prepararTarjeta(tarjetaDocumentos, R.drawable.ic_escudo, getString(R.string.dashboard_documentos),
                R.color.secundario_contenedor, R.color.on_secundario_contenedor);
        prepararTarjeta(tarjetaEmpresa, R.drawable.ic_maletin, getString(R.string.dashboard_empresa),
                R.color.primario_contenedor, R.color.on_primario_contenedor);

        LinearProgressIndicator progresoSemanas = tarjetaSemanas.findViewById(R.id.progresoTarjeta);
        progresoSemanas.setMax(6);
        progresoSemanas.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.on_acento_contenedor));
        progresoSemanas.setTrackColor(ContextCompat.getColor(requireContext(), R.color.superficie));
        progresoSemanas.setVisibility(View.VISIBLE);

        estadoError.<MaterialButton>findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarTodo());
        refrescar.setOnRefreshListener(this::cargarTodo);

        cargarTodo();
    }

    private void prepararTarjeta(MaterialCardView tarjeta, int icono, String etiqueta,
                                  @ColorRes int colorContenedor, @ColorRes int colorOnContenedor) {
        ImageView vistaIcono = tarjeta.findViewById(R.id.iconoTarjeta);
        vistaIcono.setImageResource(icono);
        vistaIcono.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), colorContenedor));
        vistaIcono.setImageTintList(ContextCompat.getColorStateList(requireContext(), colorOnContenedor));
        ((TextView) tarjeta.findViewById(R.id.etiquetaTarjeta)).setText(etiqueta);
    }

    private void valorTarjeta(MaterialCardView tarjeta, String valor) {
        ((TextView) tarjeta.findViewById(R.id.valorTarjeta)).setText(valor);
    }

    private void cargarTodo() {
        pedidosPendientes = 3;
        perfilFallo = false;
        planillasFallo = false;
        documentosFallo = false;

        if (!refrescar.isRefreshing()) {
            estadoError.setVisibility(View.GONE);
            grilla.setVisibility(View.GONE);
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
        }

        String token = sessionManager.obtenerToken();

        VolleySingleton cola = VolleySingleton.getInstancia(requireContext());

        cola.getRequestQueue().add(new ApiJsonRequest(
                Request.Method.GET, ApiConfig.BASE_URL + "/perfil", null, token,
                this::onPerfilCargado, error -> onFallo(error, 1)));

        cola.getRequestQueue().add(new ApiJsonRequest(
                Request.Method.GET, ApiConfig.BASE_URL + "/planillas?page=0&size=10", null, token,
                this::onPlanillasCargadas, error -> onFallo(error, 2)));

        cola.getRequestQueue().add(new ApiJsonRequest(
                Request.Method.GET, ApiConfig.BASE_URL + "/documentos?page=0&size=10", null, token,
                this::onDocumentosCargados, error -> onFallo(error, 3)));

        // Aparte de los 3 de arriba, sin contar en pedidosPendientes: si esto
        // falla o tarda, no tiene sentido bloquear el resto de la pantalla por
        // un aviso que ni siquiera puede haber. Mismo criterio "no crítico"
        // que ya usa el resto de esta pantalla para separar lo esencial de lo
        // que puede fallar en silencio.
        cargarAvisos();
    }

    // Mismo aviso que la campana del header web (ver ChromeContext.Aviso /
    // AvisoService): sin push real, la app los pide acá al abrir Inicio, no
    // llegan solos con la app cerrada.
    private void cargarAvisos() {
        String token = sessionManager.obtenerToken();
        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(new ApiJsonArrayRequest(
                Request.Method.GET, ApiConfig.BASE_URL + "/avisos", null, token,
                this::onAvisosCargados, error -> { /* no crítico, se ignora */ }));
    }

    private void onAvisosCargados(JSONArray json) {
        if (!isAdded()) {
            return;
        }
        contenedorAvisos.removeAllViews();
        for (int i = 0; i < json.length(); i++) {
            try {
                Aviso aviso = Aviso.desdeJson(json.getJSONObject(i));
                if (!aviso.leido) {
                    agregarFilaAviso(aviso);
                }
            } catch (JSONException ignorada) {
                // una fila con formato inesperado no debería tirar abajo el resto
            }
        }
        contenedorAvisos.setVisibility(contenedorAvisos.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void agregarFilaAviso(Aviso aviso) {
        View fila = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_aviso, contenedorAvisos, false);
        ((TextView) fila.findViewById(R.id.textoAviso)).setText(aviso.texto);
        fila.findViewById(R.id.botonMarcarLeidoAviso).setOnClickListener(v -> marcarAvisoLeido(aviso, fila));
        contenedorAvisos.addView(fila);
    }

    // No desaparece "en el momento" ni espera confirmación del servidor para
    // sentirse instantáneo: se saca la fila apenas responde el pedido. Si
    // falla, la fila se queda como estaba y el alumno puede volver a tocar el
    // botón.
    private void marcarAvisoLeido(Aviso aviso, View fila) {
        String token = sessionManager.obtenerToken();
        String url = ApiConfig.BASE_URL + "/avisos/marcar-leido"
                + "?codigo=" + aviso.codigo + "&clave=" + aviso.clave;

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(new ApiPostSimpleRequest(
                url, token,
                respuesta -> {
                    if (!isAdded()) {
                        return;
                    }
                    contenedorAvisos.removeView(fila);
                    if (contenedorAvisos.getChildCount() == 0) {
                        contenedorAvisos.setVisibility(View.GONE);
                    }
                },
                error -> { /* la fila se queda, se puede reintentar */ }));
    }

    private void onPerfilCargado(JSONObject json) {
        if (!isAdded()) {
            return;
        }
        try {
            PerfilAlumno perfil = PerfilAlumno.desdeJson(json);
            textoSaludo.setText(perfil.nombreCompleto());

            String especialidad = (perfil.especialidad == null || perfil.especialidad.isEmpty())
                    ? getString(R.string.sin_asignar) : perfil.especialidad;
            textoEspecialidadCurso.setText(
                    especialidad + " · Curso " + perfil.curso + ", Sección " + perfil.seccion);

            textoCedula.setText(perfil.ci == null || perfil.ci.isEmpty()
                    ? getString(R.string.dashboard_valor_error) : perfil.ci);
            textoExpediente.setText(getString(R.string.dashboard_expediente_valor, perfil.idAl));

            String empresa = (perfil.empresa == null || perfil.empresa.isEmpty())
                    ? getString(R.string.sin_asignar) : perfil.empresa;
            valorTarjeta(tarjetaEmpresa, empresa);
        } catch (Exception e) {
            perfilFallo = true;
            valorTarjeta(tarjetaEmpresa, getString(R.string.dashboard_valor_error));
        }
        unPedidoMenos();
    }

    private void onPlanillasCargadas(JSONObject json) {
        if (!isAdded()) {
            return;
        }
        try {
            JSONArray contenido = json.getJSONArray("content");
            double totalHoras = 0;
            for (int i = 0; i < contenido.length(); i++) {
                totalHoras += contenido.getJSONObject(i).optDouble("totalHoras", 0);
            }
            int semanas = json.optInt("totalElements", contenido.length());
            valorTarjeta(tarjetaSemanas, getString(R.string.dashboard_semanas_valor, semanas));
            valorTarjeta(tarjetaHoras, getString(R.string.dashboard_horas_valor, formatearHoras(totalHoras)));

            LinearProgressIndicator progresoSemanas = tarjetaSemanas.findViewById(R.id.progresoTarjeta);
            progresoSemanas.setProgress(Math.min(semanas, 6));
        } catch (Exception e) {
            planillasFallo = true;
            valorTarjeta(tarjetaSemanas, getString(R.string.dashboard_valor_error));
            valorTarjeta(tarjetaHoras, getString(R.string.dashboard_valor_error));
        }
        unPedidoMenos();
    }

    // Sin decimales de sobra: 145 horas exactas muestra "145", no "145.00" --
    // pero 7.5 sigue mostrando "7.5", no se redondea a un entero.
    private String formatearHoras(double horas) {
        if (horas == Math.floor(horas)) {
            return String.format(Locale.getDefault(), "%.0f", horas);
        }
        String formateado = String.format(Locale.getDefault(), "%.2f", horas);
        if (formateado.endsWith("0")) {
            formateado = formateado.substring(0, formateado.length() - 1);
        }
        return formateado;
    }

    private void onDocumentosCargados(JSONObject json) {
        if (!isAdded()) {
            return;
        }
        try {
            int total = json.optInt("totalElements", 0);
            valorTarjeta(tarjetaDocumentos, String.valueOf(total));
        } catch (Exception e) {
            documentosFallo = true;
            valorTarjeta(tarjetaDocumentos, getString(R.string.dashboard_valor_error));
        }
        unPedidoMenos();
    }

    private void onFallo(VolleyError error, int cual) {
        if (!isAdded()) {
            return;
        }
        if (esNoAutenticado(error)) {
            cerrarSesion();
            return;
        }
        if (cual == 1) {
            perfilFallo = true;
            valorTarjeta(tarjetaEmpresa, getString(R.string.dashboard_valor_error));
        } else if (cual == 2) {
            planillasFallo = true;
            valorTarjeta(tarjetaSemanas, getString(R.string.dashboard_valor_error));
            valorTarjeta(tarjetaHoras, getString(R.string.dashboard_valor_error));
        } else {
            documentosFallo = true;
            valorTarjeta(tarjetaDocumentos, getString(R.string.dashboard_valor_error));
        }
        unPedidoMenos();
    }

    private boolean esNoAutenticado(VolleyError error) {
        return error.networkResponse != null && error.networkResponse.statusCode == 401;
    }

    private void cerrarSesion() {
        sessionManager.cerrarSesion();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void unPedidoMenos() {
        pedidosPendientes--;
        if (pedidosPendientes > 0) {
            return;
        }
        refrescar.setRefreshing(false);
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);

        // Si los 3 pedidos fallaron (típicamente sin conexión), no tiene
        // sentido mostrar una grilla llena de "–" -- se bloquea la pantalla
        // con un solo estado de error y un botón para reintentar todo junto.
        if (perfilFallo && planillasFallo && documentosFallo) {
            grilla.setVisibility(View.GONE);
            estadoError.setVisibility(View.VISIBLE);
        } else {
            estadoError.setVisibility(View.GONE);
            grilla.setVisibility(View.VISIBLE);
        }
    }
}
