package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoPersonal;

public class Personal {

    private int idPersonal;
    private int idEspecialidad;
    private String nombre;
    private EstadoPersonal estado;

    /** R2: solo ACTIVO puede asignarse. */
    public boolean estaOperativo() {
        return estado == EstadoPersonal.ACTIVO;
    }

    public int getIdPersonal() {
        return idPersonal;
    }

    public void setIdPersonal(int idPersonal) {
        this.idPersonal = idPersonal;
    }

    public int getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(int idEspecialidad) {
        this.idEspecialidad = idEspecialidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public EstadoPersonal getEstado() {
        return estado;
    }

    public void setEstado(EstadoPersonal estado) {
        this.estado = estado;
    }
}
