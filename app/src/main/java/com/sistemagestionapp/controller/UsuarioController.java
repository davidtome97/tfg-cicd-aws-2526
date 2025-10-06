package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Esta clase la utilizo como controlador principal para gestionar todas las operaciones
 * relacionadas con los usuarios del sistema.
 * Me encargo de mostrar el formulario de login, listar usuarios, crear nuevos,
 * editar existentes y eliminarlos. Trabajo en conjunto con el servicio
 * {@link UsuarioService} para acceder a la lógica de negocio.
 *
 * @author David Tomé Arnáiz
 */
@Controller
public class UsuarioController {

    /**
     * Inyecto el servicio de usuarios que me permite acceder, guardar, actualizar
     * y eliminar usuarios del sistema.
     */
    @Autowired
    private UsuarioService usuarioService;

    /**
     * Muestro el formulario de login con mensajes personalizados en caso de error o cierre de sesión.
     *
     * @param error  indica si hubo un fallo de autenticación.
     * @param logout indica si el usuario cerró sesión correctamente.
     * @param model  modelo para pasar datos a la vista.
     * @return el nombre de la plantilla del formulario de login.
     */
    @GetMapping("/login")
    public String mostrarFormularioLogin(@RequestParam(value = "error", required = false) String error,
                                         @RequestParam(value = "logout", required = false) String logout,
                                         Model model) {
        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("mensaje", "Has cerrado sesión correctamente.");
        }
        return "login";
    }

    /**
     * Muestro la lista completa de usuarios en la vista "usuarios.html".
     *
     * @param model modelo para pasar la lista de usuarios a la vista.
     * @return el nombre de la plantilla que muestra los usuarios.
     */
    @GetMapping("/usuarios")
    public String verUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    /**
     * Creo un nuevo usuario a partir de los datos recibidos desde el formulario.
     * Si ya existe un usuario con el mismo correo, muestro un mensaje de error.
     *
     * @param usuario objeto con los datos del nuevo usuario.
     * @param model   modelo para pasar información a la vista en caso de error.
     * @return redirijo a la lista de usuarios o recargo el formulario con errores.
     */
    @PostMapping("/usuarios")
    public String crearUsuario(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioService.existePorCorreo(usuario.getCorreo())) {
            model.addAttribute("usuarios", usuarioService.listarUsuarios());
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "usuarios";
        }

        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    /**
     * Muestro el formulario de edición de un usuario a partir de su ID.
     *
     * @param id    identificador del usuario a editar.
     * @param model modelo para pasar los datos del usuario a la vista.
     * @return el nombre de la plantilla de edición.
     */
    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        return "editar";
    }

    /**
     * Actualizo los datos de un usuario existente. Si la nueva contraseña está vacía,
     * conservo la anterior.
     *
     * @param usuario  objeto con los datos actualizados del usuario.
     * @param password nueva contraseña (opcional).
     * @return redirijo a la lista de usuarios.
     */
    @PostMapping("/usuarios/actualizar")
    public String actualizarUsuario(@ModelAttribute Usuario usuario,
                                    @RequestParam(required = false) String password) {
        if (password != null && !password.isBlank()) {
            usuario.setPassword(password);
        } else {
            String anterior = usuarioService.obtenerUsuarioPorId(usuario.getId()).getPassword();
            usuario.setPassword(anterior);
        }

        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    /**
     * Elimino un usuario del sistema utilizando su ID.
     *
     * @param id identificador del usuario a eliminar.
     * @return redirijo a la lista de usuarios.
     */
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}