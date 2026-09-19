package com.EscuelaEmpresa.gestor_pasantes.entity;

import java.io.Serializable;
import java.util.Objects;

// Clave compuesta (id_Al, codigo) de aviso_leido: un alumno tiene a lo sumo
// una fila por código de aviso (ver AvisoLeido).
public class AvisoLeidoId implements Serializable {

    private Integer idAl;
    private String codigo;

    public AvisoLeidoId() {}

    public AvisoLeidoId(Integer idAl, String codigo) {
        this.idAl = idAl;
        this.codigo = codigo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof AvisoLeidoId that)) return false;
        return Objects.equals(idAl, that.idAl) && Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAl, codigo);
    }
}
