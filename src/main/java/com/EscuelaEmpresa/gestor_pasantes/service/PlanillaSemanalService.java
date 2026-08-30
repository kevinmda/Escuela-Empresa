package com.EscuelaEmpresa.gestor_pasantes.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void guardarPlanilla(PlanillaSemanalForm form, Alumno alumno) {

        // Maximo 6 planillas por alumno (una pasantia dura exactamente 6 semanas)
        List<PlanillaSemanal> planillasExistentes = planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        if (planillasExistentes.size() >= 6) {
            throw new RuntimeException("Ya cargaste las 6 semanas de planilla. No se pueden cargar más.");
        }

        // Validar cada día ANTES de filtrar
        DayOfWeek[] diasEsperados = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };

        // Validar cada día ANTES de filtrar
        List<DiaForm> dias = form.getDias();
        if (dias == null || dias.size() != diasEsperados.length) {
            throw new RuntimeException("La planilla debe contener los seis días de la semana");
        }
        for (int i = 0; i < dias.size(); i++) {
            validarDia(dias.get(i), diasEsperados[i]);
        }

        // 1. Filtrar solo los días que el alumno realmente cargó (fecha no vacía)
        List<DiaForm> diasCargados = form.getDias().stream()
                .filter(dia -> dia.getFecha() != null)
                .toList();

        if (diasCargados.isEmpty()) {
            throw new RuntimeException("Debe cargar al menos un día trabajado");
        }

        validarOrdenYRango(diasCargados);

        // 2. Calcular fecha_desde, fecha_hasta y total_horas
        LocalDate fechaDesde = diasCargados.stream()
                .map(DiaForm::getFecha)
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate fechaHasta = diasCargados.stream()
                .map(DiaForm::getFecha)
                .max(LocalDate::compareTo)
                .orElseThrow();

        validarSuperposicion(fechaDesde, fechaHasta, planillasExistentes);

        float totalHoras = 0f;
        for (DiaForm dia : diasCargados) {
            if (dia.getHoras() != null) {
                totalHoras += dia.getHoras();
            }
        }

        //valida el tamano de lo introducido en los campos de texto
        if (form.getSupervisor() == null || form.getSupervisor().trim().isEmpty()) {
        throw new RuntimeException("El campo Supervisor es obligatorio.");
        }
        if (form.getConocimientos() == null || form.getConocimientos().trim().isEmpty()) {
            throw new RuntimeException("El campo Conocimientos es obligatorio.");
        }
        if (form.getExperiencia() == null || form.getExperiencia().trim().isEmpty()) {
            throw new RuntimeException("El campo Experiencia es obligatorio.");
        }
        if (form.getAprendizaje() == null || form.getAprendizaje().trim().isEmpty()) {
            throw new RuntimeException("El campo Aprendizaje es obligatorio.");
        }

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

    @Transactional
    public void eliminarPlanilla(PlanillaSemanal planilla) {
        // primero los detalles (dias trabajados), porque no hay cascade configurado
        // en la relacion, y despues la cabecera
        List<PlanillaSemanalDetalle> detalles =
            planillaSemanalDetalleRepository.findByPlanillaSemanal_IdPs(planilla.getIdPs());
        planillaSemanalDetalleRepository.deleteAll(detalles);
        planillaSemanalRepository.delete(planilla);
    }

    private void validarOrdenYRango(List<DiaForm> diasCargados) {
        // Opción 1: orden cronológico -- cada día cargado tiene que tener una fecha
        // posterior al día anterior (evita "Martes antes que Lunes")
        for (int i = 1; i < diasCargados.size(); i++) {
            LocalDate anterior = diasCargados.get(i - 1).getFecha();
            LocalDate actual = diasCargados.get(i).getFecha();
            if (!actual.isAfter(anterior)) {
                throw new RuntimeException("Las fechas cargadas no siguen el orden correcto de los días de la semana.");
            }
        }

        // Opción 2: rango máximo de 5 días (Lunes a Sábado) -- evita que las fechas
        // cargadas pertenezcan a semanas distintas, aunque estén en el orden correcto
        LocalDate minFecha = diasCargados.get(0).getFecha();
        LocalDate maxFecha = diasCargados.get(diasCargados.size() - 1).getFecha();
        if (java.time.temporal.ChronoUnit.DAYS.between(minFecha, maxFecha) > 5) {
            throw new RuntimeException("Las fechas cargadas abarcan más de una semana. Revisá que todas correspondan a la misma semana.");
        }
    }

    private void validarSuperposicion(LocalDate fechaDesde, LocalDate fechaHasta, List<PlanillaSemanal> planillasExistentes) {
        // Opción 5: que la semana nueva no se superponga con ninguna semana ya cargada
        for (PlanillaSemanal existente : planillasExistentes) {
            boolean seSuperponen = !fechaHasta.isBefore(existente.getFechaDesde()) && !fechaDesde.isAfter(existente.getFechaHasta());
            if (seSuperponen) {
                throw new RuntimeException("Las fechas se superponen con una planilla ya cargada (semana del "
                        + existente.getFechaDesde() + " al " + existente.getFechaHasta() + ").");
            }
        }
    }

    private void validarDia(DiaForm dia, DayOfWeek diaEsperado) {

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

        if (dia.getDescripcion() == null || dia.getDescripcion().isBlank()) {
            throw new RuntimeException("Falta la descripción en " + dia.getNombreDia());
        }

        if (dia.getHoras() == null) {
            throw new RuntimeException("Faltan las horas en " + dia.getNombreDia());
        }

        if (dia.getDescripcion() != null && dia.getDescripcion().length() > 65) {
            throw new RuntimeException("La descripción de " + dia.getNombreDia() + " supera el máximo de 65 caracteres");
        }

        if (dia.getHoras() <= 0) {
            throw new RuntimeException("Las horas deben ser mayores a 0 en " + dia.getNombreDia());
        }

        if (dia.getFecha() != null && dia.getFecha().getDayOfWeek() != diaEsperado) {
            throw new RuntimeException(
                "La fecha ingresada en la fila '" + dia.getNombreDia() + "' no corresponde a ese día de la semana. " +
                "Verificá el calendario e intentá de nuevo."
            );
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