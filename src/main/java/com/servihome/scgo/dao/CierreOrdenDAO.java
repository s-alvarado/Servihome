package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.modelo.CierreOrden;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CierreOrdenDAO implements DAO<CierreOrden> {

    private static final String INSERT = """
            INSERT INTO cierre_orden (id_orden, horas_reales, materiales, estado_devolucion, averia, detalle_averia)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_cierre, id_orden, horas_reales, materiales, estado_devolucion, averia, detalle_averia
            FROM cierre_orden
            WHERE id_cierre = ?
            """;

    private static final String UPDATE = """
            UPDATE cierre_orden
            SET id_orden = ?, horas_reales = ?, materiales = ?, estado_devolucion = ?,
                averia = ?, detalle_averia = ?
            WHERE id_cierre = ?
            """;

    @Override
    public int insertar(CierreOrden obj) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return insertar(obj, conn);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar cierre orden id=" + obj.getIdOrden(), e);
        }
    }

    /** Variante para CU008: participa en la transaccion del controlador (R4). */
    public int insertar(CierreOrden obj, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, obj.getIdOrden());
            ps.setBigDecimal(2, obj.getHorasReales());
            ps.setString(3, obj.getMateriales());
            ps.setString(4, EnumMapper.toDb(obj.getEstadoDevolucion()));
            ps.setBoolean(5, obj.isAveria());
            ps.setString(6, obj.getDetalleAveria());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    obj.setIdCierre(id);
                    return id;
                }
            }
            throw new DaoException("No se obtuvo id generado para cierre", null);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar cierre orden id=" + obj.getIdOrden(), e);
        }
    }

    @Override
    public CierreOrden buscarPorId(int id) {
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
            throw new DaoException("Error al buscar cierre id=" + id, e);
        }
    }

    @Override
    public void actualizar(CierreOrden obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setInt(1, obj.getIdOrden());
            ps.setBigDecimal(2, obj.getHorasReales());
            ps.setString(3, obj.getMateriales());
            ps.setString(4, EnumMapper.toDb(obj.getEstadoDevolucion()));
            ps.setBoolean(5, obj.isAveria());
            ps.setString(6, obj.getDetalleAveria());
            ps.setInt(7, obj.getIdCierre());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar cierre id=" + obj.getIdCierre(), e);
        }
    }

    private CierreOrden mapRow(ResultSet rs) throws SQLException {
        CierreOrden cierre = new CierreOrden();
        cierre.setIdCierre(rs.getInt("id_cierre"));
        cierre.setIdOrden(rs.getInt("id_orden"));
        cierre.setHorasReales(rs.getBigDecimal("horas_reales"));
        cierre.setMateriales(rs.getString("materiales"));
        cierre.setEstadoDevolucion(EnumMapper.fromDbEstadoDevolucion(rs.getString("estado_devolucion")));
        cierre.setAveria(rs.getBoolean("averia"));
        cierre.setDetalleAveria(rs.getString("detalle_averia"));
        return cierre;
    }
}
