package com.sistemagestionapp.demojava.controller;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Muestra el formulario y el listado.
     * Si no se pasa producto concreto, se usa uno nuevo (crear).
     */
    @GetMapping("/productos")
    public String listarProductos(Model model) {
        List<Producto> productos = productoRepository.findAll();
        model.addAttribute("productos", productos);
        model.addAttribute("producto", new Producto()); // formulario en modo "nuevo"
        return "productos";
    }

    /**
     * Carga un producto para editarlo reutilizando la misma vista.
     */
    @GetMapping("/productos/editar/{id}")
    public String editarProducto(@PathVariable Long id, Model model) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));

        List<Producto> productos = productoRepository.findAll();

        model.addAttribute("productos", productos);
        model.addAttribute("producto", producto); // formulario en modo "editar"
        return "productos";
    }

    /**
     * Guarda un producto (nuevo o editado).
     * Si viene con id = null -> crea
     * Si viene con id != null -> actualiza
     */
    @PostMapping("/productos")
    public String guardarProducto(@ModelAttribute("producto") Producto producto) {
        productoRepository.save(producto);
        return "redirect:/productos";
    }

    /**
     * Elimina un producto.
     */
    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id) {
        productoRepository.deleteById(id);
        return "redirect:/productos";
    }
}