package com.servihome.scgo;

import com.servihome.scgo.config.DatabaseConfig;
import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.dao.ActivoDAO;
import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.PersonalDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import com.servihome.scgo.enums.EstadoActivo;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.excepcion.SolapamientoException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** CPS04: cierre con averia y rechazo de un activo que no pertenece a la orden. */
@EnabledIf("com.servihome.scgo.test.DbTestSupport#isDbAvailable")
class CierreConAveriaIntegrationTest {

    private static final int ID_SOLICITUD = 9200;
    private static final int ID_ACTIVO = 9201;
    private static final int ID_ACTIVO_AJENO = 9202;

    private OrdenController controller;
    private Connection conn;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SQLException {
        assumeTrue(DbTestSupport.isDbAvailable());
        controller = new OrdenController(
                new OrdenTrabajoDAO(),
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
    void cps04_cierreConAveria_ordenFinalizadaYActivoEnReparacion() throws SolapamientoException {
        OrdenTrabajo orden = generarYAsignarActivo();

        CierreOrden cierre = cierreConAveria("Perdida de aceite");
        controller.cerrarOrden(orden.getIdOrden(), cierre, new int[] {ID_ACTIVO});

        assertEquals(EstadoOrden.FINALIZADA, controller.buscarOrden(orden.getIdOrden()).getEstado());
        assertEquals(EstadoActivo.EN_REPARACION, new ActivoDAO().buscarPorId(ID_ACTIVO).getEstado());
        assertEquals(orden.getIdOrden(), new CierreOrdenDAO().buscarPorOrden(orden.getIdOrden()).getIdOrden());
    }

    @Test
    void r6_activoNoAsignado_noPersisteElCierre() throws SolapamientoException {
        OrdenTrabajo orden = generarYAsignarActivo();

        CierreOrden cierre = cierreConAveria("Rotura ajena");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> controller.cerrarOrden(orden.getIdOrden(), cierre, new int[] {ID_ACTIVO_AJENO}));

        assertTrue(error.getMessage().contains(
                "R6: el activo id=" + ID_ACTIVO_AJENO + " no esta asignado a la orden " + orden.getIdOrden()));
        assertEquals(EstadoOrden.ASIGNADA, controller.buscarOrden(orden.getIdOrden()).getEstado());
        assertNull(new CierreOrdenDAO().buscarPorOrden(orden.getIdOrden()));
        assertEquals(EstadoActivo.DISPONIBLE, new ActivoDAO().buscarPorId(ID_ACTIVO).getEstado());
    }

    private OrdenTrabajo generarYAsignarActivo() throws SolapamientoException {
        OrdenTrabajo orden = controller.generarOrden(
                ID_SOLICITUD,
                LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        controller.validarYAsignar(orden.getIdOrden(), ID_ACTIVO, TipoRecurso.ACTIVO);
        return orden;
    }

    private static CierreOrden cierreConAveria(String detalle) {
        CierreOrden cierre = new CierreOrden();
        cierre.setHorasReales(new BigDecimal("4.00"));
        cierre.setMateriales("Repuestos");
        cierre.setEstadoDevolucion(EstadoDevolucion.AVERIADO);
        cierre.setAveria(true);
        cierre.setDetalleAveria(detalle);
        return cierre;
    }

    private void insertarDatos() throws SQLException {
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO tipo_activo (id_tipo, nombre) VALUES (9200, 'Test Averia')
                ON DUPLICATE KEY UPDATE nombre = nombre
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO cliente (id_cliente, razon_social, tipo, activo)
                VALUES (9200, 'Cliente Averia', 'B2C', TRUE)
                ON DUPLICATE KEY UPDATE razon_social = razon_social
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO propiedad (id_propiedad, id_cliente, direccion)
                VALUES (9200, 9200, 'Dir Averia')
                ON DUPLICATE KEY UPDATE direccion = direccion
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO activo (id_activo, id_tipo, descripcion, estado)
                VALUES (9201, 9200, 'Activo asignado', 'Disponible')
                ON DUPLICATE KEY UPDATE estado = 'Disponible', descripcion = 'Activo asignado'
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO activo (id_activo, id_tipo, descripcion, estado)
                VALUES (9202, 9200, 'Activo ajeno', 'Disponible')
                ON DUPLICATE KEY UPDATE estado = 'Disponible', descripcion = 'Activo ajeno'
                """);
        DbTestSupport.ejecutarSql(conn, """
                INSERT INTO solicitud (id_solicitud, id_cliente, id_propiedad, fecha_ingreso, descripcion, estado)
                VALUES (9200, 9200, 9200, '2026-07-01', 'Solicitud averia CPS04', 'Pendiente')
                ON DUPLICATE KEY UPDATE estado = 'Pendiente', descripcion = 'Solicitud averia CPS04'
                """);
    }

    private void limpiarDatos() throws SQLException {
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cierre_orden WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9200)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM asignacion_activo WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9200)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM asignacion_personal WHERE id_orden IN (SELECT id_orden FROM orden_trabajo WHERE id_solicitud = 9200)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM orden_trabajo WHERE id_solicitud = 9200");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM solicitud WHERE id_solicitud = 9200");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM activo WHERE id_activo IN (9201, 9202)");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM propiedad WHERE id_propiedad = 9200");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM cliente WHERE id_cliente = 9200");
        DbTestSupport.ejecutarSql(conn, "DELETE FROM tipo_activo WHERE id_tipo = 9200");
    }
}
