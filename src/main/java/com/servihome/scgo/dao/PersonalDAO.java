package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.modelo.Personal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a personal para validacion R2 (recurso operativo). */
public class PersonalDAO implements DAO<Personal> {

    private static final String INSERT = """
            INSERT INTO personal (id_especialidad, nombre, estado) VALUES (?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id_personal, id_especialidad, nombre, estado
            FROM personal
            WHERE id_personal = ?
            """;

    private static final String SELECT_TODOS = """
            SELECT id_personal, id_especialidad, nombre, estado
            FROM personal
            ORDER BY nombre
            """;

    private static final String SELECT_OPERATIVOS = """
            SELECT id_personal, id_especialidad, nombre, estado
            FROM personal
            WHERE estado = 'Activo'
            ORDER BY nombre
            """;

    private static final String UPDATE = """
            UPDATE personal SET id_especialidad = ?, nombre = ?, estado = ? WHERE id_personal = ?
            """;

    @Override
    public int insertar(Personal obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, obj.getIdEspecialidad());
            ps.setString(2, obj.getNombre());
            ps.setString(3, EnumMapper.toDb(obj.getEstado()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    obj.setId(id);
                    return id;
                }
            }
            throw new DaoException("No se obtuvo id generado para personal", null);
        } catch (SQLException e) {
            throw new DaoException("Error al insertar personal", e);
        }
    }

    @Override
    public Personal buscarPorId(int id) {
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
            throw new DaoException("Error al buscar personal id=" + id, e);
        }
    }

    @Override
    public void actualizar(Personal obj) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setInt(1, obj.getIdEspecialidad());
            ps.setString(2, obj.getNombre());
            ps.setString(3, EnumMapper.toDb(obj.getEstado()));
            ps.setInt(4, obj.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al actualizar personal id=" + obj.getId(), e);
        }
    }

    public List<Personal> listarOperativos() {
        return consultarLista(SELECT_OPERATIVOS);
    }

    public List<Personal> listarTodos() {
        return consultarLista(SELECT_TODOS);
    }

    private List<Personal> consultarLista(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Personal> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar personal", e);
        }
    }

    private Personal mapRow(ResultSet rs) throws SQLException {
        return new Personal(
                rs.getInt("id_personal"),
                rs.getString("nombre"),
                rs.getInt("id_especialidad"),
                EnumMapper.fromDbEstadoPersonal(rs.getString("estado")));
    }
}
