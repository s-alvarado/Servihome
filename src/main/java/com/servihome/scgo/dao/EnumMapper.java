package com.servihome.scgo.dao;

import com.servihome.scgo.enums.EstadoActivo;
import com.servihome.scgo.enums.EstadoDevolucion;
import com.servihome.scgo.enums.EstadoOrden;
import com.servihome.scgo.enums.EstadoPersonal;
import com.servihome.scgo.enums.EstadoSolicitud;

/**
 * Traduccion explicita entre enums Java y columnas ENUM de MySQL.
 */
final class EnumMapper {

    private EnumMapper() {
    }

    static String toDb(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case PROGRAMADA -> "Programada";
            case REPROGRAMACION -> "Reprogramacion";
        };
    }

    static EstadoSolicitud fromDbEstadoSolicitud(String valor) {
        return switch (valor) {
            case "Pendiente" -> EstadoSolicitud.PENDIENTE;
            case "Programada" -> EstadoSolicitud.PROGRAMADA;
            case "Reprogramacion" -> EstadoSolicitud.REPROGRAMACION;
            default -> throw new IllegalArgumentException("Estado solicitud desconocido: " + valor);
        };
    }

    static String toDb(EstadoOrden estado) {
        return switch (estado) {
            case ASIGNADA -> "Asignada";
            case FINALIZADA -> "Finalizada";
            case CANCELADA -> "Cancelada";
        };
    }

    static EstadoOrden fromDbEstadoOrden(String valor) {
        return switch (valor) {
            case "Asignada" -> EstadoOrden.ASIGNADA;
            case "Finalizada" -> EstadoOrden.FINALIZADA;
            case "Cancelada" -> EstadoOrden.CANCELADA;
            default -> throw new IllegalArgumentException("Estado orden desconocido: " + valor);
        };
    }

    static String toDb(EstadoDevolucion estado) {
        return switch (estado) {
            case BUEN_ESTADO -> "Buen Estado";
            case AVERIADO -> "Averiado";
        };
    }

    static EstadoDevolucion fromDbEstadoDevolucion(String valor) {
        return switch (valor) {
            case "Buen Estado" -> EstadoDevolucion.BUEN_ESTADO;
            case "Averiado" -> EstadoDevolucion.AVERIADO;
            default -> throw new IllegalArgumentException("Estado devolucion desconocido: " + valor);
        };
    }

    static EstadoPersonal fromDbEstadoPersonal(String valor) {
        return switch (valor) {
            case "Activo" -> EstadoPersonal.ACTIVO;
            case "Licencia" -> EstadoPersonal.LICENCIA;
            case "Enfermedad" -> EstadoPersonal.ENFERMEDAD;
            default -> throw new IllegalArgumentException("Estado personal desconocido: " + valor);
        };
    }

    static EstadoActivo fromDbEstadoActivo(String valor) {
        return switch (valor) {
            case "Disponible" -> EstadoActivo.DISPONIBLE;
            case "En Reparacion" -> EstadoActivo.EN_REPARACION;
            case "Baja" -> EstadoActivo.BAJA;
            default -> throw new IllegalArgumentException("Estado activo desconocido: " + valor);
        };
    }
}
