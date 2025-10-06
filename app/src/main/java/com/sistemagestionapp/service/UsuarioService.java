package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Esta clase la utilizo como servicio para gestionar la lógica relacionada con los usuarios.
 * Me encargo de listar, guardar (con contraseña cifrada), buscar y eliminar usuarios.
 * Trabajo con {@link UsuarioRepository} para acceder a los datos y utilizo
 * {@link BCryptPasswordEncoder} para garantizar la seguridad de las contraseñas.
 *
 * @author David Tomé Arnáiz
 */
@Service
public class UsuarioService {

    // Inyecto el repositorio de usuarios
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Utilizo BCrypt para cifrar las contraseñas antes de guardarlas
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Devuelvo todos los usuarios almacenados en la base de datos.
     *
     * @return lista de usuarios.
     */
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll(); // obtengo todos los usuarios
    }

    /**
     * Busco un usuario por su ID. Si no lo encuentro, devuelvo null.
     *
     * @param id identificador del usuario.
     * @return el usuario encontrado o null si no existe.
     */
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null); // si no existe, devuelvo null
    }

    /**
     * Guardo un nuevo usuario o actualizo uno existente.
     * Antes de guardar, cifro la contraseña con BCrypt.
     *
     * @param usuario el usuario que quiero guardar.
     */
    public void guardarUsuario(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // cifro la contraseña
        usuarioRepository.save(usuario); // guardo el usuario en la base de datos
    }

    /**
     * Elimino un usuario de la base de datos usando su ID.
     *
     * @param id identificador del usuario a eliminar.
     */
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id); // elimino el usuario
    }

    /**
     * Compruebo si ya existe un usuario con el correo indicado.
     *
     * @param correo correo que quiero comprobar.
     * @return true si el usuario existe, false en caso contrario.
     */
    public boolean existePorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).isPresent(); // uso Optional para comprobar existencia
    }

    /**
     * Devuelvo un usuario buscando por su correo. Si no existe, devuelvo null.
     *
     * @param correo correo electrónico del usuario.
     * @return el usuario encontrado o null si no se encuentra.
     */
    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElse(null); // devuelvo el usuario o null
    }
}