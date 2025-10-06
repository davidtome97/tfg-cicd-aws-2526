package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Producto;
import com.sistemagestionapp.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Esta clase la utilizo como controlador para gestionar todas las operaciones relacionadas
 * con los productos dentro de la aplicación.
 * Me encargo de mostrar la lista de productos, así como de permitir la creación, edición
 * y eliminación de los mismos. Utilizo el servicio {@link ProductoService} para realizar
 * las operaciones con los datos.
 *
 * Todas las rutas de esta clase están prefijadas con "/productos".
 *
 * @author David Tomé Arnáiz
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    /**
     * Inyecto el servicio de productos para poder realizar operaciones como listar,
     * guardar, buscar y eliminar productos.
     */
    @Autowired
    private ProductoService productoService;

    /**
     * Muestro la lista de productos disponibles en la vista "productos.html".
     *
     * @param model objeto que utilizo para pasar los datos a la vista.
     * @return el nombre de la plantilla que muestra la lista de productos.
     */
    @GetMapping
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "productos";
    }

    /**
     * Guardo un nuevo producto recibido desde un formulario.
     *
     * @param producto objeto que representa el producto a guardar.
     * @return redirijo a la lista de productos para mostrar los datos actualizados.
     */
    @PostMapping
    public String guardarProducto(@ModelAttribute Producto producto) {
        productoService.guardar(producto);
        return "redirect:/productos";
    }

    /**
     * Elimino un producto en base a su ID.
     *
     * @param id identificador del producto que quiero eliminar.
     * @return redirijo a la lista de productos una vez eliminado.
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id) {
        productoService.eliminar(id);
        return "redirect:/productos";
    }

    /**
     * Cargo el formulario de edición con los datos del producto correspondiente al ID indicado.
     *
     * @param id identificador del producto que quiero editar.
     * @param model objeto que utilizo para pasar los datos a la vista.
     * @return el nombre de la plantilla de edición del producto.
     */
    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable Long id, Model model) {
        Producto producto = productoService.buscarPorId(id);
        model.addAttribute("producto", producto);
        return "editar_producto";
    }

    /**
     * Guardo los cambios realizados sobre un producto existente.
     *
     * @param producto objeto con los datos actualizados del producto.
     * @return redirijo a la lista de productos para reflejar los cambios.
     */
    @PostMapping("/actualizar")
    public String actualizarProducto(@ModelAttribute Producto producto) {
        productoService.guardar(producto);
        return "redirect:/productos";
    }
}