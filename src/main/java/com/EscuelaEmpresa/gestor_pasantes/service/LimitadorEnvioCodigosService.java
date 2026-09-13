package com.EscuelaEmpresa.gestor_pasantes.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Techo de cuantos codigos por correo se le pueden emitir a una misma cuenta.
 *
 * Sin esto, pedir un codigo nuevo era gratis e ilimitado, y como cada emision
 * reinicia el contador de intentos fallidos, el maximo de 5 intentos no era un
 * maximo: se probaban 5 codigos, se pedia otro, y otra vez 5. Un codigo de seis
 * digitos se termina adivinando asi. De paso, tambien evita que se use la
 * pantalla de "olvide mi contraseña" para llenarle la casilla a alguien.
 *
 * El registro vive en memoria a proposito: es informacion que solo importa
 * durante la proxima hora y no vale una columna ni una tabla. Si la aplicacion
 * se reinicia se pierde, y esta bien -- el otro freno (no reiniciar el contador
 * de intentos mientras el codigo anterior siga vigente) esta en la base y ese
 * sobrevive.
 */
@Service
public class LimitadorEnvioCodigosService {

    private static final int MAXIMO_POR_VENTANA = 3;
    private static final Duration VENTANA = Duration.ofHours(1);

    // email -> momentos en que se le envio un codigo dentro de la ventana
    private final Map<String, Deque<LocalDateTime>> enviosPorEmail = new ConcurrentHashMap<>();

    /**
     * Anota un envio para este email si todavia queda cupo.
     *
     * @return true si se puede enviar (y queda anotado), false si ya se paso del tope
     */
    public boolean registrarEnvioSiHayCupo(String email) {
        if (email == null) {
            return false;
        }

        String clave = email.toLowerCase();

        // computeIfAbsent + synchronized sobre la propia cola: dos pedidos
        // simultaneos para el mismo email no pueden colarse los dos.
        Deque<LocalDateTime> envios = enviosPorEmail.computeIfAbsent(clave, k -> new ArrayDeque<>());

        synchronized (envios) {
            LocalDateTime ahora = LocalDateTime.now();
            descartarVencidos(envios, ahora);

            if (envios.size() >= MAXIMO_POR_VENTANA) {
                return false;
            }

            envios.addLast(ahora);
            return true;
        }
    }

    /**
     * Suelta el cupo del ultimo envio anotado. Se usa cuando el correo termino
     * sin salir (Gmail caido, mal configurado): seria injusto gastarle un intento
     * al usuario por una falla nuestra.
     */
    public void devolverCupo(String email) {
        if (email == null) {
            return;
        }

        Deque<LocalDateTime> envios = enviosPorEmail.get(email.toLowerCase());
        if (envios == null) {
            return;
        }

        synchronized (envios) {
            envios.pollLast();
        }
    }

    private void descartarVencidos(Deque<LocalDateTime> envios, LocalDateTime ahora) {
        LocalDateTime limite = ahora.minus(VENTANA);
        Iterator<LocalDateTime> iterador = envios.iterator();

        while (iterador.hasNext()) {
            if (iterador.next().isBefore(limite)) {
                iterador.remove();
            } else {
                break; // la cola esta en orden: el primero que sigue vigente corta el barrido
            }
        }
    }
}
