package com.servihome.scgo.dao;

/**
 * Contrato de persistencia (patron DAO). Define las operaciones de acceso a datos
 * de forma abstracta para desacoplar al controlador del detalle de JDBC/MySQL:
 * el controlador depende de esta interfaz, no de las consultas SQL ni del driver
 * concreto, de modo que la implementacion pueda cambiar sin tocar la logica de negocio.
 *
 * @param <T> entidad del modelo gestionada por el DAO
 */
public interface DAO<T> {

    int insertar(T obj);

    T buscarPorId(int id);

    void actualizar(T obj);
}
