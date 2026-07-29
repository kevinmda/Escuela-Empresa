package com.EscuelaEmpresa.gestor_pasantes.dto;

public class AlumnoCumplimientoDTO {

    private Integer idAl;
    private String nombres;
    private String apellidos;
    private String ci;
    private boolean entrego;

    public AlumnoCumplimientoDTO(Integer idAl, String nombres, String apellidos, String ci, boolean entrego) {
        this.idAl = idAl;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.ci = ci;
        this.entrego = entrego;
    }

    public Integer getIdAl() { return idAl; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCi() { return ci; }
    public boolean isEntrego() { return entrego; }
}