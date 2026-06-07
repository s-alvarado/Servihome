package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoPersonal;

public class Personal extends Recurso {

    private int idEspecialidad;
    private EstadoPersonal estado;

    public Personal() {
    }

    public Personal(int id, String nombre, int idEspecialidad, EstadoPersonal estado) {
        super(id, nombre);
        this.idEspecialidad = idEspecialidad;
        this.estado = estado;
    }

    @Override
    public boolean estaDisponible() {
        return estado == EstadoPersonal.ACTIVO;
    }

    public int getIdPersonal() {
        return id;
    }

    public void setIdPersonal(int idPersonal) {
        this.id = idPersonal;
    }

    public int getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(int idEspecialidad) {
        this.idEspecialidad = idEspecialidad;
    }

    public String getNombre() {
        return descripcion;
    }

    public void setNombre(String nombre) {
        this.descripcion = nombre;
    }

    public EstadoPersonal getEstado() {
        return estado;
    }

    public void setEstado(EstadoPersonal estado) {
        this.estado = estado;
    }
}
