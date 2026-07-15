package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidad")
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Esp")
    private Integer idEsp;
    //atributos
    private String nombre;

    @OneToOne                               //cada especialidad solo tiene un coordinador (un administrador con cargo coordinador)
    @JoinColumn(name = "id_Ad")             //la columna id_Ad de esta tabla apunta a una fila de administrador
    private Administrador administrador;    //el coordinador de esta especialidad

    public Especialidad() {} //constructor vacio, necesario porque internamente se crea un objeto vacio que luego recien se va llenando
    //getter y los setter. Estos son usados activamente por Hibernate para leer y escribir los valores de cada campo al convertir entre el objeto Java y la fila SQL
    public Integer getIdEsp() { return idEsp; }
    public void setIdEsp(Integer idEsp) { this.idEsp = idEsp; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Administrador getAdministrador() { return administrador; }
    public void setAdministrador(Administrador administrador) { this.administrador = administrador; }
}