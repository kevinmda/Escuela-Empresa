package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PlanillaSemanalDetalleId implements Serializable {

    @Column(name = "id_PS")
    private Integer idPs;

    @Column(name = "id_PSD")
    private Integer idPsd;

    public PlanillaSemanalDetalleId() {}

    public PlanillaSemanalDetalleId(Integer idPs, Integer idPsd) {
        this.idPs = idPs;
        this.idPsd = idPsd;
    }

    public Integer getIdPs() { return idPs; }
    public void setIdPs(Integer idPs) { this.idPs = idPs; }

    public Integer getIdPsd() { return idPsd; }
    public void setIdPsd(Integer idPsd) { this.idPsd = idPsd; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanillaSemanalDetalleId)) return false;
        PlanillaSemanalDetalleId that = (PlanillaSemanalDetalleId) o;
        return Objects.equals(idPs, that.idPs) && Objects.equals(idPsd, that.idPsd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPs, idPsd);
    }
}