package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.modelo.Activo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a activos para validacion R2 (recurso operativo). */
public class ActivoDAO {

    private static final String SELECT_BY_ID = """
            SELECT id_activo, id_tipo, descripcion, estado
            FROM activo
            WHERE id_activo = ?
            """;

    private static final String SELECT_DISPONIBLES = """
            SELECT id_activo, id_tipo, descripcion, estado
            FROM activo
            WHERE estado = 'Disponible'
            ORDER BY id_activo
            """;

    public Activo buscarPorId(int id) {
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
            throw new DaoException("Error al buscar activo id=" + id, e);
        }
    }

    public List<Activo> listarDisponibles() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_DISPONIBLES);
             ResultSet rs = ps.executeQuery()) {
            List<Activo> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar activos disponibles", e);
        }
    }

    private Activo mapRow(ResultSet rs) throws SQLException {
        Activo activo = new Activo();
        activo.setIdActivo(rs.getInt("id_activo"));
        activo.setIdTipo(rs.getInt("id_tipo"));
        activo.setDescripcion(rs.getString("descripcion"));
        activo.setEstado(EnumMapper.fromDbEstadoActivo(rs.getString("estado")));
        return activo;
    }
}
