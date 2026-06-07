package com.servihome.scgo.controlador;

import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;

import java.time.LocalDate;
import java.time.LocalTime;

public class OrdenController {

    private final OrdenTrabajoDAO ordenDAO;
    private final SolicitudDAO solicitudDAO;
    private final AsignacionDAO asignacionDAO;
    private final CierreOrdenDAO cierreDAO;

    public OrdenController(OrdenTrabajoDAO ordenDAO,
                           SolicitudDAO solicitudDAO,
                           AsignacionDAO asignacionDAO,
                           CierreOrdenDAO cierreDAO) {
        this.ordenDAO = ordenDAO;
        this.solicitudDAO = solicitudDAO;
        this.asignacionDAO = asignacionDAO;
        this.cierreDAO = cierreDAO;
    }

    /** CU006: generar orden a partir de solicitud existente. */
    public OrdenTrabajo generarOrden(int idSolicitud) {
        // TODO: buscar solicitud; crear OrdenTrabajo (ASIGNADA); actualizar solicitud a PROGRAMADA
        // TODO: origen de fechaServicio/horaInicio/horaFin no especificado en briefing
        throw new UnsupportedOperationException("TODO");
    }

    /** CU007: validar recurso operativo (R2) y solapamiento (R1); registrar si OK. */
    public boolean validarYAsignar(int idOrden, int idRecurso, TipoRecurso tipo) {
        // TODO: obtener franja de la orden; verificar recurso operativo; existeSolapamiento; registrar
        // TODO: no hay DAO de Personal/Activo en briefing — definir acceso para R2
        throw new UnsupportedOperationException("TODO");
    }

    /** Delega en AsignacionDAO la regla R1 (desigualdad estricta). */
    public boolean verificarSolapamiento(int idRecurso, TipoRecurso tipo,
                                           LocalDate fecha, LocalTime ini, LocalTime fin) {
        // TODO: delegar en asignacionDAO.existeSolapamiento
        throw new UnsupportedOperationException("TODO");
    }

    /** CU008: cierre transaccional (R4). */
    public void cerrarOrden(int idOrden, CierreOrden cierre) {
        // TODO: setAutoCommit(false) → cierreDAO.insertar → ordenDAO.actualizarEstado(FINALIZADA) → commit/rollback
        // TODO: validar R5 (averia + detalleAveria) en capa de aplicacion
        throw new UnsupportedOperationException("TODO");
    }
}
