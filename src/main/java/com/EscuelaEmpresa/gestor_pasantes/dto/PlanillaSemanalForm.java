package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.util.ArrayList;
import java.util.List;

public class PlanillaSemanalForm {

    private List<DiaForm> dias = new ArrayList<>();
    private String supervisor;
    private String conocimientos;
    private String experiencia;
    private String aprendizaje;

    // Puramente de UI: no lo usa ninguna validación ni se guarda en la base.
    // Sirve para que, si el servidor rechaza el envío y vuelve a mostrar la
    // misma página con lo que el alumno ya cargó, el JS sepa cuál de los seis
    // campos de fecha eligió como ancla -- sin esto, al recargar con los seis
    // ya completos (la ancla más las cinco calculadas), no hay forma de
    // distinguir cuál fue la que el alumno realmente tocó.
    // String y no Integer: el campo oculto llega vacío ("") cuando todavía no
    // hay ancla, y Spring no sabe convertir un string vacío a Integer -- tira
    // una excepción antes de llegar siquiera al controller (error 500).
    private String indiceFechaAncla;

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

    public String getIndiceFechaAncla() { return indiceFechaAncla; }
    public void setIndiceFechaAncla(String indiceFechaAncla) { this.indiceFechaAncla = indiceFechaAncla; }
}