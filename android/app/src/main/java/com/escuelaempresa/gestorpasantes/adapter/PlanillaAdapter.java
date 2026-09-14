package com.escuelaempresa.gestorpasantes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.escuelaempresa.gestorpasantes.R;
import com.escuelaempresa.gestorpasantes.model.PlanillaResumen;

import java.util.ArrayList;
import java.util.List;

public class PlanillaAdapter extends RecyclerView.Adapter<PlanillaAdapter.PlanillaViewHolder> {

    public interface Escucha {
        void alTocar(PlanillaResumen planilla);
    }

    private final List<PlanillaResumen> planillas = new ArrayList<>();
    private final Escucha escucha;

    public PlanillaAdapter(Escucha escucha) {
        this.escucha = escucha;
    }

    public void agregarPagina(List<PlanillaResumen> pagina) {
        int posicionInicial = planillas.size();
        planillas.addAll(pagina);
        notifyItemRangeInserted(posicionInicial, pagina.size());
    }

    public void reemplazarTodo(List<PlanillaResumen> nuevos) {
        planillas.clear();
        planillas.addAll(nuevos);
        notifyDataSetChanged();
    }

    public boolean estaVacio() {
        return planillas.isEmpty();
    }

    @NonNull
    @Override
    public PlanillaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_planilla, parent, false);
        return new PlanillaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanillaViewHolder holder, int position) {
        PlanillaResumen planilla = planillas.get(position);
        holder.rangoFechas.setText(planilla.fechaDesde + " — " + planilla.fechaHasta);
        holder.totalHoras.setText("Total: " + planilla.totalHoras + " hs");
        holder.itemView.setOnClickListener(v -> escucha.alTocar(planilla));
    }

    @Override
    public int getItemCount() {
        return planillas.size();
    }

    static class PlanillaViewHolder extends RecyclerView.ViewHolder {
        final TextView rangoFechas;
        final TextView totalHoras;

        PlanillaViewHolder(@NonNull View itemView) {
            super(itemView);
            rangoFechas = itemView.findViewById(R.id.textoRangoFechas);
            totalHoras = itemView.findViewById(R.id.textoTotalHoras);
        }
    }
}
