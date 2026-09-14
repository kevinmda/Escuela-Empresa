package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;

public class PerfilAlumnoDTO {

    private final Integer idAl;
    private final String nombres;
    private final String apellidos;
    private final String ci;
    private final String email;
    private final String telefono;
    private final String curso;
    private final String seccion;
    private final String especialidad;
    private final String empresa;
    private final String supervisor;
    private final boolean contrasenaPorDefecto;

    public PerfilAlumnoDTO(Alumno alumno) {
        this.idAl = alumno.getIdAl();
        this.nombres = alumno.getNombres();
        this.apellidos = alumno.getApellidos();
        this.ci = alumno.getCi();
        this.email = alumno.getEmail();
        this.telefono = alumno.getTelefono();
        this.curso = alumno.getCurso();
        this.seccion = alumno.getSeccion();
        this.especialidad = alumno.getEspecialidad() != null ? alumno.getEspecialidad().getNombre() : null;
        this.empresa = alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : null;
        this.supervisor = alumno.getSupervisor() != null
                ? (alumno.getSupervisor().getNombres() + " " + alumno.getSupervisor().getApellidos())
                : null;
        // La app usa esto para mandar al alumno directo a cambiar su contraseña
        // despues del login, igual que ContrasenaPorDefectoInterceptor lo hace en
        // la web (ese interceptor no corre para /api/movil/**, ver WebConfig).
        this.contrasenaPorDefecto = alumno.getUsuario() != null
                && Boolean.TRUE.equals(alumno.getUsuario().getContrasenaPorDefecto());
    }

    public Integer getIdAl() { return idAl; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCi() { return ci; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getCurso() { return curso; }
    public String getSeccion() { return seccion; }
    public String getEspecialidad() { return especialidad; }
    public String getEmpresa() { return empresa; }
    public String getSupervisor() { return supervisor; }
    public boolean isContrasenaPorDefecto() { return contrasenaPorDefecto; }
}
