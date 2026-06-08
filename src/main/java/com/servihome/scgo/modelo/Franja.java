package com.servihome.scgo.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Value object para el rango horario usado en la validacion de solapamiento (R1).
 */
public class Franja {

    // LocalDate/LocalTime (java.time): tipos inmutables que separan fecha y hora,
    // comparables directamente y sin la ambiguedad de zona horaria de java.util.Date.
    private LocalDate fecha;
    private LocalTime inicio;
    private LocalTime fin;

    public Franja() {
    }

    public Franja(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        this.fecha = fecha;
        this.inicio = inicio;
        this.fin = fin;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalTime inicio) {
        this.inicio = inicio;
    }

    public LocalTime getFin() {
        return fin;
    }

    public void setFin(LocalTime fin) {
        this.fin = fin;
    }
}
