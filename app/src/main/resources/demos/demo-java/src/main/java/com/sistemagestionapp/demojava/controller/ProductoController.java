package com.sistemagestionapp.demojava.controller;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private static final String PRODUCTOS_VIEW = "productos";
    private static final String PRODUCTO_ATTR = "producto";

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model, Principal principal) {
        String correo = principal.getName();
        model.addAttribute(PRODUCTOS_VIEW, productoService.listarDelUsuario(correo));
        model.addAttribute(PRODUCTO_ATTR, new Producto());
        return PRODUCTOS_VIEW;
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, Principal principal) {
        String correo = principal.getName();
        Object producto = productoService.buscarDelUsuario(id, correo);

        model.addAttribute(PRODUCTOS_VIEW, productoService.listarDelUsuario(correo));
        model.addAttribute(PRODUCTO_ATTR, producto);
        return PRODUCTOS_VIEW;
    }

    @PostMapping
    public String guardar(@RequestParam(required = false) String id,
                          @RequestParam String nombre,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam double precio,
                          Principal principal) {
        String correo = principal.getName();
        productoService.guardar(id, nombre, descripcion, precio, correo);
        return "redirect:/productos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable String id, Principal principal) {
        String correo = principal.getName();
        productoService.borrar(id, correo);
        return "redirect:/productos";
    }
}