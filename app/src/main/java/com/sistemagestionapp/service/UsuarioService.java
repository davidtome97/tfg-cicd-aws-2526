package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio encargado de la gestión de usuarios del sistema.
 *
 * Esta clase centraliza la lógica de negocio relacionada con la entidad {@link Usuario},
 * incluyendo operaciones de consulta, creación, actualización y eliminación.
 * Además, se responsabiliza de la encriptación segura de las contraseñas antes
 * de persistirlas en la base de datos.
 *
 * La encriptación se realiza mediante {@link PasswordEncoder}, garantizando que
 * las contraseñas nunca se almacenen en texto plano.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Obtiene la lista completa de usuarios registrados en el sistema.
     *
     * @return lista de usuarios
     */
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Obtiene un usuario a partir de su identificador.
     *
     * @param id identificador del usuario
     * @return el usuario encontrado o {@code null} si no existe
     */
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    /**
     * Comprueba si existe un usuario con el correo electrónico indicado.
     *
     * @param correo correo electrónico a comprobar
     * @return {@code true} si existe un usuario con ese correo, {@code false} en caso contrario
     */
    public boolean existePorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).isPresent();
    }

    /**
     * Obtiene un usuario a partir de su correo electrónico.
     *
     * @param correo correo electrónico del usuario
     * @return el usuario encontrado o {@code null} si no existe
     */
    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    /**
     * Guarda o actualiza un usuario en la base de datos.
     *
     * Si la contraseña del usuario viene en texto plano, se encripta antes
     * de persistirse. Si la contraseña ya está en formato BCrypt, se asume
     * que está correctamente cifrada y no se vuelve a encriptar.
     *
     * Este comportamiento permite reutilizar el método tanto en el registro
     * como en la edición de usuarios sin provocar dobles encriptaciones.
     *
     * @param usuario usuario a guardar
     */
    public void guardarUsuario(Usuario usuario) {

        String password = usuario.getPassword();

        if (password != null
                && !password.isBlank()
                && !password.startsWith("$2a$")
                && !password.startsWith("$2b$")
                && !password.startsWith("$2y$")) {

            usuario.setPassword(passwordEncoder.encode(password));
        }

        usuarioRepository.save(usuario);
    }

    /**
     * Elimina un usuario del sistema a partir de su identificador.
     *
     * @param id identificador del usuario a eliminar
     */
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }
}