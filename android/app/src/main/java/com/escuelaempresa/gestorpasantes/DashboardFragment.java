package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.model.PerfilAlumno;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiJsonRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

// Pantalla de inicio: resume en 4 tarjetas lo que antes había que ir a buscar a
// tres pestañas distintas (perfil, planilla, documentos). No agrega ningún dato
// que las otras pantallas no tuvieran ya -- solo lo junta y lo cuenta.
public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout refrescar;
    private ShimmerFrameLayout shimmer;
    private View grilla;
    private View estadoError;
    private TextView textoSaludo;

    private MaterialCardView tarjetaSemanas;
    private MaterialCardView tarjetaHoras;
    private MaterialCardView tarjetaDocumentos;
    private MaterialCardView tarjetaEmpresa;

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

        tarjetaSemanas = view.findViewById(R.id.tarjetaSemanas);
        tarjetaHoras = view.findViewById(R.id.tarjetaHoras);
        tarjetaDocumentos = view.findViewById(R.id.tarjetaDocumentos);
        tarjetaEmpresa = view.findViewById(R.id.tarjetaEmpresa);

        prepararTarjeta(tarjetaSemanas, R.drawable.ic_planilla, getString(R.string.dashboard_semanas));
        prepararTarjeta(tarjetaHoras, R.drawable.ic_reloj, getString(R.string.dashboard_horas));
        prepararTarjeta(tarjetaDocumentos, R.drawable.ic_documentos, getString(R.string.dashboard_documentos));
        prepararTarjeta(tarjetaEmpresa, R.drawable.ic_maletin, getString(R.string.dashboard_empresa));

        estadoError.<MaterialButton>findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarTodo());
        refrescar.setOnRefreshListener(this::cargarTodo);

        cargarTodo();
    }

    private void prepararTarjeta(MaterialCardView tarjeta, int icono, String etiqueta) {
        ((ImageView) tarjeta.findViewById(R.id.iconoTarjeta)).setImageResource(icono);
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
    }

    private void onPerfilCargado(JSONObject json) {
        if (!isAdded()) {
            return;
        }
        try {
            PerfilAlumno perfil = PerfilAlumno.desdeJson(json);
            textoSaludo.setText(getString(R.string.dashboard_saludo, perfil.nombres));
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
            valorTarjeta(tarjetaHoras,
                    getString(R.string.dashboard_horas_valor, String.format(Locale.getDefault(), "%.2f", totalHoras)));
        } catch (Exception e) {
            planillasFallo = true;
            valorTarjeta(tarjetaSemanas, getString(R.string.dashboard_valor_error));
            valorTarjeta(tarjetaHoras, getString(R.string.dashboard_valor_error));
        }
        unPedidoMenos();
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
