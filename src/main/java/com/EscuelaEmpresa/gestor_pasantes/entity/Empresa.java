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
    private String nombre;
    private String ruc;
    private String telefono;
    private String email;
    private String direccion;

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
}
