package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "alumno")
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Al")
    private Integer idAl;
    //atributos
    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 25)
    private String ci;

    @Column(length = 15)
    private String sexo;

    @Column(name = "fechaNac")
    private LocalDate fechaNac;

    @Column(length = 35)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 45)
    private String curso;

    @Column(length = 45)
    private String seccion;

    @OneToOne                          //porque el alumno solo tiene un Usuario asociado (indica tipo de relacion)
    @JoinColumn(name = "id_Usr")       //la columna id_Usr de esta tabla apunta a una fila de usuario
    private Usuario usuario;           //esto dice quiero poder acceder a ese Usuario completo como un objeto, no solo como un número

    @ManyToOne                         //porque muchos alumnos pueden pertenecer a una especialidad
    @JoinColumn(name = "id_Esp")       //la columna id_Esp de esta tabla apunta a una fila de especialidad
    private Especialidad especialidad; //lo mismo que con Usuario solo que ahora queremos poder acceder a esa Especialidad tambien como objeto

    @ManyToOne
    @JoinColumn(name = "id_Emp")
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "id_PT")
    private PadreTutor padreTutor;

    @ManyToOne                         //muchos alumnos pueden tener el mismo supervisor asignado
    @JoinColumn(name = "id_Sup")
    private Supervisor supervisor;

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

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public PadreTutor getPadreTutor() { return padreTutor; }
    public void setPadreTutor(PadreTutor padreTutor) { this.padreTutor = padreTutor; }

    public Supervisor getSupervisor() { return supervisor; }
    public void setSupervisor(Supervisor supervisor) { this.supervisor = supervisor; }

    @Transient
    public int getEdad() {
        if (this.fechaNac == null) {
            return 0; // o podrías lanzar excepción, según prefieras manejar el caso
        }
        return Period.between(this.fechaNac, LocalDate.now()).getYears();
    }
}