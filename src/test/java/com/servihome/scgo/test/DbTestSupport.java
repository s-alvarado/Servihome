package com.servihome.scgo.test;

import com.servihome.scgo.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utilidades para tests de integracion contra MySQL.
 */
public final class DbTestSupport {

    private DbTestSupport() {
    }

    public static boolean isDbAvailable() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    public static void ejecutarSql(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}
