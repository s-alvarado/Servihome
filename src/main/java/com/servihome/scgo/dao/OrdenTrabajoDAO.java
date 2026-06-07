package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrdenTrabajoDAO implements DAO<OrdenTrabajo> {

    private static final String INSERT = """
            INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_orden, id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado
            FROM orden_trabajo
            WHERE id_orden = ?
            """;

    private static final String UPDATE = """
            UPDATE orden_trabajo
            SET id_solicitud = ?, fecha_servicio = ?, hora_inicio = ?, hora_fin = ?, estado = ?
            WHERE id_orden = ?
            """;

    private static final String UPDATE_ESTADO = """
            UPDATE orden_trabajo SET estado = ? WHERE id_orden = ?
            """;

    private static final String SELECT_POR_ESTADO = """
            SELECT id_orden, id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado
            FROM orden_trabajo
            WHERE estado = ?
            ORDER BY id_orden
            """;

    @Override
    public int insertar(OrdenTrabajo obj) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return insertar(obj, conn);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar orden de trabajo", e);
        }
    }

    /** Variante para CU006: participa en la transaccion del controlador. */
    public int insertar(OrdenTrabajo obj, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, obj.getIdSolicitud());
            ps.setObject(2, obj.getFechaServicio());
            ps.setObject(3, obj.getHoraInicio());
            ps.setObject(4, obj.getHoraFin());
            ps.setString(5, EnumMapper.toDb(obj.getEstado()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    obj.setIdOrden(id);
                    return id;
                }
            }
            throw new DaoException("No se obtuvo id generado para orden", null);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar orden de trabajo", e);
        }
    }

    @Override
    public OrdenTrabajo buscarPorId(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al buscar orden id=" + id, e);
        }
    }

    @Override
    public void actualizar(OrdenTrabajo obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setInt(1, obj.getIdSolicitud());
            ps.setObject(2, obj.getFechaServicio());
            ps.setObject(3, obj.getHoraInicio());
            ps.setObject(4, obj.getHoraFin());
            ps.setString(5, EnumMapper.toDb(obj.getEstado()));
            ps.setInt(6, obj.getIdOrden());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar orden id=" + obj.getIdOrden(), e);
        }
    }

    public void actualizarEstado(int idOrden, EstadoOrden estado) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            actualizarEstado(idOrden, estado, conn);
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar estado orden id=" + idOrden, e);
        }
    }

    /** Variante para CU008: participa en la transaccion del controlador (R4). */
    public void actualizarEstado(int idOrden, EstadoOrden estado, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_ESTADO)) {
            ps.setString(1, EnumMapper.toDb(estado));
            ps.setInt(2, idOrden);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar estado orden id=" + idOrden, e);
        }
    }

    public List<OrdenTrabajo> listarPorEstado(EstadoOrden estado) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_POR_ESTADO)) {
            ps.setString(1, EnumMapper.toDb(estado));
            try (ResultSet rs = ps.executeQuery()) {
                List<OrdenTrabajo> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al listar ordenes estado=" + estado, e);
        }
    }

    private OrdenTrabajo mapRow(ResultSet rs) throws SQLException {
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setIdOrden(rs.getInt("id_orden"));
        orden.setIdSolicitud(rs.getInt("id_solicitud"));
        orden.setFechaServicio(rs.getObject("fecha_servicio", java.time.LocalDate.class));
        orden.setHoraInicio(rs.getObject("hora_inicio", java.time.LocalTime.class));
        orden.setHoraFin(rs.getObject("hora_fin", java.time.LocalTime.class));
        orden.setEstado(EnumMapper.fromDbEstadoOrden(rs.getString("estado")));
        return orden;
    }
}
