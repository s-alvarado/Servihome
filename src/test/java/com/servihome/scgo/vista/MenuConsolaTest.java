package com.servihome.scgo.vista;

import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.dao.ActivoDAO;
import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.PersonalDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuConsolaTest {

    @Test
    void salirMuestraMensajeDeDespedida() {
        OrdenController controller = new OrdenController(
                new OrdenTrabajoDAO(),
                new SolicitudDAO(),
                new AsignacionDAO(),
                new CierreOrdenDAO(),
                new PersonalDAO(),
                new ActivoDAO());
        Scanner scanner = new Scanner("0\n");
        GestionOrdenView view = new GestionOrdenView(controller, scanner);
        MenuConsola menu = new MenuConsola(view, controller, scanner);

        PrintStream original = System.out;
        ByteArrayOutputStream captura = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captura));
        try {
            menu.ejecutar();
        } finally {
            System.setOut(original);
        }

        String salida = captura.toString();
        assertTrue(salida.contains("SCGO - Menu principal"));
        assertTrue(salida.contains("Sesion finalizada"));
    }
}
