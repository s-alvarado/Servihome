package com.servihome.scgo.dao;

import com.servihome.scgo.enums.EstadoSolicitud;
import com.servihome.scgo.modelo.Solicitud;

public class SolicitudDAO implements DAO<Solicitud> {

    @Override
    public int insertar(Solicitud obj) {
        // TODO: implementar INSERT con PreparedStatement
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Solicitud buscarPorId(int id) {
        // TODO: implementar SELECT por id_solicitud
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void actualizar(Solicitud obj) {
        // TODO: implementar UPDATE completo de solicitud
        throw new UnsupportedOperationException("TODO");
    }

    public void actualizarEstado(int idSolicitud, EstadoSolicitud estado) {
        // TODO: implementar UPDATE estado (CU006 → PROGRAMADA)
        throw new UnsupportedOperationException("TODO");
    }
}
