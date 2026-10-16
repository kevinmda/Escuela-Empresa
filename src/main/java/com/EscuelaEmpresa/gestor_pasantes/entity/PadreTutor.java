package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "padre_tutor")
public class PadreTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_PT")
    private Integer idPt;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 25)
    private String ci;

    @Column(length = 100)
    private String email;

    // Lado dueño de la relación (la FK vive acá, en id_Al): antes era al revés
    // (alumno.id_PT, un solo padre/tutor por alumno). Ver V12 y Alumno.padresTutores.
    @ManyToOne
    @JoinColumn(name = "id_Al")
    private Alumno alumno;

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

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }
}