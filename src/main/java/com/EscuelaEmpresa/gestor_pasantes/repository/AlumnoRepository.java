package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {
    Optional<Alumno> findByUsuario_IdUsr(Integer idUsr); //el guion dice "navegá a través de la relación Usuario de Alumno, y mirá su campo idUsr"
    List<Alumno> findByEspecialidad_IdEspIn(List<Integer> idsEsp); //el guion dice "navegá a través de la relación Especialidad de Alumno, y mirá su campo idEsp", y In significa "que esté dentro de esta lista de valores". Es el equivalente a WHERE id_Esp IN (?, ?, ?, ...) en SQL. Al final devuelve una lista de los alumnos que cumplen eso
}