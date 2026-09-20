package com.escuelaempresa.gestorpasantes;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.escuelaempresa.gestorpasantes.network.ApiConfig;
import com.escuelaempresa.gestorpasantes.network.VolleyMultipartRequest;
import com.escuelaempresa.gestorpasantes.network.VolleySingleton;
import com.escuelaempresa.gestorpasantes.session.SessionManager;
import com.escuelaempresa.gestorpasantes.util.AnimacionResorte;
import com.escuelaempresa.gestorpasantes.util.VistaAjusteEsquinas;
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
    // descripcion, se manda el codigo. El codigo sigue diciendo
    // "PLANTILLA_SEMANAL" (con "t") porque asi esta el nombre de la constante
    // en el enum del backend y tiene que coincidir exacto -- pero lo que ve
    // el alumno en la descripcion de abajo dice "Planilla" (sin "t"), que es
    // como se llama en realidad.
    private static final String[] CODIGOS_TIPO = {
            "CONTRATO", "PLANTILLA_SEMANAL", "AUTORIZACION", "FICHA_FINAL_ALUMNO", "FICHA_FINAL_EVALUATIVA"
    };
    private static final String[] DESCRIPCIONES_TIPO = {
            "Contrato de Pasantía", "Planilla Semanal", "Autorización",
            "Ficha Final del Alumno", "Ficha Final Evaluativa"
    };

    private Spinner spinnerTipo;
    private TextView textoArchivoElegido;
    private com.google.android.material.textfield.TextInputLayout campoNombreArchivo;
    private com.google.android.material.textfield.TextInputEditText inputNombreArchivo;
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
        campoNombreArchivo = findViewById(R.id.campoNombreArchivo);
        inputNombreArchivo = findViewById(R.id.inputNombreArchivo);
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
                mostrarArchivoElegido(nombreArchivoElegido);
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
        boolean sigueEnDialogo = false;
        try {
            bitmap = decodificarBitmapEscalado(archivoFotoTemporal, LADO_MAXIMO_ESCANEO_PX);
            if (bitmap == null) {
                // BitmapFactory.decodeFile puede devolver null en vez de tirar
                // una excepción (archivo a medio escribir, formato que no
                // reconoce, etc.) -- sin este chequeo, el resto del método
                // seguía con un bitmap nulo y terminaba en
                // NullPointerException sin capturar (la app se colgaba en vez
                // de mostrar el mensaje de error).
                mostrarError(getString(R.string.error_procesando_escaneo));
                return;
            }
            bitmap = corregirRotacion(bitmap, archivoFotoTemporal);

            // De acá en más sigue en mostrarDialogoAjusteEsquinas() /
            // continuarProcesandoConEsquinas(), esperando a que el alumno
            // arrastre las esquinas y confirme -- por eso el bitmap NO se
            // recicla en el finally de este método (sigueEnDialogo en true) ni
            // se apaga el "cargando" (mostrarDialogoAjusteEsquinas lo apaga
            // ella misma, ya que de acá en más el alumno tiene que poder
            // tocar la pantalla).
            sigueEnDialogo = true;
            mostrarDialogoAjusteEsquinas(bitmap);
        } catch (IOException | OutOfMemoryError e) {
            mostrarError(getString(R.string.error_procesando_escaneo));
        } finally {
            if (bitmap != null && !sigueEnDialogo) {
                bitmap.recycle();
            }
            archivoFotoTemporal.delete();
            archivoFotoTemporal = null;
            if (!sigueEnDialogo) {
                mostrarCargando(false);
            }
        }
    }

    // Diálogo a pantalla completa con VistaAjusteEsquinas: el alumno arrastra
    // las cuatro esquinas para que coincidan con los bordes reales de la hoja
    // (arrancan en las esquinas de la propia foto, así que si el papel ya
    // ocupa todo el cuadro no hace falta tocar nada) y confirma o cancela.
    private void mostrarDialogoAjusteEsquinas(Bitmap bitmapRotado) {
        mostrarCargando(false);

        View vistaDialogo = getLayoutInflater().inflate(R.layout.dialogo_ajuste_esquinas, null);
        VistaAjusteEsquinas vistaEsquinas = vistaDialogo.findViewById(R.id.vistaAjusteEsquinas);
        vistaEsquinas.setBitmap(bitmapRotado);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setView(vistaDialogo)
                .setCancelable(false)
                .create();
        // A pantalla completa: un AlertDialog normal queda chico por defecto,
        // y acá hace falta el espacio para poder arrastrar las esquinas con
        // precisión.
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        vistaDialogo.findViewById(R.id.botonCancelarAjuste).setOnClickListener(v -> {
            dialogo.dismiss();
            bitmapRotado.recycle();
        });

        vistaDialogo.findViewById(R.id.botonConfirmarAjuste).setOnClickListener(v -> {
            float[] esquinas = vistaEsquinas.obtenerEsquinasEnBitmap();
            dialogo.dismiss();
            continuarProcesandoConEsquinas(bitmapRotado, esquinas);
        });

        dialogo.show();
    }

    // Continúa el procesamiento después de que el alumno confirmó el ajuste
    // de esquinas: aplana la perspectiva y aplica el efecto tipo escáner --
    // después de esto sigue mostrarVistaPreviaPdf(), no arma el PDF todavía.
    private void continuarProcesandoConEsquinas(Bitmap bitmapRotado, float[] esquinas) {
        mostrarCargando(true);
        Bitmap bitmap = bitmapRotado;
        boolean sigueEnVistaPrevia = false;
        try {
            Bitmap aplanado = aplanarPerspectiva(bitmap, esquinas);
            bitmap.recycle();
            bitmap = aplanado;

            Bitmap ajustado = ajustarComoEscaneo(bitmap);
            bitmap.recycle();
            bitmap = ajustado;

            sigueEnVistaPrevia = true;
            mostrarVistaPreviaPdf(bitmap);
        } catch (OutOfMemoryError e) {
            mostrarError(getString(R.string.error_procesando_escaneo));
        } finally {
            if (bitmap != null && !sigueEnVistaPrevia) {
                bitmap.recycle();
            }
            if (!sigueEnVistaPrevia) {
                mostrarCargando(false);
            }
        }
    }

    // Diálogo a pantalla completa mostrando el resultado final (ya con el
    // efecto tipo escáner aplicado) antes de armar el PDF de verdad: "Usar
    // esta" sigue a finalizarPdf(), "Descartar" corta acá -- no se genera
    // ningún PDF, el alumno puede reintentar con "Escanear" desde cero.
    private void mostrarVistaPreviaPdf(Bitmap bitmapFinal) {
        mostrarCargando(false);

        View vistaDialogo = getLayoutInflater().inflate(R.layout.dialogo_vista_previa_pdf, null);
        ImageView imagenVistaPrevia = vistaDialogo.findViewById(R.id.imagenVistaPrevia);
        imagenVistaPrevia.setImageBitmap(bitmapFinal);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setView(vistaDialogo)
                .setCancelable(false)
                .create();
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        vistaDialogo.findViewById(R.id.botonDescartarVistaPrevia).setOnClickListener(v -> {
            dialogo.dismiss();
            bitmapFinal.recycle();
        });

        vistaDialogo.findViewById(R.id.botonUsarVistaPrevia).setOnClickListener(v -> {
            dialogo.dismiss();
            finalizarPdf(bitmapFinal);
        });

        dialogo.show();
    }

    // Último paso: recién acá se escribe el PDF de verdad a disco, una vez
    // que el alumno ya vio y aceptó cómo va a quedar.
    private void finalizarPdf(Bitmap bitmapFinal) {
        mostrarCargando(true);
        try {
            String nombrePdf = "escaneo_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                    + ".pdf";
            File carpeta = new File(getCacheDir(), "documentos");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            File archivoPdf = new File(carpeta, nombrePdf);
            escribirBitmapComoPdf(bitmapFinal, archivoPdf);

            archivoElegido = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", archivoPdf);
            nombreArchivoElegido = nombrePdf;
            mostrarArchivoElegido(nombreArchivoElegido);
        } catch (IOException | OutOfMemoryError e) {
            mostrarError(getString(R.string.error_procesando_escaneo));
        } finally {
            bitmapFinal.recycle();
            mostrarCargando(false);
        }
    }

    // Aplana el cuadrilátero marcado por el alumno (las cuatro esquinas
    // reales de la hoja en la foto, tal como quedaron en VistaAjusteEsquinas)
    // a un rectángulo derecho -- warp de perspectiva con
    // Matrix.setPolyToPoly(), que con 4 puntos hace una transformación
    // proyectiva real (no solo rotar/escalar), sin necesitar ninguna
    // librería de visión por computadora aparte.
    private Bitmap aplanarPerspectiva(Bitmap original, float[] esquinasOrigen) {
        // esquinasOrigen: sup-izq, sup-der, inf-der, inf-izq (mismo orden en
        // que las arma VistaAjusteEsquinas.obtenerEsquinasEnBitmap()). El
        // ancho/alto del rectángulo destino sale del lado más largo de cada
        // par (arriba/abajo, izquierda/derecha), para no recortar nada del
        // documento aunque el ángulo de la foto haya alargado un lado más que
        // el opuesto.
        float anchoSuperior = distanciaEntrePuntos(esquinasOrigen, 0, 1);
        float anchoInferior = distanciaEntrePuntos(esquinasOrigen, 3, 2);
        float altoIzquierdo = distanciaEntrePuntos(esquinasOrigen, 0, 3);
        float altoDerecho = distanciaEntrePuntos(esquinasOrigen, 1, 2);

        int anchoDestino = Math.max((int) Math.max(anchoSuperior, anchoInferior), 1);
        int altoDestino = Math.max((int) Math.max(altoIzquierdo, altoDerecho), 1);

        float[] esquinasDestino = {
                0, 0,
                anchoDestino, 0,
                anchoDestino, altoDestino,
                0, altoDestino
        };

        Matrix matriz = new Matrix();
        matriz.setPolyToPoly(esquinasOrigen, 0, esquinasDestino, 0, 4);

        Bitmap resultado = Bitmap.createBitmap(anchoDestino, altoDestino, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultado);
        canvas.drawColor(Color.WHITE);
        Paint pincel = new Paint(
                Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(original, matriz, pincel);
        return resultado;
    }

    private float distanciaEntrePuntos(float[] puntos, int indiceA, int indiceB) {
        float dx = puntos[indiceA * 2] - puntos[indiceB * 2];
        float dy = puntos[indiceA * 2 + 1] - puntos[indiceB * 2 + 1];
        return (float) Math.sqrt(dx * dx + dy * dy);
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

    // Ajuste automático tipo CamScanner: el papel y el texto se llevan a
    // blanco y negro fuerte (mucho más legible que la foto original, con su
    // gris apagado por la luz ambiente), pero la tinta azul de una firma o un
    // sello se mantiene en color -- para un documento oficial puede importar
    // distinguir una firma original de una fotocopia.
    private static final int UMBRAL_AZUL = 20;

    // Curva de brillo aplicada DESPUÉS del estiramiento, solo al papel/texto
    // (no a la tinta azul, para no lavarle el color): 255 * (valor/255)^GAMMA,
    // con GAMMA < 1 empuja los grises claros hacia blanco sin tocar apenas el
    // negro del texto. Se precalcula una tabla de 256 valores en vez de
    // levantar Math.pow() por cada píxel.
    private static final double GAMMA_BRILLO = 0.72;

    private Bitmap ajustarComoEscaneo(Bitmap original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        int[] pixeles = new int[ancho * alto];
        original.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto);

        // 1. Histograma de luminancia, solo de los píxeles que NO son tinta
        // azul (el papel y el texto en negro/gris) -- así una firma azul no
        // corre el rango de contraste del resto del documento.
        int[] histograma = new int[256];
        long totalNoAzul = 0;
        for (int pixel : pixeles) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            if (!esTintaAzul(r, g, b)) {
                histograma[calcularLuma(r, g, b)]++;
                totalNoAzul++;
            }
        }

        // 2. Percentiles en vez de min/max crudo: un puñado de píxeles sueltos
        // (una sombra en una esquina, un reflejo de luz) no debería definir
        // todo el rango de contraste. Asimétrico a propósito: del lado oscuro
        // se recorta poco (1%) para no perder trazos finos del texto, pero del
        // lado claro se recorta bastante más (15%) -- el papel ocupa la
        // mayoría de la foto, así que el percentil 85 para arriba es
        // prácticamente todo papel, y conviene llevarlo a blanco de una sin
        // que una luz un poco pareja se quede corta.
        int minLuma = percentilDeHistograma(histograma, totalNoAzul, true, 1);
        int maxLuma = percentilDeHistograma(histograma, totalNoAzul, false, 15);
        int rango = Math.max(maxLuma - minLuma, 10);

        // 3. Tabla de brillo: se arma una sola vez acá (no en cada píxel).
        int[] curvaBrillo = new int[256];
        for (int i = 0; i < 256; i++) {
            curvaBrillo[i] = recortarA0y255((int) Math.round(255 * Math.pow(i / 255.0, GAMMA_BRILLO)));
        }

        // 4. Estirar ese rango a blanco y negro (más la curva de brillo del
        // paso anterior), salvo la tinta azul, que se mantiene en color (con
        // el mismo estiramiento en sus tres canales, sin la curva de brillo,
        // para no lavarle el color a la firma o el sello).
        for (int i = 0; i < pixeles.length; i++) {
            int pixel = pixeles[i];
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            if (esTintaAzul(r, g, b)) {
                r = recortarA0y255((r - minLuma) * 255 / rango);
                g = recortarA0y255((g - minLuma) * 255 / rango);
                b = recortarA0y255((b - minLuma) * 255 / rango);
                pixeles[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            } else {
                int estirado = recortarA0y255((calcularLuma(r, g, b) - minLuma) * 255 / rango);
                int gris = curvaBrillo[estirado];
                pixeles[i] = (0xFF << 24) | (gris << 16) | (gris << 8) | gris;
            }
        }

        Bitmap resultado = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
        resultado.setPixels(pixeles, 0, ancho, 0, 0, ancho, alto);
        return resultado;
    }

    // El canal azul se destaca claramente sobre el rojo y el verde: el
    // negro/gris del texto y el blanco/crema del papel tienen los tres
    // canales parejos, así que no entran acá -- esto agarra nada más que una
    // lapicera o un sello azul.
    private boolean esTintaAzul(int r, int g, int b) {
        return b > r + UMBRAL_AZUL && b > g + UMBRAL_AZUL;
    }

    private int calcularLuma(int r, int g, int b) {
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    // desdeElInicio=true busca el percentil "porcentaje" recorriendo el
    // histograma de 0 a 255; false lo busca recorriendo al revés, de 255 a 0
    // (o sea, el percentil "100 - porcentaje").
    private int percentilDeHistograma(int[] histograma, long total, boolean desdeElInicio, int porcentaje) {
        if (total == 0) return desdeElInicio ? 0 : 255;

        long corte = Math.max(1, total * porcentaje / 100);
        long acumulado = 0;
        if (desdeElInicio) {
            for (int i = 0; i < 256; i++) {
                acumulado += histograma[i];
                if (acumulado >= corte) return i;
            }
            return 255;
        } else {
            for (int i = 255; i >= 0; i--) {
                acumulado += histograma[i];
                if (acumulado >= corte) return i;
            }
            return 0;
        }
    }

    private int recortarA0y255(int valor) {
        if (valor < 0) return 0;
        if (valor > 255) return 255;
        return valor;
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
    // alcanza para un documento escaneado.
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

    // Muestra el campo para renombrar en vez del texto fijo de "ningún archivo
    // elegido": se le carga el nombre tal como llegó (del selector o del
    // escaneo) pero sin la extensión .pdf, porque esa queda fija aparte
    // (app:suffixText en el layout) -- así no hay riesgo de que el alumno la
    // borre o la cambie sin querer.
    private void mostrarArchivoElegido(String nombreCompleto) {
        textoArchivoElegido.setVisibility(View.GONE);
        campoNombreArchivo.setVisibility(View.VISIBLE);
        inputNombreArchivo.setText(quitarExtensionPdf(nombreCompleto));
    }

    private String quitarExtensionPdf(String nombre) {
        if (nombre != null && nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return nombre.substring(0, nombre.length() - 4);
        }
        return nombre;
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

        // El alumno pudo haber cambiado el nombre en inputNombreArchivo desde
        // que se eligió/escaneó el archivo -- eso manda por sobre
        // nombreArchivoElegido (el original) al momento de subir. La
        // extensión .pdf no se toca: en el campo nunca se muestra (queda como
        // suffixText fijo en el layout), así que acá se agrega siempre.
        String nombreEditado = inputNombreArchivo.getText() != null
                ? inputNombreArchivo.getText().toString().trim() : "";
        if (nombreEditado.isEmpty()) {
            mostrarError(getString(R.string.nombre_archivo_vacio));
            return;
        }
        String nombreFinal = nombreEditado + ".pdf";

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
                archivos.put("archivo", new DatosArchivo(nombreFinal, "application/pdf", contenido));
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
