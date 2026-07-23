package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public class DiaForm {

    private String nombreDia;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String descripcion;
    private Float horas;

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

    public Float getHoras() { return horas; }
    public void setHoras(Float horas) { this.horas = horas; }
}