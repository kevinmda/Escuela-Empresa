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
    void laSumaDeHorasEsExactaConDosDecimales() {
        // 7.5 + 8.25 + 0.1 + 0.2 en float daba 16.049999; en BigDecimal da 16.05 exacto
        BigDecimal total = PlanillaSemanalService.sumarHoras(
                List.of(dia("7.5"), dia("8.25"), dia("0.1"), dia("0.2")));

        assertEquals(new BigDecimal("16.05"), total);
    }

    @Test
    void losDiasSinHorasNoRompenLaSuma() {
        BigDecimal total = PlanillaSemanalService.sumarHoras(List.of(dia("4"), dia(null), dia("4")));

        assertEquals(new BigDecimal("8.00"), total);
    }

    @Test
    void masDeDosDecimalesSeRedondeanAntesDeGuardar() {
        assertEquals(new BigDecimal("7.13"), PlanillaSemanalService.normalizarHoras(new BigDecimal("7.125")));
        assertEquals(new BigDecimal("8.00"), PlanillaSemanalService.normalizarHoras(new BigDecimal("8")));
    }
}
