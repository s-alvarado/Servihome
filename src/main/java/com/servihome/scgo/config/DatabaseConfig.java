package com.servihome.scgo.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuracion JDBC externa. Host, puerto, base, usuario y clave se leen de un
 * archivo de propiedades (via variable de entorno SCGO_DB_CONFIG, system property
 * o classpath), NO se hardcodean ni se asume localhost.
 *
 * Decision de infraestructura Cloud: la BD MySQL vive en un host gestionado y
 * variable segun el entorno (dev/QA/prod), por lo que la conexion debe configurarse
 * por entorno sin recompilar. Hardcodear localhost/credenciales rompe el despliegue
 * en la nube y expone secretos en el codigo fuente.
 */
public final class DatabaseConfig {

    private static final String CONFIG_ENV = "SCGO_DB_CONFIG";
    private static final String CONFIG_PROPERTY = "scgo.db.config";
    private static final String CLASSPATH_DEFAULT = "database.properties";

    private static volatile Properties cachedProperties;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        Properties props = loadProperties();
        String url = buildJdbcUrl(props);
        return DriverManager.getConnection(
                url,
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
    }

    private static Properties loadProperties() {
        if (cachedProperties != null) {
            return cachedProperties;
        }
        synchronized (DatabaseConfig.class) {
            if (cachedProperties != null) {
                return cachedProperties;
            }
            Properties props = new Properties();
            String externalPath = System.getenv(CONFIG_ENV);
            if (externalPath == null || externalPath.isBlank()) {
                externalPath = System.getProperty(CONFIG_PROPERTY);
            }
            try {
                if (externalPath != null && !externalPath.isBlank()) {
                    try (InputStream in = Files.newInputStream(Path.of(externalPath))) {
                        props.load(in);
                    }
                } else {
                    try (InputStream in = DatabaseConfig.class.getClassLoader()
                            .getResourceAsStream(CLASSPATH_DEFAULT)) {
                        if (in == null) {
                            throw new IllegalStateException(
                                    "No se encontro " + CLASSPATH_DEFAULT
                                            + ". Copie database.properties.example o defina "
                                            + CONFIG_ENV);
                        }
                        props.load(in);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error al cargar configuracion JDBC", e);
            }
            validateRequired(props);
            cachedProperties = props;
            return cachedProperties;
        }
    }

    private static void validateRequired(Properties props) {
        require(props, "jdbc.host");
        require(props, "jdbc.port");
        require(props, "jdbc.database");
        require(props, "jdbc.user");
        require(props, "jdbc.password");
    }

    private static void require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propiedad JDBC requerida ausente: " + key);
        }
    }

    private static String buildJdbcUrl(Properties props) {
        String host = props.getProperty("jdbc.host");
        String port = props.getProperty("jdbc.port");
        String database = props.getProperty("jdbc.database");
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=true&serverTimezone=UTC&characterEncoding=utf8";
    }
}
