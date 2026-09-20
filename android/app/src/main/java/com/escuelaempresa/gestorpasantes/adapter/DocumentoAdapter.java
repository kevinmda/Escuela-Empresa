package com.escuelaempresa.gestorpasantes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.escuelaempresa.gestorpasantes.R;
import com.escuelaempresa.gestorpasantes.model.Documento;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocumentoAdapter extends RecyclerView.Adapter<DocumentoAdapter.DocumentoViewHolder> {

    public interface Escucha {
        void alDescargar(Documento documento);
        void alEliminar(Documento documento);
    }

    private final List<Documento> documentos = new ArrayList<>();
    private final Set<Integer> posicionesYaAnimadas = new HashSet<>();
    private final Escucha escucha;

    public DocumentoAdapter(Escucha escucha) {
        this.escucha = escucha;
    }

    public void agregarPagina(List<Documento> pagina) {
        int posicionInicial = documentos.size();
        documentos.addAll(pagina);
        notifyItemRangeInserted(posicionInicial, pagina.size());
    }

    public void reemplazarTodo(List<Documento> nuevos) {
        documentos.clear();
        documentos.addAll(nuevos);
        posicionesYaAnimadas.clear();
        notifyDataSetChanged();
    }

    public void quitar(Documento documento) {
        int posicion = documentos.indexOf(documento);
        if (posicion >= 0) {
            documentos.remove(posicion);
            notifyItemRemoved(posicion);
        }
    }

    public boolean estaVacio() {
        return documentos.isEmpty();
    }

    @NonNull
    @Override
    public DocumentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_documento, parent, false);
        return new DocumentoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentoViewHolder holder, int position) {
        Documento documento = documentos.get(position);
        holder.tipo.setText(documento.tipoDocumentoDescripcion);
        holder.nombreArchivo.setText(documento.nombreArchivo);
        holder.fechaSubida.setText(documento.fechaSubida);
        holder.botonDescargar.setOnClickListener(v -> escucha.alDescargar(documento));
        holder.botonEliminar.setOnClickListener(v -> escucha.alEliminar(documento));

        android.content.Context contexto = holder.itemView.getContext();
        if (documento.validado) {
            holder.chipEstado.setText(R.string.chip_validado);
            holder.chipEstado.setBackgroundResource(R.drawable.bg_chip_exito);
            holder.chipEstado.setTextColor(androidx.core.content.ContextCompat.getColor(contexto, R.color.on_exito_contenedor));
        } else {
            holder.chipEstado.setText(R.string.chip_pendiente);
            holder.chipEstado.setBackgroundResource(R.drawable.bg_chip_neutro);
            holder.chipEstado.setTextColor(androidx.core.content.ContextCompat.getColor(contexto, R.color.texto_secundario));
        }

        AnimacionResorte.entradaDeFila(holder.itemView, position, posicionesYaAnimadas);
    }

    @Override
    public int getItemCount() {
        return documentos.size();
    }

    // Usado por la tarjeta "Estado del dossier" para mostrar cuantos de los
    // documentos ya cargados en pantalla estan validados. Se recalcula despues
    // de cada reemplazarTodo()/agregarPagina(), no es un valor que se guarde.
    public int contarValidados() {
        int contador = 0;
        for (Documento documento : documentos) {
            if (documento.validado) {
                contador++;
            }
        }
        return contador;
    }

    static class DocumentoViewHolder extends RecyclerView.ViewHolder {
        final TextView tipo;
        final TextView nombreArchivo;
        final TextView fechaSubida;
        final TextView chipEstado;
        final ImageButton botonDescargar;
        final ImageButton botonEliminar;

        DocumentoViewHolder(@NonNull View itemView) {
            super(itemView);
            tipo = itemView.findViewById(R.id.textoTipoDocumento);
            nombreArchivo = itemView.findViewById(R.id.textoNombreArchivo);
            fechaSubida = itemView.findViewById(R.id.textoFechaSubida);
            chipEstado = itemView.findViewById(R.id.chipEstadoDocumento);
            botonDescargar = itemView.findViewById(R.id.botonDescargar);
            botonEliminar = itemView.findViewById(R.id.botonEliminarDocumento);
            AnimacionResorte.feedbackToque(botonDescargar);
            AnimacionResorte.feedbackToque(botonEliminar);
        }
    }
}
