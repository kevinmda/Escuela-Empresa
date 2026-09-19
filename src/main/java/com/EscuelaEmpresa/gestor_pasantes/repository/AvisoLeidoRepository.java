package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.EscuelaEmpresa.gestor_pasantes.entity.AvisoLeido;
import com.EscuelaEmpresa.gestor_pasantes.entity.AvisoLeidoId;

public interface AvisoLeidoRepository extends JpaRepository<AvisoLeido, AvisoLeidoId> {
    Optional<AvisoLeido> findByIdAlAndCodigo(Integer idAl, String codigo);
}
