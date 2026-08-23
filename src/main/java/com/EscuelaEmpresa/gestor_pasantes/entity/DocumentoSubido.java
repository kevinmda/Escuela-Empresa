package com.EscuelaEmpresa.gestor_pasantes.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }
}
