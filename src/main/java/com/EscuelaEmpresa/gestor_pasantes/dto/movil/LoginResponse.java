package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

public class LoginResponse {

    private final String token;
    private final PerfilAlumnoDTO alumno;

    public LoginResponse(String token, PerfilAlumnoDTO alumno) {
        this.token = token;
        this.alumno = alumno;
    }

    public String getToken() { return token; }
    public PerfilAlumnoDTO getAlumno() { return alumno; }
}
