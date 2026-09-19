package com.EscuelaEmpresa.gestor_pasantes.entity;

import java.io.Serializable;
import java.util.Objects;

// Clave compuesta (id_Al, tipo) de aviso_leido: un alumno tiene a lo sumo una
// fila por tipo de aviso (ver AvisoLeido).
public class AvisoLeidoId implements Serializable {

    private Integer idAl;
    private String tipo;

    public AvisoLeidoId() {}

    public AvisoLeidoId(Integer idAl, String tipo) {
        this.idAl = idAl;
        this.tipo = tipo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof AvisoLeidoId that)) return false;
        return Objects.equals(idAl, that.idAl) && Objects.equals(tipo, that.tipo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAl, tipo);
    }
}
