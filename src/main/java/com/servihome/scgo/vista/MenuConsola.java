package com.servihome.scgo.vista;

import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.enums.TipoRecurso;

import java.util.Scanner;

/**
 * Menu interactivo de consola para los casos de uso CU006, CU007 y CU008.
 */
public class MenuConsola {

    // I4: arreglo de opciones del menu, de tamano fijo y conocido
    private static final String[] OPCIONES_MENU = {
            "  1. Generar orden de trabajo      (CU006)",
            "  2. Asignar recurso a orden       (CU007)",
            "  3. Cerrar orden de trabajo       (CU008)",
            "  4. Consultar estado del sistema",
            "  5. Ciclo completo guiado         (CU006->007->008)",
            "  6. Ver registro de operaciones (ultimas 10)",
            "  0. Salir"
    };

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
        for (int i = 0; i < OPCIONES_MENU.length; i++) {
            System.out.println(OPCIONES_MENU[i]);
        }
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
        view.capturarYRegistrarCierre(idOrden);
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
}
