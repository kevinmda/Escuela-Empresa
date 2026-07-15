package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Integer> {
    Optional<Administrador> findByUsuario_IdUsr(Integer idUsr); //el guion dice "navegá a través de la relación Usuario de Administrador, y mirá su campo idUsr"
}