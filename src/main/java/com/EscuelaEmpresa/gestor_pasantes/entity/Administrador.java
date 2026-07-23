package com.EscuelaEmpresa.gestor_pasantes.entity;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "administrador")
public class Administrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Ad")
    private Integer idAd;
    //atributos
    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 25)
    private String ci;

    @Column(length = 35)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 45)
    private String cargo; //puede ser "administrativo" o "coordinador"

    @OneToOne                           //porque el administrador solo tiene un Usuario asociado (indica tipo de relacion)
    @JoinColumn(name = "id_Usr")        //la columna id_Usr de esta tabla apunta a una fila de usuario
    private Usuario usuario;            //esto dice quiero poder acceder a ese Usuario completo como un objeto, no solo como un número

    @OneToMany(mappedBy = "administrador")
    private List<Especialidad> especialidades;

    public Administrador() {} //constructor vacio, necesario porque internamente se crea un objeto vacio que luego recien se va llenando
    //getter y los setter. Estos son usados activamente por Hibernate para leer y escribir los valores de cada campo al convertir entre el objeto Java y la fila SQL
    public Integer getIdAd() { return idAd; }
    public void setIdAd(Integer idAd) { this.idAd = idAd; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getCi() { return ci; }
    public void setCi(String ci) { this.ci = ci; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public List<Especialidad> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<Especialidad> especialidades) { this.especialidades = especialidades; }
}