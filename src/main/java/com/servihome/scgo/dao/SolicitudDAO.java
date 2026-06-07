package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.enums.EstadoSolicitud;
import com.servihome.scgo.modelo.Solicitud;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SolicitudDAO implements DAO<Solicitud> {

    private static final String INSERT = """
            INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_solicitud, id_cliente, id_propiedad, fecha_ingreso, descripcion, estado
            FROM solicitud
            WHERE id_solicitud = ?
            """;

    private static final String UPDATE = """
            UPDATE solicitud
            SET id_cliente = ?, id_propiedad = ?, fecha_ingreso = ?, descripcion = ?, estado = ?
            WHERE id_solicitud = ?
            """;

    private static final String UPDATE_ESTADO = """
            UPDATE solicitud SET estado = ? WHERE id_solicitud = ?
            """;

    private static final String SELECT_POR_ESTADO = """
            SELECT id_solicitud, id_cliente, id_propiedad, fecha_ingreso, descripcion, estado
            FROM solicitud
            WHERE estado = ?
            ORDER BY id_solicitud
            """;

    @Override
    public int insertar(Solicitud obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, obj.getIdCliente());
            ps.setInt(2, obj.getIdPropiedad());
            ps.setObject(3, obj.getFechaIngreso());
            ps.setString(4, obj.getDescripcion());
            ps.setString(5, EnumMapper.toDb(obj.getEstado()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    obj.setIdSolicitud(id);
                    return id;
                }
            }
            throw new DaoException("No se obtuvo id generado para solicitud", null);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar solicitud", e);
        }
    }

    @Override
    public Solicitud buscarPorId(int id) {
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
            throw new DaoException("Error al buscar solicitud id=" + id, e);
        }
    }

    @Override
    public void actualizar(Solicitud obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setInt(1, obj.getIdCliente());
            ps.setInt(2, obj.getIdPropiedad());
            ps.setObject(3, obj.getFechaIngreso());
            ps.setString(4, obj.getDescripcion());
            ps.setString(5, EnumMapper.toDb(obj.getEstado()));
            ps.setInt(6, obj.getIdSolicitud());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar solicitud id=" + obj.getIdSolicitud(), e);
        }
    }

    public void actualizarEstado(int idSolicitud, EstadoSolicitud estado) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            actualizarEstado(idSolicitud, estado, conn);
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar estado solicitud id=" + idSolicitud, e);
        }
    }

    /** Variante para CU006: participa en la transaccion del controlador. */
    public void actualizarEstado(int idSolicitud, EstadoSolicitud estado, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_ESTADO)) {
            ps.setString(1, EnumMapper.toDb(estado));
            ps.setInt(2, idSolicitud);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar estado solicitud id=" + idSolicitud, e);
        }
    }

    public List<Solicitud> listarPorEstado(EstadoSolicitud estado) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_POR_ESTADO)) {
            ps.setString(1, EnumMapper.toDb(estado));
            try (ResultSet rs = ps.executeQuery()) {
                List<Solicitud> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al listar solicitudes estado=" + estado, e);
        }
    }

    private Solicitud mapRow(ResultSet rs) throws SQLException {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(rs.getInt("id_solicitud"));
        solicitud.setIdCliente(rs.getInt("id_cliente"));
        solicitud.setIdPropiedad(rs.getInt("id_propiedad"));
        solicitud.setFechaIngreso(rs.getObject("fecha_ingreso", java.time.LocalDate.class));
        solicitud.setDescripcion(rs.getString("descripcion"));
        solicitud.setEstado(EnumMapper.fromDbEstadoSolicitud(rs.getString("estado")));
        return solicitud;
    }
}
