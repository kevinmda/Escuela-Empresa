package com.EscuelaEmpresa.gestor_pasantes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
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
    public PlanillaSemanal guardarPlanilla(PlanillaSemanalForm form, Alumno alumno) {

        List<PlanillaSemanal> planillasExistentes = planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

        // idPsEdicion viene del campo oculto que arma habilitarEdicion() en el
        // JS: si tiene algo, se está actualizando esa planilla en vez de crear
        // una nueva. Se saca de planillasExistentes ANTES de las validaciones
        // de rango/superposición más abajo: si no, una planilla siempre se
        // superpone consigo misma, y editarla sin cambiar las fechas
        // rechazaría el guardado.
        boolean esEdicion = form.getIdPsEdicion() != null && !form.getIdPsEdicion().isBlank();
        PlanillaSemanal planilla;

        if (esEdicion) {
            Integer idPsEdicion = Integer.valueOf(form.getIdPsEdicion());
            planilla = planillasExistentes.stream()
                    .filter(p -> p.getIdPs().equals(idPsEdicion))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException(
                            "La planilla que intentás editar ya no existe. Volvé a intentarlo."));
            planillasExistentes = planillasExistentes.stream()
                    .filter(p -> !p.getIdPs().equals(idPsEdicion))
                    .toList();
        } else {
            // Maximo 6 planillas por alumno (una pasantia dura exactamente 6 semanas).
            // No aplica al editar: una edición no agrega una semana nueva.
            if (planillasExistentes.size() >= 6) {
                throw new ReglaNegocioException("Ya cargaste las 6 semanas de planilla. No se pueden cargar más.");
            }
            planilla = new PlanillaSemanal();
            planilla.setAlumno(alumno);
        }

        // Validar cada día ANTES de filtrar
        DayOfWeek[] diasEsperados = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };

        // Validar cada día ANTES de filtrar
        List<DiaForm> dias = form.getDias();
        if (dias == null || dias.size() != diasEsperados.length) {
            throw new ReglaNegocioException("La planilla debe contener los seis días de la semana");
        }
        for (int i = 0; i < dias.size(); i++) {
            validarDia(dias.get(i), diasEsperados[i]);
        }

        // Los días que el alumno realmente trabajó son los que tienen horas
        // cargadas (validarDia ya garantiza que si hay horas, también hay
        // descripción y fecha). Todo lo que define la planilla -- el rango de
        // fechas, el orden, la superposición con otras semanas, el total de
        // horas y los detalles que se guardan -- sale de estos días, no de los
        // seis: las cinco fechas que no son la ancla se autocompletan solas y
        // no dicen nada sobre si el alumno trabajó ese día.
        List<DiaForm> diasTrabajados = form.getDias().stream()
                .filter(dia -> dia.getHoras() != null)
                .toList();

        if (diasTrabajados.isEmpty()) {
            throw new ReglaNegocioException("Debe cargar al menos un día trabajado");
        }

        validarOrdenYRango(diasTrabajados);

        // 2. Calcular fecha_desde, fecha_hasta y total_horas
        LocalDate fechaDesde = diasTrabajados.stream()
                .map(DiaForm::getFecha)
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate fechaHasta = diasTrabajados.stream()
                .map(DiaForm::getFecha)
                .max(LocalDate::compareTo)
                .orElseThrow();

        validarSuperposicion(fechaDesde, fechaHasta, planillasExistentes);

        BigDecimal totalHoras = sumarHoras(diasTrabajados);

        //valida el tamano de lo introducido en los campos de texto
        if (form.getSupervisor() == null || form.getSupervisor().trim().isEmpty()) {
        throw new ReglaNegocioException("El campo Supervisor es obligatorio.");
        }
        if (form.getConocimientos() == null || form.getConocimientos().trim().isEmpty()) {
            throw new ReglaNegocioException("El campo Conocimientos es obligatorio.");
        }
        if (form.getExperiencia() == null || form.getExperiencia().trim().isEmpty()) {
            throw new ReglaNegocioException("El campo Experiencia es obligatorio.");
        }
        if (form.getAprendizaje() == null || form.getAprendizaje().trim().isEmpty()) {
            throw new ReglaNegocioException("El campo Aprendizaje es obligatorio.");
        }

        if (form.getSupervisor() != null && form.getSupervisor().length() > 100) {
            throw new ReglaNegocioException("El nombre del supervisor supera el máximo de 100 caracteres");
        }

        if (form.getConocimientos() != null && form.getConocimientos().length() > 265) {
            throw new ReglaNegocioException("El campo Conocimientos supera el máximo de 265 caracteres");
        }

        if (form.getExperiencia() != null && form.getExperiencia().length() > 200) {
            throw new ReglaNegocioException("El campo Experiencia supera el máximo de 200 caracteres");
        }

        if (form.getAprendizaje() != null && form.getAprendizaje().length() > 200) {
            throw new ReglaNegocioException("El campo Aprendizaje supera el máximo de 200 caracteres");
        }

        // 3. Completar y guardar la PlanillaSemanal (cabecera)
        planilla.setSupervisor(form.getSupervisor());
        planilla.setConocimientos(form.getConocimientos());
        planilla.setExperiencia(form.getExperiencia());
        planilla.setAprendizaje(form.getAprendizaje());
        planilla.setFechaDesde(fechaDesde);
        planilla.setFechaHasta(fechaHasta);
        planilla.setTotalHoras(totalHoras);

        planillaSemanalRepository.save(planilla);

        // 4. Armar y guardar cada detalle (los días efectivamente trabajados).
        // Al editar, se reemplazan enteros: más simple y más confiable que
        // tratar de encontrar cuál día cambió, cuál se agregó y cuál se sacó.
        if (esEdicion) {
            planillaSemanalDetalleRepository.deleteByPlanillaSemanal_IdPs(planilla.getIdPs());
        }

        int contador = 1;
        for (DiaForm dia : diasTrabajados) {
            PlanillaSemanalDetalleId detalleId = new PlanillaSemanalDetalleId(planilla.getIdPs(), contador);

            PlanillaSemanalDetalle detalle = new PlanillaSemanalDetalle();
            detalle.setId(detalleId);
            detalle.setPlanillaSemanal(planilla);
            detalle.setFecha(dia.getFecha());
            detalle.setDescripcion(dia.getDescripcion());
            detalle.setHoras(normalizarHoras(dia.getHoras()));

            planillaSemanalDetalleRepository.save(detalle);

            contador++;
        }

        return planilla;
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

    // La suma se hace en BigDecimal con dos decimales fijos. Con float, 7.5 + 8.25
    // podia dar 15.749999 y eso era lo que terminaba en la base y en el PDF.
    static BigDecimal sumarHoras(List<DiaForm> dias) {
        BigDecimal total = BigDecimal.ZERO;
        for (DiaForm dia : dias) {
            if (dia.getHoras() != null) {
                total = total.add(normalizarHoras(dia.getHoras()));
            }
        }
        return total;
    }

    // El campo del formulario acepta step 0.01 y la columna es DECIMAL(5,2): se
    // redondea aca, de forma explicita, en vez de dejar que MySQL lo haga en silencio.
    static BigDecimal normalizarHoras(BigDecimal horas) {
        return horas.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarOrdenYRango(List<DiaForm> diasTrabajados) {
        // Opción 1: orden cronológico -- cada día cargado tiene que tener una fecha
        // posterior al día anterior (evita "Martes antes que Lunes")
        for (int i = 1; i < diasTrabajados.size(); i++) {
            LocalDate anterior = diasTrabajados.get(i - 1).getFecha();
            LocalDate actual = diasTrabajados.get(i).getFecha();
            if (!actual.isAfter(anterior)) {
                throw new ReglaNegocioException("Las fechas cargadas no siguen el orden correcto de los días de la semana.");
            }
        }

        // Opción 2: rango máximo de 5 días (Lunes a Sábado) -- evita que las fechas
        // cargadas pertenezcan a semanas distintas, aunque estén en el orden correcto
        LocalDate minFecha = diasTrabajados.get(0).getFecha();
        LocalDate maxFecha = diasTrabajados.get(diasTrabajados.size() - 1).getFecha();
        if (java.time.temporal.ChronoUnit.DAYS.between(minFecha, maxFecha) > 5) {
            throw new ReglaNegocioException("Las fechas cargadas abarcan más de una semana. Revisá que todas correspondan a la misma semana.");
        }
    }

    private void validarSuperposicion(LocalDate fechaDesde, LocalDate fechaHasta, List<PlanillaSemanal> planillasExistentes) {
        // Opción 5: que la semana nueva no se superponga con ninguna semana ya cargada
        for (PlanillaSemanal existente : planillasExistentes) {
            boolean seSuperponen = !fechaHasta.isBefore(existente.getFechaDesde()) && !fechaDesde.isAfter(existente.getFechaHasta());
            if (seSuperponen) {
                throw new ReglaNegocioException("Las fechas se superponen con una planilla ya cargada (semana del "
                        + existente.getFechaDesde() + " al " + existente.getFechaHasta() + ").");
            }
        }
    }

    private void validarDia(DiaForm dia, DayOfWeek diaEsperado) {

        // La fecha ya no sirve como señal de "este día tiene datos": con el
        // esquema de fecha ancla, las otras cinco se autocompletan solas y
        // siempre llegan con un valor, aunque el alumno no haya trabajado ese
        // día. Lo único que dice si el alumno quiso cargar el día es si puso
        // horas o descripción.
        boolean tieneAlgunDato = dia.getHoras() != null
                || (dia.getDescripcion() != null && !dia.getDescripcion().isBlank());

        if (!tieneAlgunDato) {
            return; // el día está completamente vacío, está bien, se ignora
        }

        // Si tiene AL MENOS un dato, entonces TODOS son obligatorios
        if (dia.getFecha() == null) {
            throw new ReglaNegocioException("Falta la fecha en " + dia.getNombreDia());
        }

        if (dia.getDescripcion() == null || dia.getDescripcion().isBlank()) {
            throw new ReglaNegocioException("Falta la descripción en " + dia.getNombreDia());
        }

        if (dia.getHoras() == null) {
            throw new ReglaNegocioException("Faltan las horas en " + dia.getNombreDia());
        }

        if (dia.getDescripcion() != null && dia.getDescripcion().length() > 65) {
            throw new ReglaNegocioException("La descripción de " + dia.getNombreDia() + " supera el máximo de 65 caracteres");
        }

        if (dia.getHoras().signum() <= 0) {
            throw new ReglaNegocioException("Las horas deben ser mayores a 0 en " + dia.getNombreDia());
        }

        if (dia.getFecha() != null && dia.getFecha().getDayOfWeek() != diaEsperado) {
            throw new ReglaNegocioException(
                "La fecha ingresada en la fila '" + dia.getNombreDia() + "' no corresponde a ese día de la semana. " +
                "Verificá el calendario e intentá de nuevo."
            );
        }
    }

    public PlanillaSemanalForm cargarParaEdicion(PlanillaSemanal planilla) {
        PlanillaSemanalForm form = new PlanillaSemanalForm(); // ya viene con Lunes..Sábado precargados
        form.setIdPsEdicion(String.valueOf(planilla.getIdPs()));

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