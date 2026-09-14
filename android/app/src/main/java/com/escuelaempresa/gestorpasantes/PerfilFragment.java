package com.escuelaempresa.gestorpasantes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

// Pantalla que consulta al servidor y muestra el resultado: cumple el requisito
// minimo de la consigna de "al menos una funcionalidad que consulte al servidor y
// presente los resultados correctamente en la aplicacion".
public class PerfilFragment extends Fragment {

    private SwipeRefreshLayout refrescar;
    private TextView textoNombre;
    private TextView textoCursoSeccion;
    private View filaEspecialidad;
    private View filaEmpresa;
    private View filaSupervisor;
    private View filaTelefono;
    private View filaEmail;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        refrescar = view.findViewById(R.id.refrescarPerfil);
        textoNombre = view.findViewById(R.id.textoNombre);
        textoCursoSeccion = view.findViewById(R.id.textoCursoSeccion);
        filaEspecialidad = view.findViewById(R.id.filaEspecialidad);
        filaEmpresa = view.findViewById(R.id.filaEmpresa);
        filaSupervisor = view.findViewById(R.id.filaSupervisor);
        filaTelefono = view.findViewById(R.id.filaTelefono);
        filaEmail = view.findViewById(R.id.filaEmail);

        etiquetar(filaEspecialidad, getString(R.string.etiqueta_especialidad));
        etiquetar(filaEmpresa, getString(R.string.etiqueta_empresa));
        etiquetar(filaSupervisor, getString(R.string.etiqueta_supervisor));
        etiquetar(filaTelefono, getString(R.string.etiqueta_telefono));
        etiquetar(filaEmail, getString(R.string.etiqueta_email));

        MaterialButton botonCerrarSesion = view.findViewById(R.id.botonCerrarSesion);
        botonCerrarSesion.setOnClickListener(v -> cerrarSesion());

        MaterialButton botonCambiarContrasena = view.findViewById(R.id.botonCambiarContrasena);
        botonCambiarContrasena.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CambiarContrasenaActivity.class)));

        MaterialButton botonFormularios = view.findViewById(R.id.botonFormularios);
        botonFormularios.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FormulariosActivity.class)));

        refrescar.setOnRefreshListener(this::cargarPerfil);
        cargarPerfil();
    }

    private void etiquetar(View fila, String texto) {
        TextView etiqueta = fila.findViewById(R.id.etiqueta);
        etiqueta.setText(texto);
    }

    private void cargarPerfil() {
        refrescar.setRefreshing(true);
        String token = sessionManager.obtenerToken();

        ApiJsonRequest pedido = new ApiJsonRequest(
                Request.Method.GET,
                ApiConfig.BASE_URL + "/perfil",
                null,
                token,
                this::onPerfilCargado,
                this::onErrorCarga);

        VolleySingleton.getInstancia(requireContext()).getRequestQueue().add(pedido);
    }

    private void onPerfilCargado(JSONObject json) {
        refrescar.setRefreshing(false);
        try {
            PerfilAlumno perfil = PerfilAlumno.desdeJson(json);
            textoNombre.setText(perfil.nombreCompleto());
            textoCursoSeccion.setText(perfil.curso + " " + perfil.seccion);
            valor(filaEspecialidad, mostrarOSinAsignar(perfil.especialidad));
            valor(filaEmpresa, mostrarOSinAsignar(perfil.empresa));
            valor(filaSupervisor, mostrarOSinAsignar(perfil.supervisor));
            valor(filaTelefono, perfil.telefono);
            valor(filaEmail, perfil.email);
        } catch (Exception e) {
            mostrarError(getString(R.string.error_red));
        }
    }

    private String mostrarOSinAsignar(String valor) {
        return (valor == null || valor.isEmpty()) ? getString(R.string.sin_asignar) : valor;
    }

    private void valor(View fila, String texto) {
        TextView valor = fila.findViewById(R.id.valor);
        valor.setText(texto);
    }

    private void onErrorCarga(VolleyError error) {
        refrescar.setRefreshing(false);
        if (esNoAutenticado(error)) {
            cerrarSesion();
            return;
        }
        mostrarError(getString(R.string.error_red));
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

    private void mostrarError(String mensaje) {
        if (getView() != null) {
            Snackbar.make(getView(), mensaje, Snackbar.LENGTH_LONG).show();
        }
    }
}
