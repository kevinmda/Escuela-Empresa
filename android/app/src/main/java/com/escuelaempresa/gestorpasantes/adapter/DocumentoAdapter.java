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

import java.util.ArrayList;
import java.util.List;

public class DocumentoAdapter extends RecyclerView.Adapter<DocumentoAdapter.DocumentoViewHolder> {

    public interface Escucha {
        void alDescargar(Documento documento);
        void alEliminar(Documento documento);
    }

    private final List<Documento> documentos = new ArrayList<>();
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
    }

    @Override
    public int getItemCount() {
        return documentos.size();
    }

    static class DocumentoViewHolder extends RecyclerView.ViewHolder {
        final TextView tipo;
        final TextView nombreArchivo;
        final TextView fechaSubida;
        final ImageButton botonDescargar;
        final ImageButton botonEliminar;

        DocumentoViewHolder(@NonNull View itemView) {
            super(itemView);
            tipo = itemView.findViewById(R.id.textoTipoDocumento);
            nombreArchivo = itemView.findViewById(R.id.textoNombreArchivo);
            fechaSubida = itemView.findViewById(R.id.textoFechaSubida);
            botonDescargar = itemView.findViewById(R.id.botonDescargar);
            botonEliminar = itemView.findViewById(R.id.botonEliminarDocumento);
        }
    }
}
