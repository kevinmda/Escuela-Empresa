package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Supervisor;

public interface SupervisorRepository extends JpaRepository<Supervisor, Integer> {
    List<Supervisor> findByEspecialidad_IdEsp(Integer idEsp);
}