package com.servihome.scgo.dao;

import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;

public class OrdenTrabajoDAO implements DAO<OrdenTrabajo> {

    @Override
    public int insertar(OrdenTrabajo obj) {
        // TODO: implementar INSERT con PreparedStatement; devolver id generado (CU006)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public OrdenTrabajo buscarPorId(int id) {
        // TODO: implementar SELECT por id_orden (CU007: obtener franja horaria)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void actualizar(OrdenTrabajo obj) {
        // TODO: implementar UPDATE completo de orden
        throw new UnsupportedOperationException("TODO");
    }

    public void actualizarEstado(int idOrden, EstadoOrden estado) {
        // TODO: implementar UPDATE estado (CU008 → FINALIZADA)
        throw new UnsupportedOperationException("TODO");
    }
}
