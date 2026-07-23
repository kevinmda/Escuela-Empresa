package com.EscuelaEmpresa.gestor_pasantes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;

public interface PlanillaSemanalRepository extends JpaRepository<PlanillaSemanal, Integer> {
}