package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoActivo;

public class Activo {

    private int idActivo;
    private int idTipo;
    private String descripcion;
    private EstadoActivo estado;

    /** R2: solo DISPONIBLE puede asignarse. */
    public boolean estaDisponible() {
        return estado == EstadoActivo.DISPONIBLE;
    }

    public int getIdActivo() {
        return idActivo;
    }

    public void setIdActivo(int idActivo) {
        this.idActivo = idActivo;
    }

    public int getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(int idTipo) {
        this.idTipo = idTipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoActivo getEstado() {
        return estado;
    }

    public void setEstado(EstadoActivo estado) {
        this.estado = estado;
    }
}
