package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public class DiaForm {

    private String nombreDia;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String descripcion;
    // BigDecimal y no Float ni int: las horas se cargan enteras (sin
    // fracciones), pero se validan y normalizan en el service antes de
    // guardar, así que el DTO todavía necesita poder representar lo que el
    // alumno haya escrito, decimales incluidos, para poder rechazarlo con un
    // mensaje claro en vez de un error de parseo.
    private BigDecimal horas;

    public DiaForm() {}

    public DiaForm(String nombreDia) {
        this.nombreDia = nombreDia;
    }

    public String getNombreDia() { return nombreDia; }
    public void setNombreDia(String nombreDia) { this.nombreDia = nombreDia; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getHoras() { return horas; }
    public void setHoras(BigDecimal horas) { this.horas = horas; }
}