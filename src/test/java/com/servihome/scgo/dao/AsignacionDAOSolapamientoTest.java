package com.servihome.scgo.dao;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.test.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CPF02: particion de equivalencia y fronteras de la validacion de solapamiento (R1).
 * Orden existente fija: 2026-06-10 de 08:00 a 12:00, personal asignado.
 */
@EnabledIf("com.servihome.scgo.test.DbTestSupport#isDbAvailable")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsignacionDAOSolapamientoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 6, 10);
    private static final int ID_PERSONAL = 9001;

    private AsignacionDAO asignacionDAO;
    private OrdenController controller;
    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        assumeTrue(DbTestSupport.isDbAvailable());
        asignacionDAO = new AsignacionDAO();
        controller = new OrdenController(
                new OrdenTrabajoDAO(),
                new SolicitudDAO(),
                asignacionDAO,
                new CierreOrdenDAO(),
                new PersonalDAO(),
                new ActivoDAO());

        conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        insertarDatosPrueba();
        conn.commit();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.setAutoCommit(false);
            limpiarDatosPrueba();
            conn.commit();
            conn.close();
        }
    }

    @ParameterizedTest(name = "franja {0}-{1} bloquea={2}")
    @CsvSource({
            "06:00, 07:59, false",
            "06:00, 08:00, false",
            "06:00, 08:01, true",
            "09:00, 11:00, true",
            "11:59, 14:00, true",
            "12:00, 14:00, false",
            "12:01, 14:00, false",
            "07:00, 13:00, true"
    })
    void cpf02_existeSolapamiento_tablaVerdad(String horaInicio, String horaFin, boolean bloquea) {
        LocalTime ini = LocalTime.parse(horaInicio);
        LocalTime fin = LocalTime.parse(horaFin);

        boolean haySolapamiento = asignacionDAO.existeSolapamiento(
                ID_PERSONAL, TipoRecurso.PERSONAL, FECHA, ini, fin);

        assertEquals(bloquea, haySolapamiento,
                "Franja " + horaInicio + "-" + horaFin + " vs existente 08:00-12:00");
    }

    @ParameterizedTest(name = "controller franja {0}-{1} bloquea={2}")
    @CsvSource({
            "06:00, 08:00, false",
            "06:00, 08:01, true",
            "12:00, 14:00, false"
    })
    void cpf02_verificarSolapamiento_delegaEnDao(String horaInicio, String horaFin, boolean bloquea) {
        LocalTime ini = LocalTime.parse(horaInicio);
        LocalTime fin = LocalTime.parse(horaFin);

        boolean haySolapamiento = controller.verificarSolapamiento(
                ID_PERSONAL, TipoRecurso.PERSONAL, FECHA, ini, fin);

        assertEquals(bloquea, haySolapamiento);
    }

    private void insertarDatosPrueba() throws SQLException {
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO especialidad (id_especialidad, nombre) VALUES (9001, 'TestEsp')
                ON DUPLICATE KEY UPDATE nombre = nombre
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO personal (id_personal, id_especialidad, nombre, estado)
                VALUES (9001, 9001, 'Test Solapamiento', 'Activo')
                ON DUPLICATE KEY UPDATE nombre = nombre
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO cliente (id_cliente, razon_social, tipo, activo)
                VALUES (9001, 'Cliente Test', 'B2C', TRUE)
                ON DUPLICATE KEY UPDATE razon_social = razon_social
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO propiedad (id_propiedad, id_cliente, direccion)
                VALUES (9001, 9001, 'Dir Test')
                ON DUPLICATE KEY UPDATE direccion = direccion
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO solicitud (id_solicitud, id_cliente, id_propiedad, fecha_ingreso, descripcion, estado)
                VALUES (9001, 9001, 9001, '2026-06-01', 'Solicitud test solapamiento', 'Programada')
                ON DUPLICATE KEY UPDATE descripcion = descripcion
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO orden_trabajo (id_orden, id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado)
                VALUES (9001, 9001, '2026-06-10', '08:00:00', '12:00:00', 'Asignada')
                ON DUPLICATE KEY UPDATE estado = 'Asignada'
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO asignacion_personal (id_orden, id_personal)
                VALUES (9001, 9001)
                ON DUPLICATE KEY UPDATE id_personal = id_personal
                """);
    }

    private void limpiarDatosPrueba() throws SQLException {
        DbTestSupport.ejecutarSql(conn, "DELETE FROM asignacion_personal WHERE id_orden = 9001");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM orden_trabajo WHERE id_orden = 9001");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM solicitud WHERE id_solicitud = 9001");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM propiedad WHERE id_propiedad = 9001");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cliente WHERE id_cliente = 9001");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM personal WHERE id_personal = 9001");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM especialidad WHERE id_especialidad = 9001");
    }
}
