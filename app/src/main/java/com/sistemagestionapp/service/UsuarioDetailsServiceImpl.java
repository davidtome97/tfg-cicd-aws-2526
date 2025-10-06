package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Usuario;
import com.sistemagestionapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Esta clase la utilizo para integrar mi sistema de usuarios con Spring Security.
 * Implemento la interfaz {@link UserDetailsService} para que Spring pueda obtener
 * la información de un usuario cuando se intenta iniciar sesión.
 *
 * Busco al usuario en la base de datos utilizando su correo electrónico y devuelvo
 * un objeto {@link UserDetails} con sus credenciales y rol.
 *
 * @author David Tomé Arnáiz
 */
@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    /**
     * Inyecto el repositorio de usuarios para poder buscar por correo electrónico.
     */
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Este método lo invoca Spring Security automáticamente cuando alguien intenta iniciar sesión.
     * Busco al usuario por su correo. Si no lo encuentro, lanzo una excepción. Si lo encuentro,
     * construyo un {@link User} con su correo, contraseña y un rol fijo de "USER".
     *
     * @param correo el correo electrónico que se usa como nombre de usuario.
     * @return los detalles del usuario necesarios para el proceso de autenticación.
     * @throws UsernameNotFoundException si el usuario no se encuentra en la base de datos.
     */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    //crea un objeto USER que representa al usuario logueado.
        return new User(usuario.getCorreo(), usuario.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("USER")));
    }
}