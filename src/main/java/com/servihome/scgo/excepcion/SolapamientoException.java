package com.servihome.scgo.excepcion;

/**
 * CU007: conflicto horario al asignar un recurso (R1).
 *
 * Es una excepcion de NEGOCIO distinta de los errores tecnicos (p. ej. DaoException
 * por fallas de JDBC). Separarlas permite que la vista capture este caso y muestre
 * un mensaje claro indicando el recurso en conflicto, en lugar de tratar la
 * sobreasignacion como un error de sistema. Es 'checked' (extends Exception) para
 * obligar al llamador a contemplar explicitamente el escenario de solapamiento.
 */
public class SolapamientoException extends Exception {

    public SolapamientoException(String message) {
        super(message);
    }
}
