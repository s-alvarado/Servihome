package com.servihome.scgo.modelo;

/**
 * Abstrae lo comun de Personal y Activo: ambos son recursos asignables a una
 * orden, identificados y con una nocion de disponibilidad. Se modela como clase
 * abstracta (y no como dos clases independientes) para habilitar el polimorfismo
 * en la validacion del CU007: el controlador trabaja con List&lt;Recurso&gt; e
 * invoca {@link #estaDisponible()} sin conocer el tipo concreto.
 */
public abstract class Recurso {

    // Atributos comunes en 'protected' para que las subclases los hereden;
    // id y descripcion son el minimo que todo recurso asignable debe tener.
    protected int id;
    protected String descripcion;

    protected Recurso() {
    }

    protected Recurso(int id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    /**
     * R2: contrato de disponibilidad. Es abstracto porque la regla de "operativo"
     * difiere por tipo (Personal ACTIVO vs Activo DISPONIBLE); cada subclase la define.
     */
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
