package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Producto;
import com.sistemagestionapp.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Esta clase la utilizo como servicio para gestionar la lógica relacionada con los productos.
 * Me encargo de listar, guardar, buscar y eliminar productos utilizando el repositorio
 * {@link ProductoRepository}. De esta forma, separo la lógica de negocio del controlador.
 *
 * @author David Tomé Arnáiz
 */
@Service
public class ProductoService {

    /**
     * Inyecto el repositorio que me permite acceder a los datos de productos en la base de datos.
     */
    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Devuelvo la lista completa de productos almacenados en la base de datos.
     *
     * @return lista de productos.
     */
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    /**
     * Guardo un producto nuevo o actualizo uno existente.
     * Si el producto ya tiene ID, se actualiza; si no, se guarda como nuevo.
     *
     * @param producto objeto que quiero guardar o actualizar.
     * @return el producto guardado con sus datos actualizados.
     */
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    /**
     * Busco un producto por su ID. Si no lo encuentro, devuelvo {@code null}.
     *
     * @param id identificador del producto que quiero buscar.
     * @return el producto encontrado o {@code null} si no existe.
     */
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    /**
     * Elimino un producto de la base de datos usando su ID.
     *
     * @param id identificador del producto que quiero eliminar.
     */
    public void eliminar(Long id) {
        productoRepository.deleteById(id);
    }
}