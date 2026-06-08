package com.servihome.scgo.vista;

import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.TipoRecurso;
import com.servihome.scgo.modelo.CierreOrden;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Menu interactivo de consola para los casos de uso CU006, CU007 y CU008.
 */
public class MenuConsola {

    private final GestionOrdenView view;
    private final OrdenController controller;
    private final Scanner scanner;
    private boolean activo = true;

    public MenuConsola(GestionOrdenView view, OrdenController controller) {
        this(view, controller, new Scanner(System.in));
    }

    public MenuConsola(GestionOrdenView view, Scanner scanner) {
        this(view, null, scanner);
    }

    public MenuConsola(GestionOrdenView view, OrdenController controller, Scanner scanner) {
        this.view = view;
        this.controller = controller;
        this.scanner = scanner;
    }

    public void ejecutar() {
        view.mostrarResultado("Bienvenido a SCGO - ServiHome (MVP)");
        view.mostrarResultado("Datos demo: solicitud PENDIENTE id=1, personal 1-2, activos 1-2.");
        while (activo) {
            mostrarMenu();
            int opcion = leerEntero("Seleccione una opcion: ");
            procesarOpcion(opcion);
        }
        view.mostrarResultado("Sesion finalizada.");
    }

    void detener() {
        activo = false;
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("========== SCGO - Menu principal ==========");
        System.out.println("  1. Generar orden de trabajo      (CU006)");
        System.out.println("  2. Asignar recurso a orden       (CU007)");
        System.out.println("  3. Cerrar orden de trabajo       (CU008)");
        System.out.println("  4. Consultar estado del sistema");
        System.out.println("  5. Ciclo completo guiado         (CU006->007->008)");
        System.out.println("  0. Salir");
        System.out.println("===========================================");
    }

    private void procesarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> generarOrden();
            case 2 -> asignarRecurso();
            case 3 -> cerrarOrden();
            case 4 -> view.mostrarEstadoSistema();
            case 5 -> view.ejecutarCicloCompleto();
            case 0 -> detener();
            default -> view.mostrarResultado("Opcion invalida. Intente nuevamente.");
        }
    }

    private void generarOrden() {
        view.mostrarResultado("--- CU006: Generar orden ---");
        if (controller != null) {
            view.mostrarEstadoSistema();
        }
        int idSolicitud = leerEntero("ID de solicitud: ");
        view.seleccionarSolicitud(idSolicitud);
    }

    private void asignarRecurso() {
        view.mostrarResultado("--- CU007: Asignar recurso ---");
        if (controller != null) {
            view.mostrarEstadoSistema();
        }
        int idOrden = leerEntero("ID de orden: ");
        int idRecurso = leerEntero("ID de recurso (personal o activo): ");
        TipoRecurso tipo = leerTipoRecurso();
        view.asignarRecurso(idOrden, idRecurso, tipo);
    }

    private void cerrarOrden() {
        view.mostrarResultado("--- CU008: Cerrar orden ---");
        if (controller != null) {
            view.mostrarEstadoSistema();
        }
        int idOrden = leerEntero("ID de orden: ");
        CierreOrden cierre = leerCierre();
        view.registrarCierre(idOrden, cierre);
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

    private TipoRecurso leerTipoRecurso() {
        while (true) {
            view.mostrarResultado("Tipo de recurso: 1=Personal, 2=Activo");
            int tipo = leerEntero("Seleccione: ");
            if (tipo == 1) {
                return TipoRecurso.PERSONAL;
            }
            if (tipo == 2) {
                return TipoRecurso.ACTIVO;
            }
            view.mostrarResultado("Tipo invalido.");
        }
    }

    private int leerEntero(String prompt) {
        while (true) {
            view.mostrarResultado(prompt);
            String linea = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                view.mostrarResultado("Ingrese un numero entero valido.");
            }
        }
    }

    private BigDecimal leerDecimal(String prompt) {
        while (true) {
            view.mostrarResultado(prompt);
            String linea = scanner.nextLine().trim().replace(',', '.');
            try {
                return new BigDecimal(linea);
            } catch (NumberFormatException e) {
                view.mostrarResultado("Ingrese un numero decimal valido.");
            }
        }
    }

    private boolean leerSiNo(String prompt) {
        while (true) {
            view.mostrarResultado(prompt);
            String respuesta = scanner.nextLine().trim().toLowerCase();
            if (respuesta.equals("s") || respuesta.equals("si")) {
                return true;
            }
            if (respuesta.equals("n") || respuesta.equals("no")) {
                return false;
            }
            view.mostrarResultado("Responda s o n.");
        }
    }

    private String leerLineaOpcional(String prompt) {
        view.mostrarResultado(prompt);
        String linea = scanner.nextLine().trim();
        return linea.isEmpty() ? null : linea;
    }

    private String leerLineaObligatoria(String prompt) {
        while (true) {
            view.mostrarResultado(prompt);
            String linea = scanner.nextLine().trim();
            if (!linea.isEmpty()) {
                return linea;
            }
            view.mostrarResultado("Este campo es obligatorio.");
        }
    }
}
