package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;

public class PlanillaResumenDTO {

    private final Integer idPs;
    private final LocalDate fechaDesde;
    private final LocalDate fechaHasta;
    private final BigDecimal totalHoras;

    public PlanillaResumenDTO(PlanillaSemanal planilla) {
        this.idPs = planilla.getIdPs();
        this.fechaDesde = planilla.getFechaDesde();
        this.fechaHasta = planilla.getFechaHasta();
        this.totalHoras = planilla.getTotalHoras();
    }

    public Integer getIdPs() { return idPs; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public LocalDate getFechaHasta() { return fechaHasta; }
    public BigDecimal getTotalHoras() { return totalHoras; }
}
