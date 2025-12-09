package com.sistemagestionapp.demojava.security;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
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

        Object usuarioObj = usuarioService.buscarPorCorreo(correo);

        if (usuarioObj == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + correo);
        }

        String username;
        String password;

        // Java 17: pattern matching con instanceof
        if (usuarioObj instanceof Usuario usuarioSql) {
            username = usuarioSql.getCorreo();
            password = usuarioSql.getPassword();
        } else if (usuarioObj instanceof UsuarioMongo usuarioMongo) {
            username = usuarioMongo.getCorreo();
            password = usuarioMongo.getPassword();
        } else {
            throw new IllegalStateException("Tipo de usuario desconocido: " + usuarioObj.getClass());
        }

        return User.withUsername(username)
                .password(password)
                .roles("USER")
                .build();
    }
}