package com.EscuelaEmpresa.gestor_pasantes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {
    Optional<Alumno> findByUsuario_IdUsr(Integer idUsr); //el guion dice "navegá a través de la relación Usuario de Alumno, y mirá su campo idUsr"
    // alumnos en pasantía de una especialidad, sin filtrar por curso/sección (usado en la asignación
    // de supervisores y empresas: a un egresado no se le asigna nada)
    List<Alumno> findByEspecialidad_IdEspAndEgresadoFalse(Integer idEsp);

    // Los egresados no cuentan en las estadísticas del inicio: ya no están en pasantía.
    long countByEgresadoFalse();

    long countByEspecialidad_IdEspInAndEgresadoFalse(List<Integer> idsEsp);

    long countBySupervisorIsNullAndEgresadoFalse();

    long countByEspecialidad_IdEspInAndSupervisorIsNullAndEgresadoFalse(List<Integer> idsEsp);

    // --- Filtro en cascada: Especialidad -> Curso -> Sección ---

    @Query("SELECT DISTINCT a.curso FROM Alumno a WHERE a.especialidad.idEsp = :idEsp ORDER BY a.curso")
    List<String> findCursosPorEspecialidad(@Param("idEsp") Integer idEsp);

    @Query("SELECT DISTINCT a.seccion FROM Alumno a WHERE a.especialidad.idEsp = :idEsp AND a.curso = :curso ORDER BY a.seccion")
    List<String> findSeccionesPorEspecialidadYCurso(@Param("idEsp") Integer idEsp, @Param("curso") String curso);

    // egresado=false: los que siguen en pasantía; egresado=true: los que ya terminaron el colegio
    List<Alumno> findByEspecialidad_IdEspAndCursoAndSeccionAndEgresado(Integer idEsp, String curso, String seccion, boolean egresado);

    // --- Búsqueda por nombre, apellido o CI (respeta el alcance: todas las especialidades
    // para administrativo, o solo la propia para coordinador, según qué lista de ids se pase) ---
    @Query("SELECT a FROM Alumno a WHERE a.especialidad.idEsp IN :idsEsp AND ("
            + "LOWER(a.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) "
            + "OR LOWER(a.apellidos) LIKE LOWER(CONCAT('%', :texto, '%')) "
            + "OR a.ci LIKE CONCAT('%', :texto, '%'))")
    List<Alumno> buscarPorNombreApellidoOCi(@Param("idsEsp") List<Integer> idsEsp, @Param("texto") String texto);

    // --- Desasignación masiva antes de borrar un supervisor / una empresa ---
    // La FK no deja borrar mientras algún alumno los tenga puestos. Un UPDATE
    // resuelve todos los alumnos de una vez, sin cargarlos ni guardarlos uno a uno.
    // El supervisor/empresa ya pertenece a la especialidad del coordinador (lo
    // verifica el controlador), así que sus alumnos también.

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Alumno a SET a.supervisor = null WHERE a.supervisor.idSup = :idSup")
    void desasignarSupervisor(@Param("idSup") Integer idSup);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Alumno a SET a.empresa = null WHERE a.empresa.idEmp = :idEmp")
    void desasignarEmpresa(@Param("idEmp") Integer idEmp);
}