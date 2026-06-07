package com.servihome.scgo.controlador;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.dao.ActivoDAO;
import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.DaoException;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.PersonalDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.enums.EstadoSolicitud;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.modelo.Activo;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.Franja;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.modelo.Personal;
import com.servihome.scgo.modelo.Solicitud;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class OrdenController {

    private final OrdenTrabajoDAO ordenDAO;
    private final SolicitudDAO solicitudDAO;
    private final AsignacionDAO asignacionDAO;
    private final CierreOrdenDAO cierreDAO;
    private final PersonalDAO personalDAO;
    private final ActivoDAO activoDAO;

    public OrdenController(OrdenTrabajoDAO ordenDAO,
                           SolicitudDAO solicitudDAO,
                           AsignacionDAO asignacionDAO,
                           CierreOrdenDAO cierreDAO,
                           PersonalDAO personalDAO,
                           ActivoDAO activoDAO) {
        this.ordenDAO = ordenDAO;
        this.solicitudDAO = solicitudDAO;
        this.asignacionDAO = asignacionDAO;
        this.cierreDAO = cierreDAO;
        this.personalDAO = personalDAO;
        this.activoDAO = activoDAO;
    }

    /** CU006: generar orden a partir de solicitud existente (requiere franja horaria). */
    public OrdenTrabajo generarOrden(int idSolicitud) {
        // TODO: RF006 no especifica origen de fechaServicio/horaInicio/horaFin
        throw new IllegalStateException(
                "Franja horaria no definida en RF006. "
                        + "Use generarOrden(idSolicitud, fechaServicio, horaInicio, horaFin)");
    }

    /** CU006 con programacion explicita. Transaccional: orden + cambio de estado solicitud. */
    public OrdenTrabajo generarOrden(int idSolicitud, LocalDate fechaServicio,
                                     LocalTime horaInicio, LocalTime horaFin) {
        Solicitud solicitud = solicitudDAO.buscarPorId(idSolicitud);
        if (solicitud == null) {
            throw new IllegalArgumentException("Solicitud no encontrada: id=" + idSolicitud);
        }
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalArgumentException(
                    "La solicitud debe estar PENDIENTE. Estado actual: " + solicitud.getEstado());
        }
        validarRangoHorario(horaInicio, horaFin);

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setIdSolicitud(idSolicitud);
        orden.setFechaServicio(fechaServicio);
        orden.setHoraInicio(horaInicio);
        orden.setHoraFin(horaFin);
        orden.setEstado(EstadoOrden.ASIGNADA);

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            ordenDAO.insertar(orden, conn);
            solicitudDAO.actualizarEstado(idSolicitud, EstadoSolicitud.PROGRAMADA, conn);
            conn.commit();
            return orden;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw new DaoException("Error en rollback al generar orden", rollbackEx);
                }
            }
            throw new DaoException("Error transaccional al generar orden", e);
        } finally {
            cerrarConexion(conn);
        }
    }

    /** CU007: validar recurso operativo (R2) y solapamiento (R1); registrar si OK. */
    public boolean validarYAsignar(int idOrden, int idRecurso, TipoRecurso tipo) {
        OrdenTrabajo orden = ordenDAO.buscarPorId(idOrden);
        if (orden == null || orden.getEstado() != EstadoOrden.ASIGNADA) {
            return false;
        }

        if (!recursoOperativo(idRecurso, tipo)) {
            return false;
        }

        Franja franja = orden.getFranjaHoraria();
        if (asignacionDAO.existeSolapamientoEnOtraOrden(
                idOrden, idRecurso, tipo, franja.getFecha(), franja.getInicio(), franja.getFin())) {
            return false;
        }

        if (tipo == TipoRecurso.PERSONAL) {
            asignacionDAO.registrarPersonal(idOrden, idRecurso);
        } else {
            asignacionDAO.registrarActivo(idOrden, idRecurso);
        }
        return true;
    }

    /** Delega en AsignacionDAO la regla R1 (desigualdad estricta). */
    public boolean verificarSolapamiento(int idRecurso, TipoRecurso tipo,
                                           LocalDate fecha, LocalTime ini, LocalTime fin) {
        return asignacionDAO.existeSolapamiento(idRecurso, tipo, fecha, ini, fin);
    }

    /** CU008: cierre transaccional (R4). */
    public void cerrarOrden(int idOrden, CierreOrden cierre) {
        OrdenTrabajo orden = ordenDAO.buscarPorId(idOrden);
        if (orden == null) {
            throw new IllegalArgumentException("Orden no encontrada: id=" + idOrden);
        }
        if (orden.getEstado() != EstadoOrden.ASIGNADA) {
            throw new IllegalArgumentException("Solo se puede cerrar una orden ASIGNADA");
        }
        validarCierre(cierre);

        cierre.setIdOrden(idOrden);

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            cierreDAO.insertar(cierre, conn);
            ordenDAO.actualizarEstado(idOrden, EstadoOrden.FINALIZADA, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw new DaoException("Error en rollback del cierre orden id=" + idOrden, rollbackEx);
                }
            }
            throw new DaoException("Error transaccional al cerrar orden id=" + idOrden, e);
        } finally {
            cerrarConexion(conn);
        }
    }

    public List<Solicitud> listarSolicitudesPendientes() {
        return solicitudDAO.listarPorEstado(EstadoSolicitud.PENDIENTE);
    }

    public List<OrdenTrabajo> listarOrdenesAsignadas() {
        return ordenDAO.listarPorEstado(EstadoOrden.ASIGNADA);
    }

    public List<Personal> listarPersonalOperativo() {
        return personalDAO.listarOperativos();
    }

    public List<Activo> listarActivosDisponibles() {
        return activoDAO.listarDisponibles();
    }

    public OrdenTrabajo buscarOrden(int idOrden) {
        return ordenDAO.buscarPorId(idOrden);
    }

    private boolean recursoOperativo(int idRecurso, TipoRecurso tipo) {
        if (tipo == TipoRecurso.PERSONAL) {
            Personal personal = personalDAO.buscarPorId(idRecurso);
            return personal != null && personal.estaOperativo();
        }
        Activo activo = activoDAO.buscarPorId(idRecurso);
        return activo != null && activo.estaDisponible();
    }

    private void validarRangoHorario(LocalTime horaInicio, LocalTime horaFin) {
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("R3: horaFin debe ser posterior a horaInicio");
        }
    }

    private void validarCierre(CierreOrden cierre) {
        if (cierre.getHorasReales() == null) {
            throw new IllegalArgumentException("Las horas reales son obligatorias");
        }
        if (cierre.getEstadoDevolucion() == null) {
            cierre.setEstadoDevolucion(EstadoDevolucion.BUEN_ESTADO);
        }
        if (cierre.isAveria()) {
            if (cierre.getDetalleAveria() == null || cierre.getDetalleAveria().isBlank()) {
                throw new IllegalArgumentException("R5: detalleAveria obligatorio cuando averia es true");
            }
            if (cierre.getEstadoDevolucion() != EstadoDevolucion.AVERIADO) {
                throw new IllegalArgumentException("R5: estadoDevolucion debe ser AVERIADO cuando hay averia");
            }
        }
    }

    private void cerrarConexion(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                throw new DaoException("Error al cerrar conexion JDBC", e);
            }
        }
    }
}
