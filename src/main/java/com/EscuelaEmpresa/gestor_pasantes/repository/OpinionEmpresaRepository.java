package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.EscuelaEmpresa.gestor_pasantes.entity.OpinionEmpresa;

public interface OpinionEmpresaRepository extends JpaRepository<OpinionEmpresa, Integer> {

    List<OpinionEmpresa> findByAlumno_IdAlOrderByFechaCreacionDesc(Integer idAl);

    // Cuántas opiniones "vivas" tiene el alumno en este momento -- es el cupo
    // contra el que se compara el tope de 3: borrar una libera lugar para otra.
    long countByAlumno_IdAl(Integer idAl);

    // Lo que ve Coordinación/Administración: todas las opiniones de alumnos
    // dentro de las especialidades permitidas (todas, si es administrativo;
    // solo la propia, si es coordinador -- la lista de ids ya viene resuelta).
    List<OpinionEmpresa> findByAlumno_Especialidad_IdEspInOrderByFechaCreacionDesc(List<Integer> idsEsp);
}
