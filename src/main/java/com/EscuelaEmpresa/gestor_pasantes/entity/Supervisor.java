package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "supervisor")
public class Supervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Sup")
    private Integer idSup;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 100)
    private String email;

    @ManyToOne                          // muchos supervisores pueden pertenecer a la misma especialidad
    @JoinColumn(name = "id_Esp")
    private Especialidad especialidad;

    public Supervisor() {}

    public Integer getIdSup() { return idSup; }
    public void setIdSup(Integer idSup) { this.idSup = idSup; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }
}