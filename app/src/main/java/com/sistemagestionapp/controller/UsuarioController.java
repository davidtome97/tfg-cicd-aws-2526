package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * En este controlador gestiono las operaciones relacionadas con los usuarios del sistema.
 *
 * Me encargo de:
 * - mostrar y procesar el login,
 * - registrar nuevos usuarios,
 * - y realizar operaciones básicas de administración (listar, crear, editar y eliminar).
 *
 * @author David Tomé Arnaiz
 */
@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * En este endpoint muestro el formulario de login y gestiono los mensajes de estado
     * asociados a errores, cierre de sesión o registro correcto.
     *
     * @param error indicador de error de autenticación
     * @param logout indicador de cierre de sesión correcto
     * @param registro_ok indicador de registro exitoso
     * @param model modelo de Spring MVC
     * @return vista del formulario de login
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

    /**
     * En este endpoint muestro el formulario de registro de usuarios.
     *
     * Inicializo una instancia vacía de {@link Usuario} para enlazarla con el formulario.
     *
     * @param model modelo de Spring MVC
     * @return vista del formulario de registro
     */
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    /**
     * En este endpoint proceso el registro de un nuevo usuario.
     *
     * Valido que las contraseñas coincidan y que el correo no exista previamente.
     * La encriptación de la contraseña se delega a la capa de servicio.
     *
     * @param usuario datos del usuario a registrar
     * @param passwordRepetida confirmación de la contraseña
     * @param model modelo de Spring MVC
     * @return redirección al login o vuelta al formulario en caso de error
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

        usuarioService.guardarUsuario(usuario);
        return "redirect:/login?registro_ok";
    }

    /**
     * En este endpoint muestro el listado completo de usuarios del sistema.
     *
     * Este método se utiliza desde la parte de administración.
     *
     * @param model modelo de Spring MVC
     * @return vista con el listado de usuarios
     */
    @GetMapping("/usuarios")
    public String verUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    /**
     * En este endpoint creo un nuevo usuario desde el panel de administración.
     *
     * Verifico previamente que el correo no esté duplicado.
     *
     * @param usuario datos del nuevo usuario
     * @param model modelo de Spring MVC
     * @return redirección al listado de usuarios
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
     * En este endpoint muestro el formulario de edición de un usuario existente.
     *
     * @param id identificador del usuario
     * @param model modelo de Spring MVC
     * @return vista del formulario de edición
     */
    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        return "editar";
    }

    /**
     * En este endpoint actualizo los datos de un usuario.
     *
     * Si se introduce una nueva contraseña, la guardo encriptada; en caso contrario,
     * conservo la contraseña previamente almacenada.
     *
     * @param usuario datos del usuario a actualizar
     * @param password nueva contraseña (opcional)
     * @return redirección al listado de usuarios
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
     * En este endpoint elimino un usuario a partir de su identificador.
     *
     * @param id identificador del usuario a eliminar
     * @return redirección al listado de usuarios
     */
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}