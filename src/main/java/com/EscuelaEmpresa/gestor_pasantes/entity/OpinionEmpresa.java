package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * La opinión que un alumno deja sobre la empresa donde está haciendo su
 * pasantía: un comentario libre más una calificación de 1 a 5 estrellas.
 *
 * "empresa" se guarda aparte de "alumno" y no se resuelve navegando
 * alumno.getEmpresa(): esto es una FOTO de a qué empresa se refería la
 * opinión el día que se escribió. Si a ese alumno más tarde le cambian la
 * empresa asignada (o se la sacan), la opinión vieja tiene que seguir
 * señalando a la empresa de la que realmente habló, no a la actual.
 */
@Entity
@Table(name = "opinion_empresa")
public class OpinionEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Opi")
    private Integer idOpi;

    @ManyToOne
    @JoinColumn(name = "id_Al")
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "id_Emp")
    private Empresa empresa;

    @Column(length = 1000)
    private String comentario;

    private int calificacion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public OpinionEmpresa() {}

    public Integer getIdOpi() { return idOpi; }
    public void setIdOpi(Integer idOpi) { this.idOpi = idOpi; }

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public int getCalificacion() { return calificacion; }
    public void setCalificacion(int calificacion) { this.calificacion = calificacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
