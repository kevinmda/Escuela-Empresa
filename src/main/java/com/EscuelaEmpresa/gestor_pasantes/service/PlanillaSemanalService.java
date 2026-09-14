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

    // Tope absoluto de semanas: una pasantia dura como maximo 6. Si el alumno
    // ya acumulo las 240 horas objetivo antes de llegar a la sexta, dejar de
    // cargar semanas pasa a ser opcional (ver alcanzoObjetivoHoras), pero
    // nunca se puede superar este tope. Publico: lo usan los controllers
    // (web y movil) para saber cuando el requisito de semanas esta cumplido.
    public static final int MAX_SEMANAS = 6;

    // Horas totales que se esperan de la pasantia completa. Una vez que la
    // suma de "total_horas" de las semanas ya cargadas llega aca, ya no hace
    // falta completar las 6 semanas: alcanza con las que el alumno ya cargo.
    private static final BigDecimal HORAS_OBJETIVO_PASANTIA = new BigDecimal("240");

    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository;

    public PlanillaSemanalService(PlanillaSemanalRepository planillaSemanalRepository,
                                   PlanillaSemanalDetalleRepository planillaSemanalDetalleRepository) {
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.planillaSemanalDetalleRepository = planillaSemanalDetalleRepository;
    }

    @Transactional
    public PlanillaSemanal guardarPlanilla(PlanillaSemanalForm form, Alumno alumno) {

        // Maximo 6 planillas por alumno (una pasantia dura exactamente 6 semanas)
        List<PlanillaSemanal> planillasExistentes = planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        if (planillasExistentes.size() >= MAX_SEMANAS) {
            throw new ReglaNegocioException("Ya cargaste las 6 semanas de planilla. No se pueden cargar más.");
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

        // 1. Filtrar solo los días que el alumno realmente cargó (fecha no vacía)
        List<DiaForm> diasCargados = form.getDias().stream()
                .filter(dia -> dia.getFecha() != null)
                .toList();

        if (diasCargados.isEmpty()) {
            throw new ReglaNegocioException("Debe cargar al menos un día trabajado");
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

        BigDecimal totalHoras = sumarHoras(diasCargados);

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

    // El campo del formulario acepta step 1 y la columna es DECIMAL(5,0): se
    // redondea aca, de forma explicita, en vez de dejar que MySQL lo haga en silencio.
    // En circunstancias normales ya llega entero (ver validarDia), pero esto es la
    // ultima garantia antes de guardar.
    static BigDecimal normalizarHoras(BigDecimal horas) {
        return horas.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Suma el total_horas de todas las semanas ya cargadas por el alumno.
     */
    public BigDecimal calcularHorasAcumuladas(List<PlanillaSemanal> planillas) {
        BigDecimal total = BigDecimal.ZERO;
        for (PlanillaSemanal planilla : planillas) {
            if (planilla.getTotalHoras() != null) {
                total = total.add(planilla.getTotalHoras());
            }
        }
        return total;
    }

    /**
     * True si con las semanas ya cargadas el alumno ya llegó a las 240 horas
     * de la pasantía. A partir de ahí cargar otra semana pasa a ser
     * opcional: no hace falta llegar a las 6 para habilitar el informe.
     */
    public boolean alcanzoObjetivoHoras(List<PlanillaSemanal> planillas) {
        return calcularHorasAcumuladas(planillas).compareTo(HORAS_OBJETIVO_PASANTIA) >= 0;
    }

    /**
     * Cuántas semanas le hacían falta a este alumno para el expediente: si ya
     * alcanzó las 240 horas antes de llegar a MAX_SEMANAS, son las que ya
     * cargó (puede ser 5); si no, sigue siendo MAX_SEMANAS. La usa
     * LimitesDocumentoService para saber cuántos PDF de "Plantilla Semanal"
     * tiene que subir este alumno en particular.
     */
    public int obtenerSemanasNecesarias(Integer idAlumno) {
        List<PlanillaSemanal> planillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(idAlumno);
        if (planillas.size() >= MAX_SEMANAS) {
            return MAX_SEMANAS;
        }
        if (alcanzoObjetivoHoras(planillas)) {
            return planillas.size();
        }
        return MAX_SEMANAS;
    }

    private void validarOrdenYRango(List<DiaForm> diasCargados) {
        // Opción 1: orden cronológico -- cada día cargado tiene que tener una fecha
        // posterior al día anterior (evita "Martes antes que Lunes")
        for (int i = 1; i < diasCargados.size(); i++) {
            LocalDate anterior = diasCargados.get(i - 1).getFecha();
            LocalDate actual = diasCargados.get(i).getFecha();
            if (!actual.isAfter(anterior)) {
                throw new ReglaNegocioException("Las fechas cargadas no siguen el orden correcto de los días de la semana.");
            }
        }

        // Opción 2: rango máximo de 5 días (Lunes a Sábado) -- evita que las fechas
        // cargadas pertenezcan a semanas distintas, aunque estén en el orden correcto
        LocalDate minFecha = diasCargados.get(0).getFecha();
        LocalDate maxFecha = diasCargados.get(diasCargados.size() - 1).getFecha();
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

        boolean tieneAlgunDato = dia.getFecha() != null 
                || dia.getHoras() != null 
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

        // Las horas se cargan enteras, sin fracciones (ni 7.5 ni 7:30).
        if (dia.getHoras().remainder(BigDecimal.ONE).signum() != 0) {
            throw new ReglaNegocioException("Las horas de " + dia.getNombreDia() + " deben ser un número entero, sin decimales.");
        }

        // Sábados se trabaja media jornada: máximo 8 horas contra las 10 de
        // lunes a viernes.
        BigDecimal maxHoras = diaEsperado == DayOfWeek.SATURDAY
                ? new BigDecimal("8")
                : new BigDecimal("10");
        if (dia.getHoras().compareTo(maxHoras) > 0) {
            throw new ReglaNegocioException("Las horas de " + dia.getNombreDia() + " no pueden superar las "
                    + maxHoras + " (máximo " + (diaEsperado == DayOfWeek.SATURDAY ? "8 horas los sábados" : "10 horas de lunes a viernes") + ").");
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