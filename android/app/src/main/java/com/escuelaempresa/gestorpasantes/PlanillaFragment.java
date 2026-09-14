package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.adapter.PlanillaAdapter;
import com.escuelaempresa.gestorpasantes.model.PlanillaResumen;
import com.escuelaempresa.gestorpasantes.network.ApiBytesRequest;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiJsonRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.escuelaempresa.gestorpasantes.util.VisorArchivos;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlanillaFragment extends Fragment implements PlanillaAdapter.Escucha {

    private static final int TAMANIO_PAGINA = 10;

    private SwipeRefreshLayout refrescar;
    private MaterialButton botonCargarMas;
    private MaterialButton botonDescargarInforme;
    private View textoVacio;

    private PlanillaAdapter adapter;
    private SessionManager sessionManager;

    private int paginaActual = 0;

    private ActivityResultLauncher<Intent> lanzadorDetalle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planilla, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        adapter = new PlanillaAdapter(this);

        refrescar = view.findViewById(R.id.refrescarPlanillas);
        RecyclerView listaPlanillas = view.findViewById(R.id.listaPlanillas);
        botonCargarMas = view.findViewById(R.id.botonCargarMasPlanillas);
        botonDescargarInforme = view.findViewById(R.id.botonDescargarInforme);
        textoVacio = view.findViewById(R.id.textoPlanillasVacio);

        listaPlanillas.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaPlanillas.setAdapter(adapter);

        refrescar.setOnRefreshListener(() -> cargarPagina(0));
        botonCargarMas.setOnClickListener(v -> cargarPagina(paginaActual + 1));
        botonDescargarInforme.setOnClickListener(v -> descargarInforme());

        lanzadorDetalle = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() == android.app.Activity.RESULT_OK) {
                cargarPagina(0);
            }
        });

        FloatingActionButton botonNueva = view.findViewById(R.id.botonNuevaPlanilla);
        AnimacionResorte.feedbackToque(botonNueva);
        botonNueva.setOnClickListener(v ->
                lanzadorDetalle.launch(new Intent(requireContext(), PlanillaDetalleActivity.class)));

        cargarPagina(0);
    }

    private void cargarPagina(int pagina) {
        refrescar.setRefreshing(true);
        String token = sessionManager.obtenerToken();
        String url = ApiConfig.BASE_URL + "/planillas?page=" + pagina + "&size=" + TAMANIO_PAGINA;

        ApiJsonRequest pedido = new ApiJsonRequest(
                Request.Method.GET, url, null, token,
                json -> onPaginaCargada(pagina, json),
                this::onError);

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(pedido);
    }

    private void onPaginaCargada(int pagina, JSONObject json) {
        refrescar.setRefreshing(false);
        try {
            JSONArray contenido = json.getJSONArray("content");
            List<PlanillaResumen> planillas = new ArrayList<>();
            for (int i = 0; i < contenido.length(); i++) {
                planillas.add(PlanillaResumen.desdeJson(contenido.getJSONObject(i)));
            }

            if (pagina == 0) {
                adapter.reemplazarTodo(planillas);
            } else {
                adapter.agregarPagina(planillas);
            }
            paginaActual = pagina;
            boolean quedanMasPaginas = !json.optBoolean("last", true);
            botonCargarMas.setVisibility(quedanMasPaginas ? View.VISIBLE : View.GONE);
            textoVacio.setVisibility(adapter.estaVacio() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    private void onError(VolleyError error) {
        refrescar.setRefreshing(false);
        mostrarError(getString(R.string.error_red));
    }

    private void descargarInforme() {
        refrescar.setRefreshing(true);
        botonDescargarInforme.setEnabled(false);
        String token = sessionManager.obtenerToken();

        ApiBytesRequest pedido = new ApiBytesRequest(
                ApiConfig.BASE_URL + "/planillas/informe",
                token,
                this::onInformeDescargado,
                this::onInformeFallido);

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(pedido);
    }

    private void onInformeDescargado(byte[] bytes) {
        refrescar.setRefreshing(false);
        botonDescargarInforme.setEnabled(true);
        try {
            VisorArchivos.abrir(requireContext(), bytes, "Informe_Pasantia.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (Exception e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    private void onInformeFallido(VolleyError error) {
        refrescar.setRefreshing(false);
        botonDescargarInforme.setEnabled(true);
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

    @Override
    public void alTocar(PlanillaResumen planilla) {
        Intent intent = new Intent(requireContext(), PlanillaDetalleActivity.class);
        intent.putExtra(PlanillaDetalleActivity.EXTRA_ID_PS, planilla.idPs);
        lanzadorDetalle.launch(intent);
    }

    private void mostrarError(String mensaje) {
        if (getView() != null) {
            Snackbar.make(getView(), mensaje, Snackbar.LENGTH_LONG).show();
        }
    }
}
