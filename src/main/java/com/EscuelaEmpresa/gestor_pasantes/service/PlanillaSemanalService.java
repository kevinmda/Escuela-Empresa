package com.EscuelaEmpresa.gestor_pasantes.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.dto.DiaForm;
import com.EscuelaEmpresa.gestor_pasantes.dto.PlanillaSemanalForm;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalleId;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;

@Service
public class PlanillaSemanalService {

    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;

    public PlanillaSemanalService(PlanillaSemanalRepository planillaSemanalRepository,
                                   PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository) {
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
    }

    public void guardarPlanilla(PlanillaSemanalForm form, Alumno alumno) {

        // Validar cada día ANTES de filtrar
        for (DiaForm dia : form.getDias()) {
            validarDia(dia);
        }

        // 1. Filtrar solo los días que el alumno realmente cargó (fecha no vacía)
        List<DiaForm> diasCargados = form.getDias().stream()
                .filter(dia -> dia.getFecha() != null)
                .toList();

        if (diasCargados.isEmpty()) {
            throw new RuntimeException("Debe cargar al menos un día trabajado");
        }

        // 2. Calcular fecha_desde, fecha_hasta y total_horas
        LocalDate fechaDesde = diasCargados.stream()
                .map(DiaForm::getFecha)
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate fechaHasta = diasCargados.stream()
                .map(DiaForm::getFecha)
                .max(LocalDate::compareTo)
                .orElseThrow();

        float totalHoras = 0f;
        for (DiaForm dia : diasCargados) {
            if (dia.getHoras() != null) {
                totalHoras += dia.getHoras();
            }
        }

        //valida el tamano de lo introducido en los campos de texto
        if (form.getSupervisor() != null && form.getSupervisor().length() > 100) {
            throw new RuntimeException("El nombre del supervisor supera el máximo de 100 caracteres");
        }

        if (form.getConocimientos() != null && form.getConocimientos().length() > 265) {
            throw new RuntimeException("El campo Conocimientos supera el máximo de 265 caracteres");
        }

        if (form.getExperiencia() != null && form.getExperiencia().length() > 200) {
            throw new RuntimeException("El campo Experiencia supera el máximo de 200 caracteres");
        }

        if (form.getAprendizaje() != null && form.getAprendizaje().length() > 200) {
            throw new RuntimeException("El campo Aprendizaje supera el máximo de 200 caracteres");
        }

        // 3. Armar y guardar la PlanillaSemanal (cabecera)
        PlanillaSemanal planilla = new PlanillaSemanal();
        planilla.setAlumno(alumno);
        planilla.setSupervisor(form.getSupervisor());
        planilla.setConocimientos(form.getConocimientos());
        planilla.setExperiencia(form.getExperiencia());
        planilla.setAprendizaje(form.getAprendizaje());
        planilla.setFechaDesde(fechaDesde);
        planilla.setFechaHasta(fechaHasta);
        planilla.setTotalHoras(totalHoras);

        planillaSemanalRepository.save(planilla);

        // 4. Armar y guardar cada detalle (los días trabajados)
        int contador = 1;
        for (DiaForm dia : diasCargados) {
            PlanillaSemanalDetalleId detalleId = new PlanillaSemanalDetalleId(planilla.getIdPs(), contador);

            PlanillaSemanalDetalle detalle = new PlanillaSemanalDetalle();
            detalle.setId(detalleId);
            detalle.setPlanillaSemanal(planilla);
            detalle.setFecha(dia.getFecha());
            detalle.setDescripcion(dia.getDescripcion());
            detalle.setHoras(dia.getHoras());

            planillaSemanalDetalleRepository.save(detalle);

            contador++;
        }
    }

    private void validarDia(DiaForm dia) {

        boolean tieneAlgunDato = dia.getFecha() != null 
                || dia.getHoras() != null 
                || (dia.getDescripcion() != null && !dia.getDescripcion().isBlank());

        if (!tieneAlgunDato) {
            return; // el día está completamente vacío, está bien, se ignora
        }

        // Si tiene AL MENOS un dato, entonces TODOS son obligatorios
        if (dia.getFecha() == null) {
            throw new RuntimeException("Falta la fecha en " + dia.getNombreDia());
        }

        if (dia.getHoras() == null) {
            throw new RuntimeException("Faltan las horas en " + dia.getNombreDia());
        }

        if (dia.getHoras() <= 0) {
            throw new RuntimeException("Las horas deben ser mayores a 0 en " + dia.getNombreDia());
        }

        if (dia.getDescripcion() == null || dia.getDescripcion().isBlank()) {
            throw new RuntimeException("Falta la descripción en " + dia.getNombreDia());
        }

        if (dia.getDescripcion() != null && dia.getDescripcion().length() > 500) {
            throw new RuntimeException("La descripción de " + dia.getNombreDia() + " supera el máximo de 500 caracteres");
        }
    }

    public PlanillaSemanalForm cargarParaEdicion(PlanillaSemanal planilla) {
        PlanillaSemanalForm form = new PlanillaSemanalForm(); // ya viene con Lunes..Sábado precargados

        form.setSupervisor(planilla.getSupervisor());
        form.setConocimientos(planilla.getConocimientos());
        form.setExperiencia(planilla.getExperiencia());
        form.setAprendizaje(planilla.getAprendizaje());

        List<PlanillaSemanalDetalle> detalles =
            planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(planilla.getIdPs());

        for (PlanillaSemanalDetalle detalle : detalles) {
            // getValue() de DayOfWeek: Lunes=1 ... Domingo=7, por eso el -1
            int indice = detalle.getFecha().getDayOfWeek().getValue() - 1;
            DiaForm dia = form.getDias().get(indice);
            dia.setFecha(detalle.getFecha());
            dia.setDescripcion(detalle.getDescripcion());
            dia.setHoras(detalle.getHoras());
        }

        return form;
    }
}