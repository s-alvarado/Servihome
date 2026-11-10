package com.servihome.scgo.registro;

import java.util.List;

/**
 * Bitacora del ciclo de la orden. Se escribe despues de confirmar la operacion
 * de negocio: el archivo no forma parte de la transaccion de la base.
 */
public interface RegistroOperaciones {

    void registrar(TipoEvento tipo, String detalle);

    /** Ultimas lineas, de la mas antigua a la mas reciente. */
    List<String> leerUltimos(int cantidad);
}
