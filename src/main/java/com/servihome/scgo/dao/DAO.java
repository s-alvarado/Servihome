package com.servihome.scgo.dao;

public interface DAO<T> {

    int insertar(T obj);

    T buscarPorId(int id);

    void actualizar(T obj);
}
