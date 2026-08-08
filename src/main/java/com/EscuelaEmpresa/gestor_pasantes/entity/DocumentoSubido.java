package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_subido")
public class DocumentoSubido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_Doc")
    private Integer idDoc;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo; // nombre original del archivo (lo que veia el alumno en su compu)

    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo; // donde vive el archivo en el filesystem del servidor

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    @ManyToOne
    @JoinColumn(name = "id_Al")
    private Alumno alumno;

    public Integer getIdDoc() { return idDoc; }
    public void setIdDoc(Integer idDoc) { this.idDoc = idDoc; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public Alumno getAlumno() { return alumno; }
    public void setAlumno(Alumno alumno) { this.alumno = alumno; }
}
