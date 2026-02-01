package com.sistemagestionapp.demojava.controller;

import com.sistemagestionapp.demojava.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model, Principal principal) {
        String correo = principal.getName();
        model.addAttribute("productos", productoService.listarDelUsuario(correo));
        model.addAttribute("producto", new com.sistemagestionapp.demojava.model.Producto()); // para el form
        return "productos";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, Principal principal) {
        String correo = principal.getName();
        Object producto = productoService.buscarDelUsuario(id, correo);

        model.addAttribute("productos", productoService.listarDelUsuario(correo));
        model.addAttribute("producto", producto);
        return "productos";
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