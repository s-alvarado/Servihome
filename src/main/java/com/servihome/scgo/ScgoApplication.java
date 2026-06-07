package com.servihome.scgo;

import com.servihome.scgo.config.DatabaseBootstrap;
import com.servihome.scgo.controlador.OrdenController;
import com.servihome.scgo.dao.ActivoDAO;
import com.servihome.scgo.dao.AsignacionDAO;
import com.servihome.scgo.dao.CierreOrdenDAO;
import com.servihome.scgo.dao.DAO;
import com.servihome.scgo.dao.OrdenTrabajoDAO;
import com.servihome.scgo.dao.PersonalDAO;
import com.servihome.scgo.dao.SolicitudDAO;
import com.servihome.scgo.modelo.CierreOrden;
import com.servihome.scgo.modelo.OrdenTrabajo;
import com.servihome.scgo.modelo.Solicitud;
import com.servihome.scgo.vista.GestionOrdenView;
import com.servihome.scgo.vista.MenuConsola;

import java.util.Scanner;

/**
 * Ensambla dependencias y arranca la aplicacion.
 */
public final class ScgoApplication {

    private ScgoApplication() {
    }

    public static void run(String[] args) {
        DatabaseBootstrap.verificarConexion();
        DatabaseBootstrap.inicializarSiEsNecesario();

        DAO<OrdenTrabajo> ordenDAO = new OrdenTrabajoDAO();
        DAO<Solicitud> solicitudDAO = new SolicitudDAO();
        AsignacionDAO asignacionDAO = new AsignacionDAO();
        DAO<CierreOrden> cierreDAO = new CierreOrdenDAO();
        PersonalDAO personalDAO = new PersonalDAO();
        ActivoDAO activoDAO = new ActivoDAO();

        OrdenController controller = new OrdenController(
                (OrdenTrabajoDAO) ordenDAO,
                (SolicitudDAO) solicitudDAO,
                asignacionDAO,
                (CierreOrdenDAO) cierreDAO,
                personalDAO,
                activoDAO);

        Scanner scanner = new Scanner(System.in);
        GestionOrdenView view = new GestionOrdenView(controller, scanner);
        MenuConsola menu = new MenuConsola(view, controller, scanner);
        menu.ejecutar();
    }
}
