package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public class DiaForm {

    private String nombreDia;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String descripcion;
    // BigDecimal y no Float: las horas se suman, y en binario 7.5 + 8.25 no da
    // 15.75 exacto. Con dos decimales fijos la cuenta cierra siempre.
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