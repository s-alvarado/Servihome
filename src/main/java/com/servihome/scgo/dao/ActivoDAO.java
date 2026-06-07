package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.modelo.Activo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a activos para validacion R2 (recurso operativo). */
public class ActivoDAO implements DAO<Activo> {

    private static final String INSERT = """
            INSERT INTO activo (id_tipo, descripcion, estado) VALUES (?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_activo, id_tipo, descripcion, estado
            FROM activo
            WHERE id_activo = ?
            """;

    private static final String SELECT_TODOS = """
            SELECT id_activo, id_tipo, descripcion, estado
            FROM activo
            ORDER BY descripcion
            """;

    private static final String SELECT_DISPONIBLES = """
            SELECT id_activo, id_tipo, descripcion, estado
            FROM activo
            WHERE estado = 'Disponible'
            ORDER BY descripcion
            """;

    private static final String UPDATE = """
            UPDATE activo SET id_tipo = ?, descripcion = ?, estado = ? WHERE id_activo = ?
            """;

    @Override
    public int insertar(Activo obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, obj.getIdTipo());
            ps.setString(2, obj.getDescripcion());
            ps.setString(3, EnumMapper.toDb(obj.getEstado()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    obj.setId(id);
                    return id;
                }
            }
            throw new DaoException("No se obtuvo id generado para activo", null);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar activo", e);
        }
    }

    @Override
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

    @Override
    public void actualizar(Activo obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setInt(1, obj.getIdTipo());
            ps.setString(2, obj.getDescripcion());
            ps.setString(3, EnumMapper.toDb(obj.getEstado()));
            ps.setInt(4, obj.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar activo id=" + obj.getId(), e);
        }
    }

    public List<Activo> listarDisponibles() {
        return consultarLista(SELECT_DISPONIBLES);
    }

    public List<Activo> listarTodos() {
        return consultarLista(SELECT_TODOS);
    }

    private List<Activo> consultarLista(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Activo> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar activos", e);
        }
    }

    private Activo mapRow(ResultSet rs) throws SQLException {
        return new Activo(
                rs.getInt("id_activo"),
                rs.getString("descripcion"),
                rs.getInt("id_tipo"),
                EnumMapper.fromDbEstadoActivo(rs.getString("estado")));
    }
}
