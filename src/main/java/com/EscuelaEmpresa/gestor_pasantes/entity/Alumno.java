package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "alumno")
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Al")
    private Integer idAl;
    //atributos
    private String nombres;
    private String apellidos;
    private String ci;
    private String sexo;

    @Column(name = "fechaNac")
    private LocalDate fechaNac;

    private String telefono;
    private String email;
    private String curso;
    private String seccion;

    @OneToOne                          //porque el alumno solo tiene un Usuario asociado (indica tipo de relacion)
    @JoinColumn(name = "id_Usr")       //la columna id_Usr de esta tabla apunta a una fila de usuario
    private Usuario usuario;           //esto dice quiero poder acceder a ese Usuario completo como un objeto, no solo como un número

    @ManyToOne                         //porque muchos alumnos pueden pertenecer a una especialidad
    @JoinColumn(name = "id_Esp")       //la columna id_Esp de esta tabla apunta a una fila de especialidad
    private Especialidad especialidad; //lo mismo que con Usuario solo que ahora queremos poder acceder a esa Especialidad tambien como objeto

    // Nota: id_Sup, id_Emp, id_PT quedan pendientes de mapear
    // cuando trabajemos Supervisor, Empresa y Padre_Tutor.

    public Alumno() {} //constructor vacio, necesario porque internamente se crea un objeto vacio que luego recien se va llenando
    //getter y los setter. Estos son usados activamente por Hibernate para leer y escribir los valores de cada campo al convertir entre el objeto Java y la fila SQL
    public Integer getIdAl() { return idAl; }
    public void setIdAl(Integer idAl) { this.idAl = idAl; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getCi() { return ci; }
    public void setCi(String ci) { this.ci = ci; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public LocalDate getFechaNac() { return fechaNac; }
    public void setFechaNac(LocalDate fechaNac) { this.fechaNac = fechaNac; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Especialidad getEspecialidad() { return especialidad; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }
}
