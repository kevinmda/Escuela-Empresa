package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DiaDTO {

    private String nombreDia;
    private LocalDate fecha;
    private String descripcion;
    private BigDecimal horas;

    public DiaDTO() {}

    public DiaDTO(String nombreDia, LocalDate fecha, String descripcion, BigDecimal horas) {
        this.nombreDia = nombreDia;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.horas = horas;
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
