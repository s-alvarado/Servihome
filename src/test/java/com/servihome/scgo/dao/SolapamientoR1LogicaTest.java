package com.servihome.scgo.dao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CPF02 (sin BD): valida la regla R1 en memoria.
 * Orden existente fija: 08:00-12:00. Misma tabla de verdad del briefing seccion 6.
 */
class SolapamientoR1LogicaTest {

    private static final LocalTime EXISTENTE_INI = LocalTime.of(8, 0);
    private static final LocalTime EXISTENTE_FIN = LocalTime.of(12, 0);

    @ParameterizedTest(name = "franja {0}-{1} bloquea={2}")
    @CsvSource({
            "06:00, 07:59, false",
            "06:00, 08:00, false",
            "06:00, 08:01, true",
            "09:00, 11:00, true",
            "11:59, 14:00, true",
            "12:00, 14:00, false",
            "12:01, 14:00, false",
            "07:00, 13:00, true"
    })
    void cpf02_reglaSolapamiento_desigualdadEstricta(String horaInicio, String horaFin, boolean bloquea) {
        LocalTime nuevoIni = LocalTime.parse(horaInicio);
        LocalTime nuevoFin = LocalTime.parse(horaFin);

        boolean haySolapamiento = seSolapan(EXISTENTE_INI, EXISTENTE_FIN, nuevoIni, nuevoFin);

        assertEquals(bloquea, haySolapamiento,
                "Franja " + horaInicio + "-" + horaFin + " vs existente 08:00-12:00");
    }

    /** R1: ini_existente &lt; fin_nuevo AND fin_existente &gt; ini_nuevo */
    static boolean seSolapan(LocalTime existenteIni, LocalTime existenteFin,
                           LocalTime nuevoIni, LocalTime nuevoFin) {
        return existenteIni.isBefore(nuevoFin) && existenteFin.isAfter(nuevoIni);
    }
}
