package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {
    Optional<Alumno> findByUsuario_IdUsr(Integer idUsr); //el guion dice "navegá a través de la relación Usuario de Alumno, y mirá su campo idUsr"
    List<Alumno> findByEspecialidad_IdEspIn(List<Integer> idsEsp); //el guion dice "navegá a través de la relación Especialidad de Alumno, y mirá su campo idEsp", y In significa "que esté dentro de esta lista de valores". Es el equivalente a WHERE id_Esp IN (?, ?, ?, ...) en SQL. Al final devuelve una lista de los alumnos que cumplen eso

    // --- Filtro en cascada: Especialidad -> Curso -> Sección ---

    @Query("SELECT DISTINCT a.curso FROM Alumno a WHERE a.especialidad.idEsp = :idEsp ORDER BY a.curso")
    List<String> findCursosPorEspecialidad(@Param("idEsp") Integer idEsp);

    @Query("SELECT DISTINCT a.seccion FROM Alumno a WHERE a.especialidad.idEsp = :idEsp AND a.curso = :curso ORDER BY a.seccion")
    List<String> findSeccionesPorEspecialidadYCurso(@Param("idEsp") Integer idEsp, @Param("curso") String curso);

    List<Alumno> findByEspecialidad_IdEspAndCursoAndSeccion(Integer idEsp, String curso, String seccion);
}