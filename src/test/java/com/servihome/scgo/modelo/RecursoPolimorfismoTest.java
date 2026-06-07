package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoPersonal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursoPolimorfismoTest {

    @Test
    void estaDisponible_polimorfismoSobreRecurso() {
        Recurso operativo = new Personal(1, "Carlos", 1, EstadoPersonal.ACTIVO);
        Recurso noOperativo = new Personal(2, "Luis", 2, EstadoPersonal.LICENCIA);

        assertTrue(operativo.estaDisponible());
        assertFalse(noOperativo.estaDisponible());
    }
}
