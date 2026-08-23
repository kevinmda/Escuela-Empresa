package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
    List<Empresa> findByEspecialidad_IdEsp(Integer idEsp);
}