package com.EscuelaEmpresa.gestor_pasantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.EscuelaEmpresa.gestor_pasantes.dto.DiaForm;

class PlanillaSemanalServiceTest {

    private static DiaForm dia(String horas) {
        DiaForm dia = new DiaForm("Lunes");
        dia.setHoras(horas == null ? null : new BigDecimal(horas));
        return dia;
    }

    @Test
    void laSumaDeHorasEsExacta() {
        BigDecimal total = PlanillaSemanalService.sumarHoras(
                List.of(dia("7"), dia("8"), dia("1")));

        assertEquals(new BigDecimal("16"), total);
    }

    @Test
    void losDiasSinHorasNoRompenLaSuma() {
        BigDecimal total = PlanillaSemanalService.sumarHoras(List.of(dia("4"), dia(null), dia("4")));

        assertEquals(new BigDecimal("8"), total);
    }

    @Test
    void lasHorasConDecimalesSeRedondeanAntesDeGuardar() {
        assertEquals(new BigDecimal("8"), PlanillaSemanalService.normalizarHoras(new BigDecimal("7.5")));
        assertEquals(new BigDecimal("8"), PlanillaSemanalService.normalizarHoras(new BigDecimal("8")));
    }
}
