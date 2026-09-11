package com.EscuelaEmpresa.gestor_pasantes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;

    public AlumnoService(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public List<Alumno> listarSegunCargo(Administrador admin) {

        if ("administrativo".equalsIgnoreCase(admin.getCargo())) {
            return alumnoRepository.findAll();
        }

        if ("coordinador".equalsIgnoreCase(admin.getCargo())) {
            if (admin.getEspecialidades() == null || admin.getEspecialidades().isEmpty()) {
                throw new ReglaNegocioException("El coordinador no tiene especialidad asignada");
            }
            Integer idEsp = admin.getEspecialidades().get(0).getIdEsp();
            return alumnoRepository.findByEspecialidad_IdEspIn(List.of(idEsp));
        }

        throw new ReglaNegocioException("Cargo de administrador no reconocido: " + admin.getCargo());
    }
}