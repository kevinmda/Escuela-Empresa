package com.escuelaempresa.gestorpasantes.network;

public final class ApiConfig {

    private ApiConfig() {}

    // 10.0.2.2 es el alias que usa el emulador de Android para llegar al localhost
    // de la compu donde corre el backend. Para probar en un celular fisico, cambiar
    // por la IP de la compu en la red Wi-Fi (ej. "http://192.168.1.10:8080") y agregar
    // esa IP en res/xml/network_security_config.xml.
    public static final String BASE_URL = "http://10.0.2.2:8080/api/movil";
}
