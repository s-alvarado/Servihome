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
import com.servihome.scgo.excepcion.SolapamientoException;
import com.servihome.scgo.modelo.Activo;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.Franja;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.modelo.Personal;
import com.servihome.scgo.modelo.Recurso;
import com.servihome.scgo.modelo.Solicitud;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Coordina los casos de uso del nucleo de gestion de ordenes (CU006 generar,
 * CU007 asignar/validar, CU008 cerrar). Orquesta los DAO y concentra aqui las
 * reglas de negocio y el control transaccional, manteniendo a la vista y a la
 * persistencia sin logica de dominio.
 */
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
            // Transaccion: crear la orden y pasar la solicitud a PROGRAMADA deben
            // ocurrir como una unidad. setAutoCommit(false) abre la transaccion para
            // que ambos cambios se confirmen juntos (commit) o ninguno (rollback) y
            // no quede una solicitud programada sin orden, ni viceversa.
            conn.setAutoCommit(false);
            ordenDAO.insertar(orden, conn);
            solicitudDAO.actualizarEstado(idSolicitud, EstadoSolicitud.PROGRAMADA, conn);
            conn.commit();
            return orden;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    // Ante cualquier fallo se revierte todo: la BD vuelve al estado previo.
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

    /**
     * CU007: valida recurso operativo (R2, polimorfismo sobre List&lt;Recurso&gt;) y solapamiento (R1).
     * @throws SolapamientoException si hay conflicto horario
     */
    public void validarYAsignar(int idOrden, int idRecurso, TipoRecurso tipo)
            throws SolapamientoException {
        OrdenTrabajo orden = ordenDAO.buscarPorId(idOrden);
        if (orden == null || orden.getEstado() != EstadoOrden.ASIGNADA) {
            throw new IllegalArgumentException("Orden no encontrada o no esta ASIGNADA: id=" + idOrden);
        }

        // Se trabaja sobre una List<Recurso> (tipo base). La validacion de
        // disponibilidad invoca estaDisponible() de forma uniforme, sin if por tipo:
        // el polimorfismo resuelve en tiempo de ejecucion la regla de Personal o Activo.
        // Agregar un nuevo tipo de recurso no obliga a tocar esta validacion.
        List<Recurso> recursos = listarRecursosPorTipo(tipo);
        Recurso recurso = buscarRecursoPorId(recursos, idRecurso);
        if (recurso == null) {
            throw new IllegalArgumentException("Recurso no encontrado: id=" + idRecurso);
        }
        if (!recursoEstaDisponibleEnLista(recursos, idRecurso)) {
            throw new IllegalArgumentException("Recurso no operativo: id=" + idRecurso);
        }

        Franja franja = orden.getFranjaHoraria();
        if (asignacionDAO.existeSolapamientoEnOtraOrden(
                idOrden, idRecurso, tipo, franja.getFecha(), franja.getInicio(), franja.getFin())) {
            throw new SolapamientoException(
                    "Solapamiento detectado para recurso id=" + idRecurso
                            + " en " + franja.getFecha() + " "
                            + franja.getInicio() + "-" + franja.getFin());
        }

        if (tipo == TipoRecurso.PERSONAL) {
            asignacionDAO.registrarPersonal(idOrden, idRecurso);
        } else {
            asignacionDAO.registrarActivo(idOrden, idRecurso);
        }
    }

    /** Delega en AsignacionDAO la regla R1 (desigualdad estricta). */
    public boolean verificarSolapamiento(int idRecurso, TipoRecurso tipo,
                                           LocalDate fecha, LocalTime ini, LocalTime fin) {
        return asignacionDAO.existeSolapamiento(idRecurso, tipo, fecha, ini, fin);
    }

    /**
     * CU008: cierre transaccional (R4). Registrar el cierre y pasar la orden a
     * FINALIZADA forman una unica operacion atomica (ver bloque transaccional abajo).
     */
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
            // Atomicidad del cierre (R4): insertar el cierre y marcar la orden como
            // FINALIZADA deben ser indivisibles. setAutoCommit(false) agrupa ambos en
            // una transaccion; sin esto podria persistirse el cierre pero quedar la
            // orden ASIGNADA (o al reves), un estado inconsistente. Esto justifica usar
            // motor InnoDB en MySQL: es el que soporta transacciones (MyISAM no).
            conn.setAutoCommit(false);
            cierreDAO.insertar(cierre, conn);
            ordenDAO.actualizarEstado(idOrden, EstadoOrden.FINALIZADA, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    // Si algo falla, se deshacen ambos cambios juntos.
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

    /** Lista heterogenea ordenada por descripcion (polimorfismo + I3). */
    public List<Recurso> listarRecursosOperativos() {
        List<Recurso> recursos = new ArrayList<>();
        recursos.addAll(personalDAO.listarOperativos());
        recursos.addAll(activoDAO.listarDisponibles());
        recursos.sort(Comparator.comparing(Recurso::getDescripcion, String.CASE_INSENSITIVE_ORDER));
        return recursos;
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

    /** Busqueda lineal sobre List&lt;Recurso&gt; (I3). */
    public Recurso buscarRecursoPorId(List<Recurso> recursos, int idRecurso) {
        for (Recurso recurso : recursos) {
            if (recurso.getId() == idRecurso) {
                return recurso;
            }
        }
        return null;
    }

    /** Polimorfismo: invoca estaDisponible() sin conocer el tipo concreto. */
    public boolean recursoEstaDisponibleEnLista(List<Recurso> recursos, int idRecurso) {
        Recurso recurso = buscarRecursoPorId(recursos, idRecurso);
        return recurso != null && recurso.estaDisponible();
    }

    private List<Recurso> listarRecursosPorTipo(TipoRecurso tipo) {
        List<Recurso> recursos = new ArrayList<>();
        if (tipo == TipoRecurso.PERSONAL) {
            recursos.addAll(personalDAO.listarTodos());
        } else {
            recursos.addAll(activoDAO.listarTodos());
        }
        recursos.sort(Comparator.comparing(Recurso::getDescripcion, String.CASE_INSENSITIVE_ORDER));
        return recursos;
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
