package com.servihome.scgo.controlador;

import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContarPorEstadoTest {

    @Test
    void cuentaCadaEstadoSegunElOrdinal() {
        List<OrdenTrabajo> ordenes = new ArrayList<>();
        ordenes.add(orden(EstadoOrden.ASIGNADA));
        ordenes.add(orden(EstadoOrden.FINALIZADA));
        ordenes.add(orden(EstadoOrden.ASIGNADA));

        int[] contadores = OrdenController.contarPorEstado(ordenes);

        assertEquals(EstadoOrden.values().length, contadores.length);
        assertEquals(2, contadores[EstadoOrden.ASIGNADA.ordinal()]);
        assertEquals(1, contadores[EstadoOrden.FINALIZADA.ordinal()]);
        assertEquals(0, contadores[EstadoOrden.CANCELADA.ordinal()]);
    }

    @Test
    void listaVaciaDejaTodosLosContadoresEnCero() {
        int[] contadores = OrdenController.contarPorEstado(List.of());

        assertEquals(EstadoOrden.values().length, contadores.length);
        for (int cantidad : contadores) {
            assertEquals(0, cantidad);
        }
    }

    private static OrdenTrabajo orden(EstadoOrden estado) {
        return new OrdenTrabajo(
                1,
                LocalDate.of(2026, 6, 10),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                estado);
    }
}
