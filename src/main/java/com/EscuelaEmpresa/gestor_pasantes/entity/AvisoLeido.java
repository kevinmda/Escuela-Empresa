package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Qué aviso del header (ver ChromeContext.Aviso) marcó como leído cada
// alumno, por código -- una fila por (alumno, código). "clave" es la
// ocurrencia puntual que se leyó (ver el comentario en la migración V3): si
// la clave actual de ese código no coincide con esta, vuelve a contar como
// no leído.
@Entity
@Table(name = "aviso_leido")
@IdClass(AvisoLeidoId.class)
public class AvisoLeido {

    @Id
    @Column(name = "id_Al")
    private Integer idAl;

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(length = 30)
    private String clave;

    public AvisoLeido() {}

    public Integer getIdAl() { return idAl; }
    public void setIdAl(Integer idAl) { this.idAl = idAl; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
}
