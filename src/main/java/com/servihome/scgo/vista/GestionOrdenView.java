package com.servihome.scgo.vista;

import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.excepcion.SolapamientoException;
import com.servihome.scgo.modelo.Activo;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.modelo.Personal;
import com.servihome.scgo.modelo.Recurso;
import com.servihome.scgo.modelo.Solicitud;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
            controller.validarYAsignar(idOrden, idRecurso, tipo);
            mostrarResultado("Recurso asignado correctamente a la orden " + idOrden);
        } catch (SolapamientoException e) {
            mostrarResultado("Solapamiento detectado: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            mostrarResultado("Asignacion rechazada: " + e.getMessage());
        } catch (RuntimeException e) {
            mostrarResultado("Error al asignar recurso: " + e.getMessage());
        }
    }

    /** CU008 sin averia. Conserva la firma anterior delegando en un arreglo vacio. */
    public void registrarCierre(int idOrden, CierreOrden cierre) {
        registrarCierre(idOrden, cierre, new int[0]);
    }

    /** CU008: dispara cerrarOrden, con los activos averiados si los hay. */
    public void registrarCierre(int idOrden, CierreOrden cierre, int[] idsActivosAveriados) {
        try {
            controller.cerrarOrden(idOrden, cierre, idsActivosAveriados);
            if (idsActivosAveriados.length == 0) {
                mostrarResultado("Orden " + idOrden + " cerrada correctamente (FINALIZADA)");
            } else {
                mostrarResultado("Orden " + idOrden
                        + " cerrada correctamente (FINALIZADA). Activos enviados a reparacion: "
                        + formatearIds(idsActivosAveriados));
            }
        } catch (IllegalArgumentException e) {
            mostrarResultado("Error de validacion al cerrar: " + e.getMessage());
        } catch (RuntimeException e) {
            mostrarResultado("Error al cerrar orden: " + e.getMessage());
        }
    }

    /**
     * Lectura unica del cierre (opcion 3 y ciclo guiado). Si hay averia, los ids
     * se capturan en una lista de tamano variable y se copian a un arreglo fijo
     * antes de llamar al controlador.
     */
    public void capturarYRegistrarCierre(int idOrden) {
        CierreOrden cierre = leerCierre();
        int[] idsActivosAveriados = new int[0];
        if (cierre.isAveria()) {
            idsActivosAveriados = capturarActivosAveriados(idOrden);
            if (idsActivosAveriados == null) {
                return;
            }
        }
        registrarCierre(idOrden, cierre, idsActivosAveriados);
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
        capturarYRegistrarCierre(idOrden);

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

        mostrarResultado("--- Recursos operativos (polimorfismo) ---");
        for (Recurso recurso : controller.listarRecursosOperativos()) {
            mostrarResultado("  id=" + recurso.getId()
                    + " | " + recurso.getDescripcion()
                    + " | disponible=" + recurso.estaDisponible());
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
            mostrarResultado("  id=" + p.getId() + " | " + p.getNombre() + " | " + p.getEstado());
        }
    }

    private void mostrarActivos(List<Activo> activos) {
        if (activos.isEmpty()) {
            mostrarResultado("(ninguno)");
            return;
        }
        for (Activo a : activos) {
            mostrarResultado("  id=" + a.getId() + " | " + a.getDescripcion() + " | " + a.getEstado());
        }
    }

    private CierreOrden leerCierre() {
        CierreOrden cierre = new CierreOrden();
        cierre.setHorasReales(leerDecimal("Horas reales (ej. 4.5): "));
        cierre.setMateriales(leerLineaOpcional("Materiales utilizados (Enter para omitir): "));
        cierre.setAveria(leerSiNo("Hubo averia en algun activo? (s/n): "));
        if (cierre.isAveria()) {
            cierre.setDetalleAveria(leerLineaObligatoria("Detalle de la averia: "));
            cierre.setEstadoDevolucion(EstadoDevolucion.AVERIADO);
        } else {
            cierre.setEstadoDevolucion(EstadoDevolucion.BUEN_ESTADO);
        }
        return cierre;
    }

    /**
     * @return arreglo de ids, o null si el usuario no completo una seleccion valida
     */
    private int[] capturarActivosAveriados(int idOrden) {
        List<Activo> asignados = controller.listarActivosDeOrden(idOrden);
        mostrarResultado("--- Activos asignados a la orden " + idOrden + " ---");
        mostrarActivos(asignados);
        if (asignados.isEmpty()) {
            mostrarResultado("La orden no tiene activos asignados. No se puede registrar la averia.");
            return null;
        }

        // La cantidad de averiados no se conoce de antemano: se usa una lista.
        ArrayList<Integer> capturados = new ArrayList<>();
        while (true) {
            int id = leerEntero("ID de activo averiado, 0 para terminar: ");
            if (id == 0) {
                break;
            }
            if (!activoEnLista(asignados, id)) {
                mostrarResultado("El activo id=" + id + " no esta asignado a la orden.");
                continue;
            }
            if (capturados.contains(id)) {
                mostrarResultado("El activo id=" + id + " ya fue indicado.");
                continue;
            }
            capturados.add(id);
        }
        if (capturados.isEmpty()) {
            mostrarResultado("Debe indicar al menos un activo averiado.");
            return null;
        }

        // I4: arreglo de tamano fijo; la lista solo sirvio para capturar
        int[] ids = new int[capturados.size()];
        for (int i = 0; i < capturados.size(); i++) {
            ids[i] = capturados.get(i);
        }
        return ids;
    }

    private boolean activoEnLista(List<Activo> asignados, int idActivo) {
        for (Activo activo : asignados) {
            if (activo.getId() == idActivo) {
                return true;
            }
        }
        return false;
    }

    private String formatearIds(int[] ids) {
        StringBuilder texto = new StringBuilder("[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(ids[i]);
        }
        texto.append(']');
        return texto.toString();
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

    private BigDecimal leerDecimal(String prompt) {
        while (true) {
            mostrarResultado(prompt);
            try {
                return new BigDecimal(scanner.nextLine().trim().replace(',', '.'));
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
