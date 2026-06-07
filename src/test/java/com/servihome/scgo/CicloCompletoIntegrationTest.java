package com.servihome.scgo;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.dao.ActivoDAO;
import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.PersonalDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.enums.EstadoSolicitud;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.test.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** CPS03: ciclo funcional CU006 -> CU007 -> CU008 contra MySQL. */
@EnabledIf("com.servihome.scgo.test.DbTestSupport#isDbAvailable")
class CicloCompletoIntegrationTest {

    private static final int ID_SOLICITUD = 9100;
    private static final int ID_PERSONAL = 9101;
    private static final int ID_ACTIVO = 9101;

    private OrdenController controller;
    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        assumeTrue(DbTestSupport.isDbAvailable());
        controller = new OrdenController(
                new OrdenTrabajoDAO(),
                new SolicitudDAO(),
                new AsignacionDAO(),
                new CierreOrdenDAO(),
                new PersonalDAO(),
                new ActivoDAO());
        conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
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
    void cps03_cicloCompleto_generarAsignarCerrar() {
        OrdenTrabajo orden = controller.generarOrden(
                ID_SOLICITUD,
                LocalDate.of(2026, 7, 1),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        assertTrue(orden.getIdOrden() > 0);
        assertEquals(EstadoOrden.ASIGNADA, orden.getEstado());

        assertTrue(controller.validarYAsignar(orden.getIdOrden(), ID_PERSONAL, TipoRecurso.PERSONAL));
        assertTrue(controller.validarYAsignar(orden.getIdOrden(), ID_ACTIVO, TipoRecurso.ACTIVO));

        CierreOrden cierre = new CierreOrden();
        cierre.setHorasReales(new BigDecimal("4.00"));
        cierre.setMateriales("Cloro, repuestos");
        cierre.setEstadoDevolucion(EstadoDevolucion.BUEN_ESTADO);
        cierre.setAveria(false);
        controller.cerrarOrden(orden.getIdOrden(), cierre);

        OrdenTrabajo finalizada = controller.buscarOrden(orden.getIdOrden());
        assertEquals(EstadoOrden.FINALIZADA, finalizada.getEstado());
    }

    private void insertarDatos() throws SQLException {
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO especialidad (id_especialidad, nombre) VALUES (9100, 'Test Ciclo')
                ON DUPLICATE KEY UPDATE nombre = nombre
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO tipo_activo (id_tipo, nombre) VALUES (9100, 'TestTipo')
                ON DUPLICATE KEY UPDATE nombre = nombre
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO cliente (id_cliente, razon_social, tipo, activo)
                VALUES (9100, 'Cliente Ciclo', 'B2C', TRUE)
                ON DUPLICATE KEY UPDATE razon_social = razon_social
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO propiedad (id_propiedad, id_cliente, direccion)
                VALUES (9100, 9100, 'Dir Ciclo')
                ON DUPLICATE KEY UPDATE direccion = direccion
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO personal (id_personal, id_especialidad, nombre, estado)
                VALUES (9101, 9100, 'Operario Ciclo', 'Activo')
                ON DUPLICATE KEY UPDATE nombre = nombre
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO activo (id_activo, id_tipo, descripcion, estado)
                VALUES (9101, 9100, 'Herramienta Ciclo', 'Disponible')
                ON DUPLICATE KEY UPDATE descripcion = descripcion
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO solicitud (id_solicitud, id_cliente, id_propiedad, fecha_ingreso, descripcion, estado)
                VALUES (9100, 9100, 9100, '2026-06-01', 'Solicitud ciclo CPS03', 'Pendiente')
                ON DUPLICATE KEY UPDATE estado = 'Pendiente', descripcion = 'Solicitud ciclo CPS03'
                """);
    }

    private void limpiarDatos() throws SQLException {
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cierre_orden WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9100)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM asignacion_personal WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9100)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM asignacion_activo WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9100)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM orden_trabajo WHERE id_solicitud = 9100");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM solicitud WHERE id_solicitud = 9100");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM activo WHERE id_activo = 9101");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM personal WHERE id_personal = 9101");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM propiedad WHERE id_propiedad = 9100");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cliente WHERE id_cliente = 9100");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM tipo_activo WHERE id_tipo = 9100");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM especialidad WHERE id_especialidad = 9100");
    }
}
