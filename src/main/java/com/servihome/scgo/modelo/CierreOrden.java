package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoDevolucion;

import java.math.BigDecimal;

public class CierreOrden {

    private int idCierre;
    private int idOrden;
    private BigDecimal horasReales;
    private String materiales;
    private EstadoDevolucion estadoDevolucion;
    private boolean averia;
    private String detalleAveria;

    public CierreOrden() {
    }

    public CierreOrden(BigDecimal horasReales, String materiales,
                       EstadoDevolucion estadoDevolucion, boolean averia, String detalleAveria) {
        this.horasReales = horasReales;
        this.materiales = materiales;
        this.estadoDevolucion = estadoDevolucion;
        this.averia = averia;
        this.detalleAveria = detalleAveria;
    }

    public int getIdCierre() {
        return idCierre;
    }

    public void setIdCierre(int idCierre) {
        this.idCierre = idCierre;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public BigDecimal getHorasReales() {
        return horasReales;
    }

    public void setHorasReales(BigDecimal horasReales) {
        this.horasReales = horasReales;
    }

    public String getMateriales() {
        return materiales;
    }

    public void setMateriales(String materiales) {
        this.materiales = materiales;
    }

    public EstadoDevolucion getEstadoDevolucion() {
        return estadoDevolucion;
    }

    public void setEstadoDevolucion(EstadoDevolucion estadoDevolucion) {
        this.estadoDevolucion = estadoDevolucion;
    }

    public boolean isAveria() {
        return averia;
    }

    public void setAveria(boolean averia) {
        this.averia = averia;
    }

    public String getDetalleAveria() {
        return detalleAveria;
    }

    public void setDetalleAveria(String detalleAveria) {
        this.detalleAveria = detalleAveria;
    }
}
