package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.jpa.UsuarioRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;            // null si mongo
    private final UsuarioMongoRepository usuarioMongoRepository;  // null si sql
    private final PasswordEncoder passwordEncoder;
    private final String dbEngine;
    private final Environment env;

    public UsuarioService(
            ObjectProvider<UsuarioRepository> usuarioRepository,
            ObjectProvider<UsuarioMongoRepository> usuarioMongoRepository,
            PasswordEncoder passwordEncoder,
            @Value("${DB_ENGINE:mysql}") String dbEngine,
            Environment env
    ) {
        this.usuarioRepository = usuarioRepository.getIfAvailable();
        this.usuarioMongoRepository = usuarioMongoRepository.getIfAvailable();
        this.passwordEncoder = passwordEncoder;
        this.dbEngine = (dbEngine == null ? "mysql" : dbEngine.toLowerCase());
        this.env = env;
    }

    private boolean isMongo() {
        // Si el perfil mongo está activo, manda siempre
        if (env != null && env.acceptsProfiles(Profiles.of("mongo"))) return true;
        // Fallback por variable
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (perfil mongo mal configurado)");
            }

            UsuarioMongo u = usuarioMongoRepository.findByCorreo(correo)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en Mongo: " + correo));

            return User.withUsername(u.getCorreo())
                    .password(u.getPassword())
                    .roles("USER")
                    .build();
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil sql mal configurado)");
        }

        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en SQL: " + correo));

        return User.withUsername(u.getCorreo())
                .password(u.getPassword())
                .roles("USER")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean existePorCorreo(String correo) {
        if (isMongo()) {
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");
            return usuarioMongoRepository.existsByCorreo(correo);
        }
        if (usuarioRepository == null) throw new IllegalStateException("UsuarioRepository no disponible");
        return usuarioRepository.existsByCorreo(correo);
    }

    @Transactional
    public void registrarUsuario(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        if (isMongo()) {
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");
            if (usuarioMongoRepository.existsByCorreo(usuario.getCorreo())) return;

            UsuarioMongo um = new UsuarioMongo();
            um.setNombre(usuario.getNombre());
            um.setCorreo(usuario.getCorreo());
            um.setPassword(usuario.getPassword());
            usuarioMongoRepository.save(um);
            return;
        }

        if (usuarioRepository == null) throw new IllegalStateException("UsuarioRepository no disponible");
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) return;

        usuarioRepository.save(usuario);
    }
}