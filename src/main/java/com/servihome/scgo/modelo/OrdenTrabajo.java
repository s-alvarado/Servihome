package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoOrden;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Orden de trabajo. Encapsulamiento: todos los atributos son privados y solo se
 * acceden/modifican via getters/setters. En particular, el ciclo de vida (estado)
 * no se manipula desde afuera tocando el campo: el cambio de estado pasa por el
 * controlador (CU006/CU008) dentro de su transaccion, para no dejar la orden en un
 * estado invalido respecto de la solicitud y el cierre asociados.
 */
public class OrdenTrabajo {

    private int idOrden;
    private int idSolicitud;
    // LocalDate/LocalTime (java.time) en vez de java.util.Date: son inmutables,
    // sin componente de zona horaria y separan fecha de hora, justo lo que necesita
    // la franja del servicio y la comparacion de solapamiento (R1).
    private LocalDate fechaServicio;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoOrden estado;

    public OrdenTrabajo() {
    }

    public OrdenTrabajo(int idSolicitud, LocalDate fechaServicio,
                        LocalTime horaInicio, LocalTime horaFin, EstadoOrden estado) {
        this.idSolicitud = idSolicitud;
        this.fechaServicio = fechaServicio;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = estado;
    }

    /** R3 invariante: horaFin > horaInicio (validar en capa de aplicacion). */
    public Franja getFranjaHoraria() {
        return new Franja(fechaServicio, horaInicio, horaFin);
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public int getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(int idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public LocalDate getFechaServicio() {
        return fechaServicio;
    }

    public void setFechaServicio(LocalDate fechaServicio) {
        this.fechaServicio = fechaServicio;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }
}
