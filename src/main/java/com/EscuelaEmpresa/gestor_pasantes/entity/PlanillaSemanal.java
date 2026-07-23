package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "planilla_semanal")
public class PlanillaSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_PS")
    private Integer idPs;

    @Column(length = 100)
    private String supervisor;

    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    @Column(name = "total_horas")
    private Float totalHoras;
    
    @Column(length = 265)
    private String conocimientos;

    @Column(length = 200)
    private String experiencia;

    @Column(length = 200)
    private String aprendizaje;

    @ManyToOne
    @JoinColumn(name = "id_Al")
    private Alumno alumno;

    public PlanillaSemanal() {}

    public Integer getIdPs() { return idPs; }
    public void setIdPs(Integer idPs) { this.idPs = idPs; }

    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public LocalDate getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(LocalDate fechaDesde) { this.fechaDesde = fechaDesde; }

    public LocalDate getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(LocalDate fechaHasta) { this.fechaHasta = fechaHasta; }

    public Float getTotalHoras() { return totalHoras; }
    public void setTotalHoras(Float totalHoras) { this.totalHoras = totalHoras; }

    public String getConocimientos() { return conocimientos; }
    public void setConocimientos(String conocimientos) { this.conocimientos = conocimientos; }

    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String experiencia) { this.experiencia = experiencia; }

    public String getAprendizaje() { return aprendizaje; }
    public void setAprendizaje(String aprendizaje) { this.aprendizaje = aprendizaje; }

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }
}