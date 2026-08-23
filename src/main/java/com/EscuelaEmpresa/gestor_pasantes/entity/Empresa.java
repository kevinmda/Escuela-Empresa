package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Emp")
    private Integer idEmp;
    //atributos
    @Column(length = 100)
    private String nombre;

    @Column(length = 25)
    private String ruc;

    @Column(length = 35)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String direccion;

    @ManyToOne                          // muchas empresas pueden pertenecer a la misma especialidad
    @JoinColumn(name = "id_Esp")
    private Especialidad especialidad;

    public Empresa() {}

    public Integer getIdEmp() { return idEmp; }
    public void setIdEmp(Integer idEmp) { this.idEmp = idEmp; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }
}