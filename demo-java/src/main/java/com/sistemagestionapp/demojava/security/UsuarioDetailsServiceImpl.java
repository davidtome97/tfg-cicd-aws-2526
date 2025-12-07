package com.sistemagestionapp.demojava.security;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.service.UsuarioService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioService usuarioService;

    public UsuarioDetailsServiceImpl(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioService.buscarPorCorreo(correo);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + correo);
        }

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .roles("USER")
                .build();
    }
}