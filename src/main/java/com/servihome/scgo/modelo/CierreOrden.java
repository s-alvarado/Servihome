package com.servihome.scgo.modelo;

import com.servihome.scgo.enums.EstadoDevolucion;

import java.math.BigDecimal;

/**
 * Datos del cierre de una orden (CU008). Encapsulamiento: atributos privados con
 * acceso por setters/getters; las invariantes de negocio (R5: si hay averia, el
 * detalle es obligatorio y el estado debe ser AVERIADO) se validan en el controlador
 * antes de persistir, manteniendo la entidad como portadora de datos consistente.
 */
public class CierreOrden {

    private int idCierre;
    private int idOrden;
    // BigDecimal y no double: las horas reales son un valor con decimales sobre el
    // que puede haber calculos/facturacion; double introduce errores de redondeo
    // binario. BigDecimal da precision decimal exacta.
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
