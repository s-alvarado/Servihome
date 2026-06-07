package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoActivo;

public class Activo extends Recurso {

    private int idTipo;
    private EstadoActivo estado;

    public Activo() {
    }

    public Activo(int id, String descripcion, int idTipo, EstadoActivo estado) {
        super(id, descripcion);
        this.idTipo = idTipo;
        this.estado = estado;
    }

    @Override
    public boolean estaDisponible() {
        return estado == EstadoActivo.DISPONIBLE;
    }

    public int getIdActivo() {
        return id;
    }

    public void setIdActivo(int idActivo) {
        this.id = idActivo;
    }

    public int getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(int idTipo) {
        this.idTipo = idTipo;
    }

    public EstadoActivo getEstado() {
        return estado;
    }

    public void setEstado(EstadoActivo estado) {
        this.estado = estado;
    }
}
