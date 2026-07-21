package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "padre_tutor")
public class PadreTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_PT")
    private Integer idPt;

    private String nombres;
    private String apellidos;
    private String ci;
    private String email;

    public PadreTutor() {}

    public Integer getIdPt() { return idPt; }
    public void setIdPt(Integer idPt) { this.idPt = idPt; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getCi() { return ci; }
    public void setCi(String ci) { this.ci = ci; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}