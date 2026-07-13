package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {
    Optional<Especialidad> findByAdministrador_IdAd(Integer idAd); //el guion dice "navegá a través de la relación Administrador de Especialidad, y mirá su campo idAd". Sino mal entiendo devuelve una lista de las especialidades que cumplen eso
}