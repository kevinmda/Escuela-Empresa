package com.escuelaempresa.gestorpasantes;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navegacion = findViewById(R.id.navegacionInferior);

        if (savedInstanceState == null) {
            mostrarFragmento(new PerfilFragment());
        }

        navegacion.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_perfil) {
                mostrarFragmento(new PerfilFragment());
                return true;
            } else if (id == R.id.nav_documentos) {
                mostrarFragmento(new DocumentosFragment());
                return true;
            } else if (id == R.id.nav_planilla) {
                mostrarFragmento(new PlanillaFragment());
                return true;
            }
            return false;
        });
    }

    private void mostrarFragmento(Fragment fragmento) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedorFragmentos, fragmento)
                .commit();
    }
}
