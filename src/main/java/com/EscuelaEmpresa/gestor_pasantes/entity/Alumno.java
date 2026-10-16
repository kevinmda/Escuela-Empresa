package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

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

    // Antes era un @ManyToOne (un solo padre/tutor por alumno, con la FK en
    // alumno.id_PT). Ahora un alumno puede tener varios: la FK pasó a vivir
    // en padre_tutor.id_Al (ver V12), así que del lado de Alumno esto es la
    // punta inversa de ese @ManyToOne -- mappedBy, sin columna propia acá.
    // @OrderBy para que el orden sea siempre el mismo (por id, el orden en
    // que se fueron cargando) y no dependa de lo que decida devolver MySQL.
    @OneToMany(mappedBy = "alumno")
    @OrderBy("idPt ASC")
    private List<PadreTutor> padresTutores = new ArrayList<>();

    @ManyToOne                         //muchos alumnos pueden tener el mismo supervisor asignado
    @JoinColumn(name = "id_Sup")
    private Supervisor supervisor;

    // Egresado: ya terminó el colegio. Sus datos quedan en la base para que los
    // consulten el coordinador y el administrativo, pero no puede usar el sistema
    // y no cuenta en el inicio ni en la asignación de supervisores/empresas. Se
    // pone en true solo (ver autorizaConservarDatos más abajo) o, si el alumno
    // no dio esa autorización, se sigue marcando a mano en la base (0 por
    // defecto en los dos casos).
    @Column(name = "egresado", nullable = false)
    private boolean egresado = false;

    // Autorización del propio alumno (casilla en su Inicio, con aviso previo
    // explicando la implicancia) para que sus datos sigan en el sistema una
    // vez que termine la pasantía, en vez de borrarse. Con esto en true, en
    // cuanto suba sus Documentos Adjuntos (el expediente final combinado y
    // firmado, el último paso del proceso) el sistema pone egresado=true por
    // su cuenta -- ver DocumentoAlmacenamientoService.marcarEgresadoSiCorresponde.
    // Sin esta autorización, nada cambia: egresado sigue marcándose a mano.
    @Column(name = "autoriza_conservar_datos", nullable = false)
    private boolean autorizaConservarDatos = false;

    public Alumno() {} //constructor vacio, necesario porque internamente se crea un objeto vacio que luego recien se va llenando
    //getter y los setter. Estos son usados activamente por Hibernate para leer y escribir los valores de cada campo al convertir entre el objeto Java y la fila SQL
    public boolean isEgresado() { return egresado; }
    public void setEgresado(boolean egresado) { this.egresado = egresado; }
    public boolean isAutorizaConservarDatos() { return autorizaConservarDatos; }
    public void setAutorizaConservarDatos(boolean autorizaConservarDatos) { this.autorizaConservarDatos = autorizaConservarDatos; }
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

    public List<PadreTutor> getPadresTutores() { return padresTutores; }
    public void setPadresTutores(List<PadreTutor> padresTutores) { this.padresTutores = padresTutores; }

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