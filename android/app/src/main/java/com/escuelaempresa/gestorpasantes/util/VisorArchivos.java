package com.escuelaempresa.gestorpasantes.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// Guarda un archivo descargado en cache/ y lo abre con la app del sistema que
// corresponda (visor de PDF, Word, etc.), via FileProvider. Antes esta logica
// vivia duplicada en DocumentosFragment.
public final class VisorArchivos {

    private VisorArchivos() {
    }

    public static void abrir(Context context, byte[] contenido, String nombreArchivo, String tipoMime)
            throws IOException {
        File carpeta = new File(context.getCacheDir(), "documentos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        File archivo = new File(carpeta, nombreArchivo);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            salida.write(contenido);
        }

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", archivo);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, tipoMime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
