package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * En esta clase integro mi sistema de usuarios con Spring Security.
 *
 * Implemento {@link UserDetailsService} para que Spring Security pueda cargar los datos del usuario
 * durante el proceso de autenticación. Busco el usuario por correo electrónico y, si existe, construyo
 * un {@link UserDetails} con sus credenciales y una autoridad fija de tipo {@code USER}.
 *
 * @author David Tomé Arnaiz
 */
@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Cargo los detalles de un usuario a partir de su correo electrónico.
     *
     * Este método lo invoca Spring Security automáticamente cuando alguien intenta iniciar sesión.
     * Si no encuentro el usuario, lanzo {@link UsernameNotFoundException}. Si lo encuentro, devuelvo
     * un {@link UserDetails} con el correo, la contraseña cifrada y una autoridad {@code USER}.
     *
     * @param correo correo electrónico utilizado como identificador de usuario
     * @return detalles del usuario necesarios para autenticación y autorización
     * @throws UsernameNotFoundException si no existe un usuario con ese correo
     */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return new User(
                usuario.getCorreo(),
                usuario.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("USER"))
        );
    }
}