package com.escuelaempresa.gestorpasantes;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.adapter.DocumentoAdapter;
import com.escuelaempresa.gestorpasantes.model.Documento;
import com.escuelaempresa.gestorpasantes.network.ApiBytesRequest;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.ApiDeleteRequest;
import com.escuelaempresa.gestorpasantes.network.ApiJsonRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.escuelaempresa.gestorpasantes.util.VisorArchivos;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentosFragment extends Fragment implements DocumentoAdapter.Escucha {

    private static final int TAMANIO_PAGINA = 10;

    private SwipeRefreshLayout refrescar;
    private RecyclerView listaDocumentos;
    private MaterialButton botonCargarMas;
    private View textoVacio;
    private ShimmerFrameLayout shimmer;
    private View estadoError;

    private DocumentoAdapter adapter;
    private SessionManager sessionManager;

    private int paginaActual = 0;
    private boolean quedanMasPaginas = false;
    private boolean cargaInicialHecha = false;

    private ActivityResultLauncher<Intent> lanzadorSubida;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_documentos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        adapter = new DocumentoAdapter(this);

        refrescar = view.findViewById(R.id.refrescarDocumentos);
        listaDocumentos = view.findViewById(R.id.listaDocumentos);
        botonCargarMas = view.findViewById(R.id.botonCargarMasDocumentos);
        textoVacio = view.findViewById(R.id.textoDocumentosVacio);
        shimmer = view.findViewById(R.id.shimmerDocumentos);
        estadoError = view.findViewById(R.id.estadoErrorDocumentos);
        estadoError.<MaterialButton>findViewById(R.id.botonReintentar).setOnClickListener(v -> cargarPagina(0));

        int columnas = getResources().getInteger(R.integer.columnas_documentos);
        listaDocumentos.setLayoutManager(new GridLayoutManager(requireContext(), columnas));
        listaDocumentos.setAdapter(adapter);

        refrescar.setOnRefreshListener(() -> cargarPagina(0));
        botonCargarMas.setOnClickListener(v -> cargarPagina(paginaActual + 1));

        FloatingActionButton botonSubir = view.findViewById(R.id.botonSubirDocumento);
        AnimacionResorte.feedbackToque(botonSubir);
        lanzadorSubida = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() == android.app.Activity.RESULT_OK) {
                cargarPagina(0);
            }
        });
        botonSubir.setOnClickListener(v ->
                lanzadorSubida.launch(new Intent(requireContext(), SubirDocumentoActivity.class)));

        cargarPagina(0);
    }

    private void cargarPagina(int pagina) {
        boolean primeraCargaVisible = pagina == 0 && !cargaInicialHecha;
        if (primeraCargaVisible) {
            estadoError.setVisibility(View.GONE);
            textoVacio.setVisibility(View.GONE);
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
        } else {
            refrescar.setRefreshing(true);
        }

        String token = sessionManager.obtenerToken();
        String url = ApiConfig.BASE_URL + "/documentos?page=" + pagina + "&size=" + TAMANIO_PAGINA;

        ApiJsonRequest pedido = new ApiJsonRequest(
                Request.Method.GET, url, null, token,
                json -> onPaginaCargada(pagina, json),
                error -> onError(pagina, error));

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(pedido);
    }

    private void onPaginaCargada(int pagina, JSONObject json) {
        refrescar.setRefreshing(false);
        ocultarShimmer();
        cargaInicialHecha = true;
        estadoError.setVisibility(View.GONE);
        try {
            JSONArray contenido = json.getJSONArray("content");
            java.util.List<Documento> documentos = new java.util.ArrayList<>();
            for (int i = 0; i < contenido.length(); i++) {
                documentos.add(Documento.desdeJson(contenido.getJSONObject(i)));
            }

            if (pagina == 0) {
                adapter.reemplazarTodo(documentos);
            } else {
                adapter.agregarPagina(documentos);
            }
            paginaActual = pagina;
            quedanMasPaginas = !json.optBoolean("last", true);
            botonCargarMas.setVisibility(quedanMasPaginas ? View.VISIBLE : View.GONE);
            textoVacio.setVisibility(adapter.estaVacio() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    private void onError(int pagina, VolleyError error) {
        refrescar.setRefreshing(false);
        ocultarShimmer();
        if (pagina == 0 && !cargaInicialHecha) {
            textoVacio.setVisibility(View.GONE);
            estadoError.setVisibility(View.VISIBLE);
        } else {
            mostrarError(getString(R.string.error_red));
        }
    }

    private void ocultarShimmer() {
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    @Override
    public void alDescargar(Documento documento) {
        String token = sessionManager.obtenerToken();
        String url = ApiConfig.BASE_URL + "/documentos/" + documento.idDs + "/descargar";

        ApiBytesRequest pedido = new ApiBytesRequest(url, token,
                bytes -> abrirPdf(documento, bytes),
                error -> mostrarError(getString(R.string.error_red)));

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(pedido);
    }

    private void abrirPdf(Documento documento, byte[] bytes) {
        try {
            VisorArchivos.abrir(requireContext(), bytes, "documento_" + documento.idDs + ".pdf", "application/pdf");
        } catch (Exception e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    @Override
    public void alEliminar(Documento documento) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirmar_eliminar_documento)
                .setPositiveButton(android.R.string.ok, (dialogo, cual) -> eliminarDocumento(documento))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void eliminarDocumento(Documento documento) {
        String token = sessionManager.obtenerToken();
        String url = ApiConfig.BASE_URL + "/documentos/" + documento.idDs;

        ApiDeleteRequest pedido = new ApiDeleteRequest(url, token,
                respuesta -> {
                    adapter.quitar(documento);
                    textoVacio.setVisibility(adapter.estaVacio() ? View.VISIBLE : View.GONE);
                    mostrarError(getString(R.string.documento_eliminado_ok));
                },
                error -> mostrarError(getString(R.string.error_red)));

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(pedido);
    }

    private void mostrarError(String mensaje) {
        if (getView() != null) {
            Snackbar.make(getView(), mensaje, Snackbar.LENGTH_LONG).show();
        }
    }
}
