package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.modelo.Personal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a personal para validacion R2 (recurso operativo). */
public class PersonalDAO {

    private static final String SELECT_BY_ID = """
            SELECT id_personal, id_especialidad, nombre, estado
            FROM personal
            WHERE id_personal = ?
            """;

    private static final String SELECT_OPERATIVOS = """
            SELECT id_personal, id_especialidad, nombre, estado
            FROM personal
            WHERE estado = 'Activo'
            ORDER BY id_personal
            """;

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

    public List<Personal> listarOperativos() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_OPERATIVOS);
             ResultSet rs = ps.executeQuery()) {
            List<Personal> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DaoException("Error al listar personal operativo", e);
        }
    }

    private Personal mapRow(ResultSet rs) throws SQLException {
        Personal personal = new Personal();
        personal.setIdPersonal(rs.getInt("id_personal"));
        personal.setIdEspecialidad(rs.getInt("id_especialidad"));
        personal.setNombre(rs.getString("nombre"));
        personal.setEstado(EnumMapper.fromDbEstadoPersonal(rs.getString("estado")));
        return personal;
    }
}
