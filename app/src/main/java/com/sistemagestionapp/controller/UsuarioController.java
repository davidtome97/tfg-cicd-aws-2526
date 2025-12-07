package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador principal para gestionar las operaciones relacionadas con los usuarios del sistema.
 * Me encargo de:
 * - Mostrar el formulario de login.
 * - Listar usuarios.
 * - Crear nuevos usuarios.
 * - Editar y eliminar usuarios.
 * - Gestionar el registro de nuevos usuarios.
 */
@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // ---------------- LOGIN ----------------

    @GetMapping("/login")
    public String mostrarFormularioLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registro_ok", required = false) String registro_ok,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("mensaje", "Has cerrado sesión correctamente.");
        }
        if (registro_ok != null) {
            model.addAttribute("mensaje", "Registro realizado correctamente. Ya puedes iniciar sesión.");
        }

        return "login";
    }

    // ---------------- REGISTRO ----------------

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("usuario") Usuario usuario,
                                   @RequestParam("passwordRepetida") String passwordRepetida,
                                   Model model) {

        if (!usuario.getPassword().equals(passwordRepetida)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "registro";
        }

        if (usuarioService.existePorCorreo(usuario.getCorreo())) {
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "registro";
        }

        // IMPORTANTE: aquí NO encripto, lo hará UsuarioService.guardarUsuario()
        usuarioService.guardarUsuario(usuario);

        return "redirect:/login?registro_ok";
    }

    // ---------------- GESTIÓN USUARIOS ----------------

    @GetMapping("/usuarios")
    public String verUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioService.existePorCorreo(usuario.getCorreo())) {
            model.addAttribute("usuarios", usuarioService.listarUsuarios());
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "usuarios";
        }

        // Contraseña en texto plano → el servicio la encripta
        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        return "editar";
    }

    @PostMapping("/usuarios/actualizar")
    public String actualizarUsuario(@ModelAttribute Usuario usuario,
                                    @RequestParam(required = false) String password) {

        if (password != null && !password.isBlank()) {
            // Nueva contraseña en texto plano → el servicio la encripta
            usuario.setPassword(password);
        } else {
            // Mantengo la contraseña anterior tal cual está en BD
            String anterior = usuarioService.obtenerUsuarioPorId(usuario.getId()).getPassword();
            usuario.setPassword(anterior);
        }

        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}