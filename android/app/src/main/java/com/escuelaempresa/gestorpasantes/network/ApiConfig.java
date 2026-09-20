package com.escuelaempresa.gestorpasantes.network;

public final class ApiConfig {

    private ApiConfig() {}

    // Servidor de produccion. Para desarrollo local con el emulador, usar
    // "http://10.0.2.2:8080/api/movil" (localhost de la compu que corre el backend)
    // o la IP de la compu en la red Wi-Fi para un celular fisico.
    public static final String BASE_URL = "https://gestor-pasante.duckdns.org/api/movil";
}
