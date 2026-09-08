package com.EscuelaEmpresa.gestor_pasantes.dto;

/**
 * Datos de identidad que necesita el cromo compartido de las pantallas autenticadas.
 * No expone la entidad de dominio completa a las vistas: el header solo necesita
 * saber quién está dentro, qué rol ocupa y, para un alumno, su especialidad.
 */
public record ChromeContext(
        String rol,
        String nombre,
        String email,
        Integer especialidadId,
        String especialidadNombre) {
}
