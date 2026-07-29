package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;

public interface PlanillaSemanalRepository extends JpaRepository<PlanillaSemanal, Integer> {
    List<PlanillaSemanal> findByAlumno_IdAlOrderByFechaDesdeDesc(Integer idAl);

    // Chequea si el alumno tiene alguna planilla cuyo rango de fechas se solape con la semana consultada.
    // Se usa solapamiento (en vez de igualdad exacta) porque un alumno podría cargar menos de los 6 días
    // de la semana, y entonces fechaDesde/fechaHasta de su planilla no coinciden exactamente con
    // el lunes/sábado de la semana calendario.
    boolean existsByAlumno_IdAlAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
            Integer idAl, LocalDate semanaHasta, LocalDate semanaDesde);
}