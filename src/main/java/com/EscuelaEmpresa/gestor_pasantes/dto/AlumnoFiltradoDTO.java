package com.EscuelaEmpresa.gestor_pasantes.dto;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;

public class AlumnoFiltradoDTO {

    private Integer idAl;
    private String nombres;
    private String apellidos;
    private String ci;
    private String email;
    private String curso;
    private String seccion;

    public AlumnoFiltradoDTO(Alumno alumno) {
        this.idAl = alumno.getIdAl();
        this.nombres = alumno.getNombres();
        this.apellidos = alumno.getApellidos();
        this.ci = alumno.getCi();
        this.email = alumno.getEmail();
        this.curso = alumno.getCurso();
        this.seccion = alumno.getSeccion();
    }

    public Integer getIdAl() { return idAl; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCi() { return ci; }
    public String getEmail() { return email; }
    public String getCurso() { return curso; }
    public String getSeccion() { return seccion; }
}