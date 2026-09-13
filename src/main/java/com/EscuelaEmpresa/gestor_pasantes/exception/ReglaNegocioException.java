package com.EscuelaEmpresa.gestor_pasantes.exception;

// Una regla del sistema que el usuario puede corregir: "ya cargaste las 6 semanas",
// "falta la fecha del martes", "el coordinador no tiene especialidad". El mensaje esta
// pensado para mostrarse tal cual en pantalla.
//
// Distinguirla de RuntimeException importa en dos lugares: en PlanillaSemanalController,
// que muestra el mensaje adentro del formulario y antes atrapaba CUALQUIER RuntimeException
// (un NullPointerException tambien terminaba como "error del formulario"); y en
// ManejoErroresGlobal, que la muestra como un 400 con el texto en vez de un 500 mudo.
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
