package com.escuelaempresa.gestorpasantes.util;

import android.view.MotionEvent;
import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.Set;

// Pequenios toques de movimiento con resortes de verdad en vez de animaciones de
// duracion fija: la respuesta al tocar es inmediata y el resorte se frena solo,
// sin importar en que punto este cuando el dedo se levanta.
public final class AnimacionResorte {

    private AnimacionResorte() {
    }

    // Escala hacia abajo apenas se toca (nunca hay que esperar a soltar para ver
    // feedback) y vuelve a su tamanio original al soltar o cancelar. Devuelve false
    // para no interferir con el click ni con el ripple normal de la vista.
    public static void feedbackToque(View vista) {
        vista.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    escalar(v, 0.96f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    escalar(v, 1f);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private static void escalar(View vista, float destino) {
        resorte(vista, SpringAnimation.SCALE_X, destino, SpringForce.DAMPING_RATIO_NO_BOUNCY, 700f).start();
        resorte(vista, SpringAnimation.SCALE_Y, destino, SpringForce.DAMPING_RATIO_NO_BOUNCY, 700f).start();
    }

    // Entrada de una fila de RecyclerView: sube con un resorte y aparece. Se anima
    // una sola vez por posicion -- si no, cada reciclado de vista al scrollear
    // repetiria la animacion, que se siente pesado en vez de fluido.
    public static void entradaDeFila(View vista, int posicion, Set<Integer> posicionesYaAnimadas) {
        if (!posicionesYaAnimadas.add(posicion)) {
            vista.setTranslationY(0f);
            vista.setAlpha(1f);
            return;
        }
        vista.setTranslationY(60f);
        vista.setAlpha(0f);
        resorte(vista, SpringAnimation.TRANSLATION_Y, 0f, SpringForce.DAMPING_RATIO_NO_BOUNCY, 300f).start();
        vista.animate().alpha(1f).setDuration(180).start();
    }

    private static SpringAnimation resorte(View vista, DynamicAnimation.ViewProperty propiedad,
                                            float destino, float amortiguacion, float rigidez) {
        SpringAnimation animacion = new SpringAnimation(vista, propiedad, destino);
        SpringForce fuerza = new SpringForce(destino);
        fuerza.setDampingRatio(amortiguacion);
        fuerza.setStiffness(rigidez);
        animacion.setSpring(fuerza);
        return animacion;
    }
}
