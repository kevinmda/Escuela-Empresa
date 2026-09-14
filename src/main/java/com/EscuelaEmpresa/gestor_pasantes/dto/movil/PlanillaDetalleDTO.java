package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;

public class PlanillaDetalleDTO {

    private final Integer idPs;
    private final LocalDate fechaDesde;
    private final LocalDate fechaHasta;
    private final BigDecimal totalHoras;
    private final String supervisor;
    private final String conocimientos;
    private final String experiencia;
    private final String aprendizaje;
    private final List<DiaDTO> dias;

    public PlanillaDetalleDTO(PlanillaSemanal planilla, List<DiaDTO> dias) {
        this.idPs = planilla.getIdPs();
        this.fechaDesde = planilla.getFechaDesde();
        this.fechaHasta = planilla.getFechaHasta();
        this.totalHoras = planilla.getTotalHoras();
        this.supervisor = planilla.getSupervisor();
        this.conocimientos = planilla.getConocimientos();
        this.experiencia = planilla.getExperiencia();
        this.aprendizaje = planilla.getAprendizaje();
        this.dias = dias;
    }

    public Integer getIdPs() { return idPs; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public LocalDate getFechaHasta() { return fechaHasta; }
    public BigDecimal getTotalHoras() { return totalHoras; }
    public String getSupervisor() { return supervisor; }
    public String getConocimientos() { return conocimientos; }
    public String getExperiencia() { return experiencia; }
    public String getAprendizaje() { return aprendizaje; }
    public List<DiaDTO> getDias() { return dias; }
}
