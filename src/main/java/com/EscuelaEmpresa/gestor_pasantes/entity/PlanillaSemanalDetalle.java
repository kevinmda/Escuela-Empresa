package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "planilla_semanal_detalle")
public class PlanillaSemanalDetalle {

    @EmbeddedId
    private PlanillaSemanalDetalleId id;

    @ManyToOne
    @MapsId("idPs")
    @JoinColumn(name = "id_PS")
    private PlanillaSemanal planillaSemanal;

    private LocalDate fecha;

    @Column(length = 65)
    private String descripcion;

    @Column(precision = 5, scale = 0) // DECIMAL(5,0), mismo motivo que total_horas
    private BigDecimal horas;

    public PlanillaSemanalDetalle() {}

    public PlanillaSemanalDetalleId getId() { return id; }
    public void setId(PlanillaSemanalDetalleId id) { this.id = id; }

    public PlanillaSemanal getPlanillaSemanal() { return planillaSemanal; }
    public void setPlanillaSemanal(PlanillaSemanal planillaSemanal) { this.planillaSemanal = planillaSemanal; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getHoras() { return horas; }
    public void setHoras(BigDecimal horas) { this.horas = horas; }
}