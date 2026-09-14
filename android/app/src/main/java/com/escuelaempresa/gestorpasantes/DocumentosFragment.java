package com.escuelaempresa.gestorpasantes;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

public class DocumentosFragment extends Fragment implements DocumentoAdapter.Escucha {

    private static final int TAMANIO_PAGINA = 10;

    private SwipeRefreshLayout refrescar;
    private RecyclerView listaDocumentos;
    private MaterialButton botonCargarMas;
    private View textoVacio;

    private DocumentoAdapter adapter;
    private SessionManager sessionManager;

    private int paginaActual = 0;
    private boolean quedanMasPaginas = false;

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
        refrescar.setRefreshing(true);
        String token = sessionManager.obtenerToken();
        String url = ApiConfig.BASE_URL + "/documentos?page=" + pagina + "&size=" + TAMANIO_PAGINA;

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

    private void onError(VolleyError error) {
        refrescar.setRefreshing(false);
        mostrarError(getString(R.string.error_red));
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
            File carpeta = new File(requireContext().getCacheDir(), "documentos");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            File archivo = new File(carpeta, "documento_" + documento.idDs + ".pdf");
            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                salida.write(bytes);
            }

            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", archivo);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
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
