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
 * Techo generico de pedidos por IP y por minuto, para las dos pantallas publicas
 * sin sesion donde cualquiera puede mandar el formulario las veces que quiera:
 * /login-check y /olvide-contrasena. Ninguna de las dos revela si una cuenta
 * existe (esa es la garantia que ya tienen), pero sin un techo por IP nada
 * impide barrer miles de emails distintos contra ellas a la velocidad de la
 * red. El limite por cuenta que ya existe en LimitadorEnvioCodigosService no
 * alcanza para eso: ese cuenta emails enviados a una MISMA cuenta, no pedidos
 * hechos desde un mismo origen.
 *
 * Vive en memoria por la misma razon que ese limitador: es informacion que
 * solo importa por el proximo minuto, no vale persistirla.
 */
@Service
public class LimitadorPeticionesPorIpService {

    private static final int MAXIMO_POR_VENTANA = 20;
    private static final Duration VENTANA = Duration.ofMinutes(1);

    // clave (ej: "login-check:1.2.3.4") -> momentos en que se anoto un pedido
    // dentro de la ventana. La clave incluye el nombre del endpoint para que
    // /login-check y /olvide-contrasena tengan cada uno su propio cupo.
    private final Map<String, Deque<LocalDateTime>> peticionesPorClave = new ConcurrentHashMap<>();

    /**
     * @return true si el pedido puede seguir (y queda anotado), false si esa
     * clave ya se paso del tope para la ventana actual.
     */
    public boolean permitir(String clave) {
        if (clave == null) {
            return true; // sin forma de identificar el origen, no se bloquea por las dudas
        }

        Deque<LocalDateTime> peticiones = peticionesPorClave.computeIfAbsent(clave, k -> new ArrayDeque<>());

        // synchronized sobre la propia cola, mismo criterio que LimitadorEnvioCodigosService:
        // dos pedidos simultaneos desde el mismo origen no pueden colarse los dos.
        synchronized (peticiones) {
            LocalDateTime ahora = LocalDateTime.now();
            descartarVencidas(peticiones, ahora);

            if (peticiones.size() >= MAXIMO_POR_VENTANA) {
                return false;
            }

            peticiones.addLast(ahora);
            return true;
        }
    }

    private void descartarVencidas(Deque<LocalDateTime> peticiones, LocalDateTime ahora) {
        LocalDateTime limite = ahora.minus(VENTANA);
        Iterator<LocalDateTime> iterador = peticiones.iterator();

        while (iterador.hasNext()) {
            if (iterador.next().isBefore(limite)) {
                iterador.remove();
            } else {
                break; // la cola esta en orden: el primero que sigue vigente corta el barrido
            }
        }
    }
}
