package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;

public interface PlanillaSemanalRepository extends JpaRepository<PlanillaSemanal, Integer> {
    List<PlanillaSemanal> findByAlumno_IdAlOrderByFechaDesdeDesc(Integer idAl);

    // Version paginada del mismo listado, para la API movil (GET /api/movil/planillas?page=&size=)
    Page<PlanillaSemanal> findByAlumno_IdAlOrderByFechaDesdeDesc(Integer idAl, Pageable pageable);

    // Cuenta cuántos alumnos distintos (dentro de las especialidades dadas) ya entregaron
    // alguna planilla que se solape con la semana consultada. Se usa para el % de cumplimiento
    // que se muestra en el Inicio del Coordinador.
    @Query("SELECT COUNT(DISTINCT p.alumno.idAl) FROM PlanillaSemanal p "
            + "WHERE p.alumno.especialidad.idEsp IN :idsEsp "
            + "AND p.fechaDesde <= :semanaHasta AND p.fechaHasta >= :semanaDesde")
    long countAlumnosConEntregaEnSemana(@Param("idsEsp") List<Integer> idsEsp,
                                         @Param("semanaDesde") LocalDate semanaDesde,
                                         @Param("semanaHasta") LocalDate semanaHasta);

    // Misma idea, pero sin filtrar por especialidad (para el Administrativo, que ve todo el colegio)
    @Query("SELECT COUNT(DISTINCT p.alumno.idAl) FROM PlanillaSemanal p "
            + "WHERE p.fechaDesde <= :semanaHasta AND p.fechaHasta >= :semanaDesde")
    long countAlumnosConEntregaEnSemanaGlobal(@Param("semanaDesde") LocalDate semanaDesde,
                                                @Param("semanaHasta") LocalDate semanaHasta);
}