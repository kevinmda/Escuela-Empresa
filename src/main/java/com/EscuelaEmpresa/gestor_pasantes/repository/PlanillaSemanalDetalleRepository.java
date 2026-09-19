package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalleId;

public interface PlanillaSemanalDetalleRepository extends JpaRepository<PlanillaSemanalDetalle, PlanillaSemanalDetalleId> {
    List<PlanillaSemanalDetalle> findByPlanillaSemanal_IdPs(Integer idPs);
    void deleteByPlanillaSemanal_IdPs(Integer idPs);
}