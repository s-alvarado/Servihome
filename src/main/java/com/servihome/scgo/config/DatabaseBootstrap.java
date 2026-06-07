package com.servihome.scgo.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Inicializa esquema y datos de demo si la base esta vacia.
 */
public final class DatabaseBootstrap {

    private DatabaseBootstrap() {
    }

    public static void inicializarSiEsNecesario() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!existeTablaSolicitud(conn)) {
                ejecutarScript(conn, "/db/schema.sql");
                System.out.println("[SCGO] Esquema creado.");
            }
            if (contarRegistros(conn, "cliente") == 0) {
                ejecutarScript(conn, "/db/seed.sql");
                System.out.println("[SCGO] Datos iniciales cargados (solicitud PENDIENTE id=1).");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar la base de datos", e);
        }
    }

    public static void verificarConexion() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!conn.isValid(3)) {
                throw new IllegalStateException("Conexion JDBC invalida");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo conectar a MySQL. Configure database.properties o SCGO_DB_CONFIG.", e);
        }
    }

    private static boolean existeTablaSolicitud(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "solicitud", new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static int contarRegistros(Connection conn, String tabla) throws SQLException {
        if (!existeTabla(conn, tabla)) {
            return 0;
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean existeTabla(Connection conn, String tabla) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tabla, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void ejecutarScript(Connection conn, String resourcePath) throws SQLException {
        String contenido = leerRecurso(resourcePath);
        for (String sentencia : contenido.split(";")) {
            String sql = limpiarSentencia(sentencia);
            if (sql.isEmpty()) {
                continue;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    private static String limpiarSentencia(String sentencia) {
        StringBuilder limpia = new StringBuilder();
        for (String linea : sentencia.split("\n")) {
            String trim = linea.trim();
            if (trim.isEmpty() || trim.startsWith("--")) {
                continue;
            }
            limpia.append(linea).append('\n');
        }
        return limpia.toString().trim();
    }

    private static String leerRecurso(String path) {
        InputStream in = DatabaseBootstrap.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Recurso SQL no encontrado: " + path);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer " + path, e);
        }
    }
}
