package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoOrden;

import java.time.LocalDate;
import java.time.LocalTime;

public class OrdenTrabajo {

    private int idOrden;
    private int idSolicitud;
    private LocalDate fechaServicio;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoOrden estado;

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
