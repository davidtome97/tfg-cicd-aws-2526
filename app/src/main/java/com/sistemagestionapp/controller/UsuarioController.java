package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * En este controlador gestiono todo lo relacionado con los usuarios del sistema.
 * Me encargo del login, el registro y la administración básica de usuarios
 * (crear, editar, listar y eliminar).
 */
@Controller
public class UsuarioController {

    // Servicio que utilizo para acceder a la lógica de negocio de los usuarios
    @Autowired
    private UsuarioService usuarioService;

    // ---------------- LOGIN ----------------

    /**
     * Muestro el formulario de login.
     * También gestiono los mensajes que llegan por parámetros en la URL,
     * como errores de autenticación, logout correcto o registro exitoso.
     */
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

    /**
     * Muestro el formulario de registro de nuevos usuarios.
     * Inicializo un objeto Usuario vacío para enlazarlo con el formulario.
     */
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    /**
     * Proceso el registro de un nuevo usuario.
     * Compruebo que las contraseñas coincidan y que el correo no exista ya.
     * La contraseña se guarda encriptada en el servicio, no aquí.
     */
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

        // Aquí delego el guardado al servicio, que se encarga de encriptar la contraseña
        usuarioService.guardarUsuario(usuario);

        return "redirect:/login?registro_ok";
    }

    // ---------------- GESTIÓN USUARIOS ----------------

    /**
     * Muestro la lista completa de usuarios del sistema.
     * Este método se utiliza para la parte de administración.
     */
    @GetMapping("/usuarios")
    public String verUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    /**
     * Creo un nuevo usuario desde el panel de administración.
     * Verifico que el correo no esté duplicado antes de guardarlo.
     */
    @PostMapping("/usuarios")
    public String crearUsuario(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioService.existePorCorreo(usuario.getCorreo())) {
            model.addAttribute("usuarios", usuarioService.listarUsuarios());
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "usuarios";
        }

        // La contraseña llega en texto plano y se encripta en el servicio
        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    /**
     * Muestro el formulario de edición de un usuario existente.
     * Cargo los datos actuales para que puedan modificarse.
     */
    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        return "editar";
    }

    /**
     * Actualizo los datos de un usuario.
     * Si se introduce una nueva contraseña, la guardo encriptada.
     * Si no, mantengo la contraseña anterior sin modificarla.
     */
    @PostMapping("/usuarios/actualizar")
    public String actualizarUsuario(@ModelAttribute Usuario usuario,
                                    @RequestParam(required = false) String password) {

        if (password != null && !password.isBlank()) {
            // Si hay nueva contraseña, la paso al servicio para que la encripte
            usuario.setPassword(password);
        } else {
            // Si no, conservo la contraseña almacenada en base de datos
            String anterior = usuarioService.obtenerUsuarioPorId(usuario.getId()).getPassword();
            usuario.setPassword(anterior);
        }

        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    /**
     * Elimino un usuario del sistema a partir de su identificador.
     */
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}