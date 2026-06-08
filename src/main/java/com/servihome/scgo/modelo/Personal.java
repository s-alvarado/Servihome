package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoPersonal;

/**
 * Recurso humano. Hereda id/descripcion de {@link Recurso} y agrega lo propio
 * del personal (especialidad y estado laboral).
 */
public class Personal extends Recurso {

    private int idEspecialidad;
    private EstadoPersonal estado;

    public Personal() {
    }

    public Personal(int id, String nombre, int idEspecialidad, EstadoPersonal estado) {
        // super(...) inicializa la parte heredada (id y descripcion/nombre);
        // la subclase solo se ocupa de sus atributos especificos.
        super(id, nombre);
        this.idEspecialidad = idEspecialidad;
        this.estado = estado;
    }

    /** Regla concreta del personal: operativo solo si su estado es ACTIVO. */
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
