package com.sistemagestionapp.demojava.controller;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute("usuario") Usuario usuario, Model model) {

        if (usuarioService.existePorCorreo(usuario.getCorreo())) {
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "registro";
        }

        usuarioService.registrarUsuario(usuario);
        return "redirect:/login?registroOk";
    }
}