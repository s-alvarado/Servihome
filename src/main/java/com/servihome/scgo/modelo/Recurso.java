package com.servihome.scgo.modelo;

/**
 * Superclase abstracta de recursos asignables (Personal, Activo).
 */
public abstract class Recurso {

    protected int id;
    protected String descripcion;

    protected Recurso() {
    }

    protected Recurso(int id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    /** R2: cada subclase define cuando el recurso puede asignarse. */
    public abstract boolean estaDisponible();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
