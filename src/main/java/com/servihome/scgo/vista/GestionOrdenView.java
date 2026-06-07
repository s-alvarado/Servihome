package com.servihome.scgo.vista;

import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.modelo.CierreOrden;

public class GestionOrdenView {

    private final OrdenController controller;

    public GestionOrdenView(OrdenController controller) {
        this.controller = controller;
    }

    /** CU006: dispara generarOrden. */
    public void seleccionarSolicitud(int idSolicitud) {
        // TODO: invocar controller.generarOrden y mostrar resultado
        throw new UnsupportedOperationException("TODO");
    }

    /** CU007: dispara validarYAsignar; feedback de exito o solapamiento. */
    public void asignarRecurso(int idOrden, int idRecurso, TipoRecurso tipo) {
        // TODO: invocar controller.validarYAsignar; mostrarResultado segun resultado
        throw new UnsupportedOperationException("TODO");
    }

    /** CU008: dispara cerrarOrden. */
    public void registrarCierre(int idOrden, CierreOrden cierre) {
        // TODO: invocar controller.cerrarOrden
        throw new UnsupportedOperationException("TODO");
    }

    public void mostrarResultado(String mensaje) {
        // TODO: implementar feedback al usuario (consola o GUI)
        throw new UnsupportedOperationException("TODO");
    }
}
