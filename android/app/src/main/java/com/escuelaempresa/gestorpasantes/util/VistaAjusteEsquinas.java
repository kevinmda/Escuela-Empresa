package com.escuelaempresa.gestorpasantes.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

// Vista para ajustar a mano las cuatro esquinas de un documento fotografiado
// de costado, antes de "aplanarlo" a un rectángulo derecho (ver
// SubirDocumentoActivity.aplanarPerspectiva()). Muestra la foto completa y
// cuatro círculos arrastrables, uno por esquina, que arrancan en las propias
// esquinas de la foto -- si el papel ya ocupa todo el cuadro, no hace falta
// tocar nada -- y se pueden mover con el dedo para que coincidan con los
// bordes reales del papel.
public class VistaAjusteEsquinas extends View {

    // Orden fijo: superior-izquierda, superior-derecha, inferior-derecha,
    // inferior-izquierda. El mismo orden lo espera aplanarPerspectiva() del
    // lado de la Activity.
    private Bitmap bitmap;
    private final PointF[] esquinas = new PointF[4]; // en coordenadas de PANTALLA, no del bitmap
    private int esquinaArrastrada = -1;
    private final Paint pincelLinea = new Paint();
    private final Paint pincelPunto = new Paint();
    private final Matrix matrizBitmapAPantalla = new Matrix();
    private final Matrix matrizPantallaABitmap = new Matrix();

    private static final float RADIO_PUNTO_DP = 14f;
    // Más grande que el punto que se ve, para que sea fácil agarrarlo con el
    // dedo sin tener que acertarle al centro exacto.
    private static final float RADIO_TOQUE_DP = 32f;

    public VistaAjusteEsquinas(Context context, AttributeSet attrs) {
        super(context, attrs);

        pincelLinea.setColor(Color.parseColor("#4CAF50"));
        pincelLinea.setStrokeWidth(dpAPx(3));
        pincelLinea.setStyle(Paint.Style.STROKE);
        pincelLinea.setAntiAlias(true);

        pincelPunto.setColor(Color.parseColor("#4CAF50"));
        pincelPunto.setStyle(Paint.Style.FILL);
        pincelPunto.setAntiAlias(true);
    }

    private float dpAPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        // El layout todavía puede no tener ancho/alto definitivos en este
        // momento (por ejemplo, si esto corre antes de que el diálogo termine
        // de armarse) -- post() lo deja para cuando la vista ya tenga tamaño real.
        post(this::recalcularTransformacionYEsquinasIniciales);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalcularTransformacionYEsquinasIniciales();
    }

    private void recalcularTransformacionYEsquinasIniciales() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;

        // Encajar el bitmap dentro de la vista, centrado, manteniendo la
        // proporción (equivalente a un ImageView con scaleType="fitCenter",
        // pero armado a mano para poder despues convertir toques de pantalla
        // a coordenadas reales del bitmap con la matriz inversa).
        RectF rectOrigen = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectDestino = new RectF(0, 0, getWidth(), getHeight());
        matrizBitmapAPantalla.setRectToRect(rectOrigen, rectDestino, Matrix.ScaleToFit.CENTER);
        matrizBitmapAPantalla.invert(matrizPantallaABitmap);

        float[] esquinasBitmap = {
                0, 0,
                bitmap.getWidth(), 0,
                bitmap.getWidth(), bitmap.getHeight(),
                0, bitmap.getHeight()
        };
        float[] esquinasPantalla = new float[8];
        matrizBitmapAPantalla.mapPoints(esquinasPantalla, esquinasBitmap);
        for (int i = 0; i < 4; i++) {
            esquinas[i] = new PointF(esquinasPantalla[i * 2], esquinasPantalla[i * 2 + 1]);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || esquinas[0] == null) return;

        canvas.drawBitmap(bitmap, matrizBitmapAPantalla, null);

        for (int i = 0; i < 4; i++) {
            PointF actual = esquinas[i];
            PointF siguiente = esquinas[(i + 1) % 4];
            canvas.drawLine(actual.x, actual.y, siguiente.x, siguiente.y, pincelLinea);
        }
        for (PointF esquina : esquinas) {
            canvas.drawCircle(esquina.x, esquina.y, dpAPx(RADIO_PUNTO_DP), pincelPunto);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        if (esquinas[0] == null) return false;

        float x = evento.getX();
        float y = evento.getY();

        switch (evento.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                esquinaArrastrada = encontrarEsquinaCercana(x, y);
                return esquinaArrastrada != -1;
            case MotionEvent.ACTION_MOVE:
                if (esquinaArrastrada != -1) {
                    esquinas[esquinaArrastrada].x = recortar(x, 0, getWidth());
                    esquinas[esquinaArrastrada].y = recortar(y, 0, getHeight());
                    invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                esquinaArrastrada = -1;
                return true;
            default:
                return false;
        }
    }

    private float recortar(float valor, float min, float max) {
        return Math.max(min, Math.min(max, valor));
    }

    private int encontrarEsquinaCercana(float x, float y) {
        float radioToque = dpAPx(RADIO_TOQUE_DP);
        int masCercana = -1;
        float distanciaMinima = Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float dx = x - esquinas[i].x;
            float dy = y - esquinas[i].y;
            float distancia = (float) Math.sqrt(dx * dx + dy * dy);
            if (distancia <= radioToque && distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercana = i;
            }
        }
        return masCercana;
    }

    // Las cuatro esquinas actuales, convertidas de coordenadas de pantalla a
    // coordenadas reales del bitmap (no las del View, que dependen del tamaño
    // de la pantalla) -- lo que necesita el warp de perspectiva del lado de
    // la Activity. Mismo orden que se dibuja: sup-izq, sup-der, inf-der, inf-izq.
    public float[] obtenerEsquinasEnBitmap() {
        float[] puntosPantalla = new float[8];
        for (int i = 0; i < 4; i++) {
            puntosPantalla[i * 2] = esquinas[i].x;
            puntosPantalla[i * 2 + 1] = esquinas[i].y;
        }
        float[] puntosBitmap = new float[8];
        matrizPantallaABitmap.mapPoints(puntosBitmap, puntosPantalla);
        return puntosBitmap;
    }
}
