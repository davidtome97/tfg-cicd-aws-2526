package com.sistemagestionapp.demojava.controller;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import com.sistemagestionapp.demojava.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import java.util.List;



@Controller
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Muestra listado + formulario (nuevo o editar).
     */
    @GetMapping("/productos")
    public String listarProductos(Model model) {

        List<?> productos = productoService.listarTodos();

        model.addAttribute("productos", productos);

        // El formulario puede manejar ambos modelos (SQL o Mongo)
        model.addAttribute("producto", new Producto());

        return "productos";
    }

    /**
     * Cargar un producto para editarlo.
     * Para Mongo el id es String, para SQL es Long → por eso usamos String.
     */
    @GetMapping("/productos/editar/{id}")
    public String editarProducto(@PathVariable String id, Model model) {

        List<?> productos = productoService.listarTodos();
        model.addAttribute("productos", productos);

        // Buscar dependiendo del motor
        Object producto = productoService.buscarPorId(id);
        model.addAttribute("producto", producto);

        return "productos";
    }

    /**
     * Guardar producto (crear o actualizar).
     */
    @PostMapping("/productos")
    public String guardarProducto(
            @RequestParam(required = false) String id,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam double precio
    ) {
        productoService.guardar(id, nombre, descripcion, precio);
        return "redirect:/productos";
    }

    /**
     * Eliminar producto.
     */
    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable String id) {
        productoService.borrarPorId(id);
        return "redirect:/productos";
    }
}