package com.escuelaempresa.gestorpasantes;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.VolleyMultipartRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SubirDocumentoActivity extends AppCompatActivity {

    // Mismos codigos que el enum TipoDocumento del backend -- se muestra la
    // descripcion, se manda el codigo.
    private static final String[] CODIGOS_TIPO = {
            "CONTRATO", "PLANTILLA_SEMANAL", "AUTORIZACION", "FICHA_FINAL_ALUMNO", "FICHA_FINAL_EVALUATIVA"
    };
    private static final String[] DESCRIPCIONES_TIPO = {
            "Contrato de Pasantía", "Plantilla Semanal", "Autorización",
            "Ficha Final del Alumno", "Ficha Final Evaluativa"
    };

    private Spinner spinnerTipo;
    private TextView textoArchivoElegido;
    private MaterialButton botonSubir;
    private ProgressBar progreso;

    // Lado ancho máximo de la foto escaneada: una hoja legible no necesita más,
    // y sin este tope una foto de 4000px+ de una cámara moderna generaría un PDF
    // de varios MB por una sola página (el servidor limita a 10MB por archivo).
    private static final int LADO_MAXIMO_ESCANEO_PX = 2000;

    private SessionManager sessionManager;
    // archivoElegido apunta a un PDF en los dos caminos: el que el alumno elige
    // con "Elegir archivo" (URI del selector del sistema) y el que arma
    // procesarFotoEscaneada() a partir de la foto (URI de FileProvider sobre un
    // PDF en cache/). subir() no distingue entre los dos -- para ContentResolver
    // ambos son URIs legibles igual.
    private Uri archivoElegido;
    private String nombreArchivoElegido;

    private ActivityResultLauncher<String[]> lanzadorSelector;
    private ActivityResultLauncher<Uri> lanzadorCamara;
    private ActivityResultLauncher<String> lanzadorPermisoCamara;
    // Dónde la cámara del sistema deja la foto de verdad. No es archivoElegido
    // (eso queda para el PDF ya armado): esta es la foto cruda, de paso, que se
    // borra apenas se convierte.
    private File archivoFotoTemporal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subir_documento);

        sessionManager = new SessionManager(this);

        Toolbar barra = findViewById(R.id.barraSubir);
        setSupportActionBar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        barra.setNavigationOnClickListener(v -> finish());

        spinnerTipo = findViewById(R.id.spinnerTipoDocumento);
        textoArchivoElegido = findViewById(R.id.textoArchivoElegido);
        MaterialButton botonElegirArchivo = findViewById(R.id.botonElegirArchivo);
        MaterialButton botonEscanear = findViewById(R.id.botonEscanear);
        botonSubir = findViewById(R.id.botonSubirDocumento);
        progreso = findViewById(R.id.progresoSubida);
        AnimacionResorte.feedbackToque(botonElegirArchivo);
        AnimacionResorte.feedbackToque(botonEscanear);
        AnimacionResorte.feedbackToque(botonSubir);

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DESCRIPCIONES_TIPO);
        spinnerTipo.setAdapter(adaptador);

        lanzadorSelector = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                archivoElegido = uri;
                nombreArchivoElegido = obtenerNombreArchivo(uri);
                textoArchivoElegido.setText(nombreArchivoElegido);
            }
        });

        // El pedido de permiso y la foto en sí son dos pasos separados: hasta
        // que el usuario no concede CAMERA no tiene sentido intentar abrir la
        // cámara, así que lanzarCamara() solo se llama después de un permiso ya
        // concedido (acá o porque ya lo estaba desde antes).
        lanzadorPermisoCamara = registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            if (concedido) {
                lanzarCamara();
            } else {
                mostrarError(getString(R.string.permiso_camara_denegado));
            }
        });

        lanzadorCamara = registerForActivityResult(new ActivityResultContracts.TakePicture(), exito -> {
            if (exito) {
                procesarFotoEscaneada();
            } else if (archivoFotoTemporal != null) {
                archivoFotoTemporal.delete();
            }
        });

        botonElegirArchivo.setOnClickListener(v -> lanzadorSelector.launch(new String[]{"application/pdf"}));
        botonEscanear.setOnClickListener(v -> alTocarEscanear());
        botonSubir.setOnClickListener(v -> subir());
    }

    private void alTocarEscanear() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara();
        } else {
            lanzadorPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    private void lanzarCamara() {
        try {
            File carpeta = new File(getCacheDir(), "documentos");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            archivoFotoTemporal = new File(carpeta, "captura_tmp.jpg");
            Uri destinoFoto = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", archivoFotoTemporal);
            lanzadorCamara.launch(destinoFoto);
        } catch (Exception e) {
            // ActivityNotFoundException (sin app de cámara instalada) y
            // cualquier otro fallo al preparar el archivo caen acá igual: en
            // los dos casos el alumno no puede escanear ahora mismo.
            mostrarError(getString(R.string.error_camara_no_disponible));
        }
    }

    // La foto que devuelve la cámara puede pesar varios MB y venir con
    // orientación al revés (el sensor siempre graba "acostado"; el giro real
    // queda en el EXIF, no en los píxeles) -- de acá sale un PDF de una sola
    // página, ya liviano y ya derecho, listo para subir() como cualquier otro
    // archivo elegido.
    private void procesarFotoEscaneada() {
        if (archivoFotoTemporal == null || !archivoFotoTemporal.exists()) {
            mostrarError(getString(R.string.error_procesando_escaneo));
            return;
        }

        mostrarCargando(true);
        Bitmap bitmap = null;
        try {
            bitmap = decodificarBitmapEscalado(archivoFotoTemporal, LADO_MAXIMO_ESCANEO_PX);
            bitmap = corregirRotacion(bitmap, archivoFotoTemporal);

            String nombrePdf = "escaneo_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                    + ".pdf";
            File carpeta = archivoFotoTemporal.getParentFile();
            File archivoPdf = new File(carpeta, nombrePdf);
            escribirBitmapComoPdf(bitmap, archivoPdf);

            archivoElegido = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", archivoPdf);
            nombreArchivoElegido = nombrePdf;
            textoArchivoElegido.setText(nombreArchivoElegido);
        } catch (IOException | OutOfMemoryError e) {
            mostrarError(getString(R.string.error_procesando_escaneo));
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            archivoFotoTemporal.delete();
            mostrarCargando(false);
        }
    }

    // inSampleSize solo acepta potencias de 2, y BitmapFactory las redondea
    // para abajo -- se decodifican primero solo las dimensiones (sin cargar los
    // píxeles) para elegir el tamaño de muestreo antes de decodificar de verdad.
    private Bitmap decodificarBitmapEscalado(File archivo, int ladoMaximoPx) {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(archivo.getAbsolutePath(), limites);

        int muestreo = 1;
        while (limites.outWidth / muestreo > ladoMaximoPx || limites.outHeight / muestreo > ladoMaximoPx) {
            muestreo *= 2;
        }

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = muestreo;
        return BitmapFactory.decodeFile(archivo.getAbsolutePath(), opciones);
    }

    private Bitmap corregirRotacion(Bitmap bitmap, File archivo) throws IOException {
        ExifInterface exif = new ExifInterface(archivo.getAbsolutePath());
        int orientacion = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        int grados;
        switch (orientacion) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                grados = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                grados = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                grados = 270;
                break;
            default:
                grados = 0;
        }
        if (grados == 0) {
            return bitmap;
        }

        Matrix matriz = new Matrix();
        matriz.postRotate(grados);
        Bitmap rotado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matriz, true);
        if (rotado != bitmap) {
            bitmap.recycle();
        }
        return rotado;
    }

    // Una página, del mismo tamaño en píxeles que la foto ya escalada: no hace
    // falta encajarla en A4 ni nada parecido, con que se vea igual que la foto
    // alcanza para un comprobante escaneado.
    private void escribirBitmapComoPdf(Bitmap bitmap, File destino) throws IOException {
        PdfDocument documento = new PdfDocument();
        try {
            PdfDocument.PageInfo info = new PdfDocument.PageInfo
                    .Builder(bitmap.getWidth(), bitmap.getHeight(), 1)
                    .create();
            PdfDocument.Page pagina = documento.startPage(info);
            pagina.getCanvas().drawBitmap(bitmap, 0, 0, null);
            documento.finishPage(pagina);

            try (FileOutputStream salida = new FileOutputStream(destino)) {
                documento.writeTo(salida);
            }
        } finally {
            documento.close();
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = "documento.pdf";
        ContentResolver resolver = getContentResolver();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (indice >= 0) {
                    nombre = cursor.getString(indice);
                }
            }
        }
        return nombre;
    }

    private void subir() {
        if (archivoElegido == null) {
            mostrarError(getString(R.string.ningun_archivo_elegido));
            return;
        }

        int posicionTipo = spinnerTipo.getSelectedItemPosition();
        String codigoTipo = CODIGOS_TIPO[posicionTipo];

        byte[] contenido;
        try (InputStream entrada = getContentResolver().openInputStream(archivoElegido)) {
            contenido = leerBytes(entrada);
        } catch (IOException e) {
            mostrarError(getString(R.string.error_red));
            return;
        }

        mostrarCargando(true);
        String token = sessionManager.obtenerToken();

        VolleyMultipartRequest pedido = new VolleyMultipartRequest(
                ApiConfig.BASE_URL + "/documentos", token,
                this::onSubidaExitosa,
                this::onSubidaFallida) {

            @Override
            protected Map<String, String> getStringParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tipoDocumento", codigoTipo);
                return params;
            }

            @Override
            protected Map<String, DatosArchivo> getFileParams() {
                Map<String, DatosArchivo> archivos = new HashMap<>();
                archivos.put("archivo", new DatosArchivo(nombreArchivoElegido, "application/pdf", contenido));
                return archivos;
            }
        };

        VolleySingleton.getInstancia(this).getRequestQueue().add(pedido);
    }

    private byte[] leerBytes(InputStream entrada) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            salida.write(buffer, 0, leidos);
        }
        return salida.toByteArray();
    }

    private void onSubidaExitosa(NetworkResponse respuesta) {
        mostrarCargando(false);
        Snackbar.make(botonSubir, R.string.documento_subido_ok, Snackbar.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void onSubidaFallida(VolleyError error) {
        mostrarCargando(false);
        mostrarError(extraerMensaje(error));
    }

    private String extraerMensaje(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                org.json.JSONObject cuerpo = new org.json.JSONObject(new String(error.networkResponse.data, "UTF-8"));
                if (cuerpo.has("mensaje")) {
                    return cuerpo.getString("mensaje");
                }
            } catch (Exception ignorada) {
                // el body no era el JSON esperado -- se usa el mensaje generico de abajo
            }
        }
        return getString(R.string.error_red);
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonSubir.setEnabled(!cargando);
    }

    private void mostrarError(String mensaje) {
        Snackbar.make(botonSubir, mensaje, Snackbar.LENGTH_LONG).show();
    }
}
