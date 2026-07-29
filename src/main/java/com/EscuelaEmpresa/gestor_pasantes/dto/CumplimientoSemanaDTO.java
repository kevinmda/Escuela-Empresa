package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.time.LocalDate;
import java.util.List;

public class CumplimientoSemanaDTO {

    private LocalDate semanaDesde;
    private LocalDate semanaHasta;
    private List<AlumnoCumplimientoDTO> alumnos;

    public CumplimientoSemanaDTO(LocalDate semanaDesde, LocalDate semanaHasta, List<AlumnoCumplimientoDTO> alumnos) {
        this.semanaDesde = semanaDesde; 
        this.semanaHasta = semanaHasta;
        this.alumnos = alumnos;
    }

    public LocalDate getSemanaDesde() { return semanaDesde; }
    public LocalDate getSemanaHasta() { return semanaHasta; }
    public List<AlumnoCumplimientoDTO> getAlumnos() { return alumnos; }
}