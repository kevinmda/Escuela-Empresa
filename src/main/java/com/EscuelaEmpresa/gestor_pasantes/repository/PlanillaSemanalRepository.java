package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;

public interface PlanillaSemanalRepository extends JpaRepository<PlanillaSemanal, Integer> {
    List<PlanillaSemanal> findByAlumno_IdAlOrderByFechaDesdeDesc(Integer idAl);
}