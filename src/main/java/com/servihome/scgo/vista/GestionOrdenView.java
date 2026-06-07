package com.servihome.scgo.vista;

import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.modelo.Activo;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.modelo.Personal;
import com.servihome.scgo.modelo.Solicitud;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class GestionOrdenView {

    private final OrdenController controller;
    private final Scanner scanner;

    public GestionOrdenView(OrdenController controller) {
        this(controller, new Scanner(System.in));
    }

    public GestionOrdenView(OrdenController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
    }

    /** CU006: dispara generarOrden. */
    public void seleccionarSolicitud(int idSolicitud) {
        try {
            mostrarResultado("Programacion de la orden de servicio:");
            LocalDate fecha = leerFecha("Fecha de servicio (AAAA-MM-DD): ");
            LocalTime horaInicio = leerHora("Hora inicio (HH:MM): ");
            LocalTime horaFin = leerHora("Hora fin (HH:MM): ");

            OrdenTrabajo orden = controller.generarOrden(idSolicitud, fecha, horaInicio, horaFin);
            mostrarResultado("Orden generada: id=" + orden.getIdOrden()
                    + ", estado=" + orden.getEstado()
                    + ", franja=" + orden.getFechaServicio() + " "
                    + orden.getHoraInicio() + "-" + orden.getHoraFin());
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarResultado("Error al generar orden: " + e.getMessage());
        } catch (RuntimeException e) {
            mostrarResultado("Error inesperado al generar orden: " + e.getMessage());
        }
    }

    /** CU007: dispara validarYAsignar; feedback de exito o solapamiento. */
    public void asignarRecurso(int idOrden, int idRecurso, TipoRecurso tipo) {
        try {
            boolean asignado = controller.validarYAsignar(idOrden, idRecurso, tipo);
            if (asignado) {
                mostrarResultado("Recurso asignado correctamente a la orden " + idOrden);
            } else {
                mostrarResultado("Asignacion rechazada: recurso no operativo o solapamiento detectado");
            }
        } catch (RuntimeException e) {
            mostrarResultado("Error al asignar recurso: " + e.getMessage());
        }
    }

    /** CU008: dispara cerrarOrden. */
    public void registrarCierre(int idOrden, CierreOrden cierre) {
        try {
            controller.cerrarOrden(idOrden, cierre);
            mostrarResultado("Orden " + idOrden + " cerrada correctamente (FINALIZADA)");
        } catch (IllegalArgumentException e) {
            mostrarResultado("Error de validacion al cerrar: " + e.getMessage());
        } catch (RuntimeException e) {
            mostrarResultado("Error al cerrar orden: " + e.getMessage());
        }
    }

    /** CPS03: ciclo completo CU006 -> CU007 -> CU008 en consola. */
    public void ejecutarCicloCompleto() {
        mostrarResultado("=== Ciclo completo CU006 -> CU007 -> CU008 ===");

        List<Solicitud> pendientes = controller.listarSolicitudesPendientes();
        if (pendientes.isEmpty()) {
            mostrarResultado("No hay solicitudes PENDIENTES. Cree una solicitud en la base o reinicie datos.");
            return;
        }
        mostrarSolicitudes(pendientes);
        int idSolicitud = leerEntero("Seleccione ID de solicitud: ");

        mostrarResultado("--- Paso 1/3: CU006 Generar orden ---");
        seleccionarSolicitud(idSolicitud);

        List<OrdenTrabajo> asignadas = controller.listarOrdenesAsignadas();
        OrdenTrabajo orden = asignadas.stream()
                .filter(o -> o.getIdSolicitud() == idSolicitud)
                .reduce((first, second) -> second)
                .orElse(null);
        if (orden == null) {
            mostrarResultado("No se pudo continuar: la orden no fue creada.");
            return;
        }
        int idOrden = orden.getIdOrden();
        mostrarResultado("Orden activa para el ciclo: id=" + idOrden);

        mostrarResultado("--- Paso 2/3: CU007 Asignar recursos ---");
        mostrarPersonal(controller.listarPersonalOperativo());
        int idPersonal = leerEntero("ID de personal a asignar: ");
        asignarRecurso(idOrden, idPersonal, TipoRecurso.PERSONAL);

        mostrarActivos(controller.listarActivosDisponibles());
        int idActivo = leerEntero("ID de activo a asignar: ");
        asignarRecurso(idOrden, idActivo, TipoRecurso.ACTIVO);

        mostrarResultado("--- Paso 3/3: CU008 Cerrar orden ---");
        CierreOrden cierre = leerCierreBasico();
        registrarCierre(idOrden, cierre);

        OrdenTrabajo finalizada = controller.buscarOrden(idOrden);
        if (finalizada != null && finalizada.getEstado() == EstadoOrden.FINALIZADA) {
            mostrarResultado("Ciclo completo exitoso. Orden " + idOrden + " FINALIZADA.");
        }
    }

    public void mostrarEstadoSistema() {
        mostrarResultado("--- Solicitudes PENDIENTES ---");
        List<Solicitud> pendientes = controller.listarSolicitudesPendientes();
        if (pendientes.isEmpty()) {
            mostrarResultado("(ninguna)");
        } else {
            mostrarSolicitudes(pendientes);
        }

        mostrarResultado("--- Ordenes ASIGNADAS ---");
        List<OrdenTrabajo> asignadas = controller.listarOrdenesAsignadas();
        if (asignadas.isEmpty()) {
            mostrarResultado("(ninguna)");
        } else {
            mostrarOrdenes(asignadas);
        }

        mostrarResultado("--- Personal operativo ---");
        mostrarPersonal(controller.listarPersonalOperativo());

        mostrarResultado("--- Activos disponibles ---");
        mostrarActivos(controller.listarActivosDisponibles());
    }

    public void mostrarResultado(String mensaje) {
        System.out.println("[SCGO] " + mensaje);
    }

    private void mostrarSolicitudes(List<Solicitud> solicitudes) {
        for (Solicitud s : solicitudes) {
            mostrarResultado("  id=" + s.getIdSolicitud()
                    + " | " + s.getDescripcion()
                    + " | ingreso=" + s.getFechaIngreso()
                    + " | estado=" + s.getEstado());
        }
    }

    private void mostrarOrdenes(List<OrdenTrabajo> ordenes) {
        for (OrdenTrabajo o : ordenes) {
            mostrarResultado("  id=" + o.getIdOrden()
                    + " | solicitud=" + o.getIdSolicitud()
                    + " | " + o.getFechaServicio() + " " + o.getHoraInicio() + "-" + o.getHoraFin()
                    + " | estado=" + o.getEstado());
        }
    }

    private void mostrarPersonal(List<Personal> personal) {
        if (personal.isEmpty()) {
            mostrarResultado("(ninguno)");
            return;
        }
        for (Personal p : personal) {
            mostrarResultado("  id=" + p.getIdPersonal() + " | " + p.getNombre() + " | " + p.getEstado());
        }
    }

    private void mostrarActivos(List<Activo> activos) {
        if (activos.isEmpty()) {
            mostrarResultado("(ninguno)");
            return;
        }
        for (Activo a : activos) {
            mostrarResultado("  id=" + a.getIdActivo() + " | " + a.getDescripcion() + " | " + a.getEstado());
        }
    }

    private CierreOrden leerCierreBasico() {
        CierreOrden cierre = new CierreOrden();
        cierre.setHorasReales(leerDecimal("Horas reales (ej. 4.0): "));
        cierre.setMateriales(leerLineaOpcional("Materiales (Enter para omitir): "));
        cierre.setAveria(leerSiNo("Hubo averia? (s/n): "));
        if (cierre.isAveria()) {
            cierre.setDetalleAveria(leerLineaObligatoria("Detalle averia: "));
            cierre.setEstadoDevolucion(com.servihome.scgo.enums.EstadoDevolucion.AVERIADO);
        } else {
            cierre.setEstadoDevolucion(com.servihome.scgo.enums.EstadoDevolucion.BUEN_ESTADO);
        }
        return cierre;
    }

    private int leerEntero(String prompt) {
        while (true) {
            mostrarResultado(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                mostrarResultado("Ingrese un numero entero valido.");
            }
        }
    }

    private java.math.BigDecimal leerDecimal(String prompt) {
        while (true) {
            mostrarResultado(prompt);
            try {
                return new java.math.BigDecimal(scanner.nextLine().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                mostrarResultado("Ingrese un numero decimal valido.");
            }
        }
    }

    private boolean leerSiNo(String prompt) {
        while (true) {
            mostrarResultado(prompt);
            String r = scanner.nextLine().trim().toLowerCase();
            if (r.equals("s") || r.equals("si")) {
                return true;
            }
            if (r.equals("n") || r.equals("no")) {
                return false;
            }
            mostrarResultado("Responda s o n.");
        }
    }

    private String leerLineaOpcional(String prompt) {
        mostrarResultado(prompt);
        String linea = scanner.nextLine().trim();
        return linea.isEmpty() ? null : linea;
    }

    private String leerLineaObligatoria(String prompt) {
        while (true) {
            mostrarResultado(prompt);
            String linea = scanner.nextLine().trim();
            if (!linea.isEmpty()) {
                return linea;
            }
            mostrarResultado("Campo obligatorio.");
        }
    }

    private LocalDate leerFecha(String prompt) {
        mostrarResultado(prompt);
        try {
            return LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha invalida. Use formato AAAA-MM-DD");
        }
    }

    private LocalTime leerHora(String prompt) {
        mostrarResultado(prompt);
        try {
            return LocalTime.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Hora invalida. Use formato HH:MM");
        }
    }
}
