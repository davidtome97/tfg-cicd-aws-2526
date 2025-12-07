package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para gestionar la lógica de negocio de usuarios.
 * Aquí centralizo el guardado y la encriptación de contraseñas.
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public boolean existePorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).isPresent();
    }

    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    /**
     * Guarda un usuario.
     * Si la contraseña viene sin encriptar, la encripto.
     * Si ya viene con formato BCrypt, NO la vuelvo a encriptar.
     */
    public void guardarUsuario(Usuario usuario) {

        String pwd = usuario.getPassword();

        // Si la contraseña viene en texto plano → la encripto
        if (pwd != null && !pwd.isBlank()
                && !pwd.startsWith("$2a$")
                && !pwd.startsWith("$2b$")
                && !pwd.startsWith("$2y$")) {

            usuario.setPassword(passwordEncoder.encode(pwd));
        }

        usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }
}