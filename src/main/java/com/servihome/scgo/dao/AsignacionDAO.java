package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.enums.TipoRecurso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class AsignacionDAO {

    /**
     * R1: solapamiento si ini_existente &lt; fin_nuevo AND fin_existente &gt; ini_nuevo
     * (desigualdad estricta; turnos contiguos no solapan).
     * Solo ordenes en estado ASIGNADA.
     */
    private static final String SOLAPAMIENTO_PERSONAL = """
            SELECT ot.id_orden
            FROM asignacion_personal ap
            JOIN orden_trabajo ot ON ap.id_orden = ot.id_orden
            WHERE ap.id_personal = ?
              AND ot.fecha_servicio = ?
              AND ot.estado = 'Asignada'
              AND ot.hora_inicio < ?
              AND ot.hora_fin > ?
            """;

    private static final String SOLAPAMIENTO_ACTIVO = """
            SELECT ot.id_orden
            FROM asignacion_activo aa
            JOIN orden_trabajo ot ON aa.id_orden = ot.id_orden
            WHERE aa.id_activo = ?
              AND ot.fecha_servicio = ?
              AND ot.estado = 'Asignada'
              AND ot.hora_inicio < ?
              AND ot.hora_fin > ?
            """;

    private static final String EXCLUIR_ORDEN = " AND ot.id_orden <> ?";

    private static final String INSERT_PERSONAL = """
            INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (?, ?)
            """;

    private static final String INSERT_ACTIVO = """
            INSERT INTO asignacion_activo (id_orden, id_activo) VALUES (?, ?)
            """;

    public boolean existeSolapamiento(int idRecurso, TipoRecurso tipo,
                                      LocalDate fecha, LocalTime ini, LocalTime fin) {
        return existeSolapamientoEnOtraOrden(-1, idRecurso, tipo, fecha, ini, fin);
    }

    /**
     * Igual que {@link #existeSolapamiento} pero excluye una orden (R1: conflicto con otra orden).
     * Si idOrdenExcluir &lt;= 0 no aplica exclusion.
     */
    public boolean existeSolapamientoEnOtraOrden(int idOrdenExcluir, int idRecurso, TipoRecurso tipo,
                                                 LocalDate fecha, LocalTime ini, LocalTime fin) {
        String sql = (tipo == TipoRecurso.PERSONAL ? SOLAPAMIENTO_PERSONAL : SOLAPAMIENTO_ACTIVO)
                + (idOrdenExcluir > 0 ? EXCLUIR_ORDEN : "");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRecurso);
            ps.setObject(2, fecha);
            ps.setObject(3, fin);
            ps.setObject(4, ini);
            if (idOrdenExcluir > 0) {
                ps.setInt(5, idOrdenExcluir);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("Error al verificar solapamiento recurso id=" + idRecurso, e);
        }
    }

    public void registrarPersonal(int idOrden, int idPersonal) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_PERSONAL)) {
            ps.setInt(1, idOrden);
            ps.setInt(2, idPersonal);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al registrar personal en orden id=" + idOrden, e);
        }
    }

    public void registrarActivo(int idOrden, int idActivo) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_ACTIVO)) {
            ps.setInt(1, idOrden);
            ps.setInt(2, idActivo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error al registrar activo en orden id=" + idOrden, e);
        }
    }
}
