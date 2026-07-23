package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.util.ArrayList;
import java.util.List;

public class PlanillaSemanalForm {

    private List<DiaForm> dias = new ArrayList<>();
    private String supervisor;
    private String conocimientos;
    private String experiencia;
    private String aprendizaje;

    public PlanillaSemanalForm() {
        dias.add(new DiaForm("Lunes"));
        dias.add(new DiaForm("Martes"));
        dias.add(new DiaForm("Miércoles"));
        dias.add(new DiaForm("Jueves"));
        dias.add(new DiaForm("Viernes"));
        dias.add(new DiaForm("Sábado"));
    }

    public List<DiaForm> getDias() { return dias; }
    public void setDias(List<DiaForm> dias) { this.dias = dias; }

    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public String getConocimientos() { return conocimientos; }
    public void setConocimientos(String conocimientos) { this.conocimientos = conocimientos; }

    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String experiencia) { this.experiencia = experiencia; }

    public String getAprendizaje() { return aprendizaje; }
    public void setAprendizaje(String aprendizaje) { this.aprendizaje = aprendizaje; }
}