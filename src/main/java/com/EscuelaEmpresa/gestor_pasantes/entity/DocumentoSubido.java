package com.EscuelaEmpresa.gestor_pasantes.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "documento_scan")
public class DocumentoSubido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_DS")
    private Integer idDs;

    @Column(name = "nombre", length = 100)
    private String nombreArchivo; // nombre original del archivo (lo que veia el alumno en su compu)

    @Column(name = "ruta", length = 200)
    private String rutaArchivo; // donde vive el archivo en el filesystem del servidor

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = true)
    private TipoDocumento tipoDocumento; // tipo de documento que se está subiendo

    @Column(name = "hash_integridad", length = 64)
    private String hashIntegridad; // SHA-256 del archivo para verificar integridad

    @Column(name = "validado", columnDefinition = "boolean default false")
    private Boolean validado = false; // indica si el documento pasó las validaciones

    @ManyToOne
    @JoinColumn(name = "id_Al")
    private Alumno alumno;

    public Integer getIdDs() { return idDs; }
    public void setIdDs(Integer idDs) { this.idDs = idDs; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getHashIntegridad() { return hashIntegridad; }
    public void setHashIntegridad(String hashIntegridad) { this.hashIntegridad = hashIntegridad; }

    public Boolean getValidado() { return validado != null ? validado : false; }
    public void setValidado(Boolean validado) { this.validado = validado; }

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }
}
