package com.servihome.scgo.dao;

import com.servihome.scgo.enums.TipoRecurso;

import java.time.LocalDate;
import java.time.LocalTime;

public class AsignacionDAO {

    /**
     * R1: solapamiento si ini_existente &lt; fin_nuevo AND fin_existente &gt; ini_nuevo
     * (desigualdad estricta; turnos contiguos no solapan).
     * Solo ordenes en estado ASIGNADA.
     */
    public boolean existeSolapamiento(int idRecurso, TipoRecurso tipo,
                                      LocalDate fecha, LocalTime ini, LocalTime fin) {
        // TODO: implementar consulta sobre asignacion_personal o asignacion_activo
        throw new UnsupportedOperationException("TODO");
    }

    public void registrarPersonal(int idOrden, int idPersonal) {
        // TODO: implementar INSERT en asignacion_personal
        throw new UnsupportedOperationException("TODO");
    }

    public void registrarActivo(int idOrden, int idActivo) {
        // TODO: implementar INSERT en asignacion_activo
        throw new UnsupportedOperationException("TODO");
    }
}
