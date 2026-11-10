package com.servihome.scgo;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.dao.ActivoDAO;
import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.DaoException;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.PersonalDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.registro.RegistroArchivo;
import com.servihome.scgo.test.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CPI05 / DEF-01: si el segundo paso del cierre falla, no queda el cierre
 * persistido y la orden sigue ASIGNADA.
 */
@EnabledIf("com.servihome.scgo.test.DbTestSupport#isDbAvailable")
class TransaccionRollbackIntegrationTest {

    private static final int ID_SOLICITUD = 9300;

    private OrdenController controller;
    private Connection conn;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SQLException {
        assumeTrue(DbTestSupport.isDbAvailable());
        OrdenTrabajoDAO ordenQueFalla = new OrdenTrabajoDAO() {
            @Override
            public void actualizarEstado(int idOrden, EstadoOrden estado, Connection conn) {
                throw new DaoException("Fallo forzado en el segundo paso");
            }
        };
        controller = new OrdenController(
                ordenQueFalla,
                new SolicitudDAO(),
                new AsignacionDAO(),
                new CierreOrdenDAO(),
                new PersonalDAO(),
                new ActivoDAO(),
                new RegistroArchivo(tempDir.resolve("scgo.log")));
        conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        limpiarDatos();
        insertarDatos();
        conn.commit();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.setAutoCommit(false);
            limpiarDatos();
            conn.commit();
            conn.close();
        }
    }

    @Test
    void cpi05_falloEnElSegundoPaso_rollbackCompleto() {
        OrdenTrabajo orden = controller.generarOrden(
                ID_SOLICITUD,
                LocalDate.of(2026, 9, 1),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0));

        CierreOrden cierre = new CierreOrden();
        cierre.setHorasReales(new BigDecimal("3.50"));
        cierre.setEstadoDevolucion(EstadoDevolucion.BUEN_ESTADO);
        cierre.setAveria(false);

        assertThrows(DaoException.class, () -> controller.cerrarOrden(orden.getIdOrden(), cierre));

        assertEquals(EstadoOrden.ASIGNADA, new OrdenTrabajoDAO().buscarPorId(orden.getIdOrden()).getEstado());
        assertNull(new CierreOrdenDAO().buscarPorOrden(orden.getIdOrden()));
    }

    private void insertarDatos() throws SQLException {
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO cliente (id_cliente, razon_social, tipo, activo)
                VALUES (9300, 'Cliente Rollback', 'B2C', TRUE)
                ON DUPLICATE KEY UPDATE razon_social = razon_social
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO propiedad (id_propiedad, id_cliente, direccion)
                VALUES (9300, 9300, 'Dir Rollback')
                ON DUPLICATE KEY UPDATE direccion = direccion
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO solicitud (id_solicitud, id_cliente, id_propiedad, fecha_ingreso, descripcion, estado)
                VALUES (9300, 9300, 9300, '2026-08-01', 'Solicitud rollback CPI05', 'Pendiente')
                ON DUPLICATE KEY UPDATE estado = 'Pendiente', descripcion = 'Solicitud rollback CPI05'
                """);
    }

    private void limpiarDatos() throws SQLException {
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cierre_orden WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9300)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM orden_trabajo WHERE id_solicitud = 9300");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM solicitud WHERE id_solicitud = 9300");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM propiedad WHERE id_propiedad = 9300");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cliente WHERE id_cliente = 9300");
    }
}
