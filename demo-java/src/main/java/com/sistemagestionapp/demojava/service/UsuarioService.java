package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.UsuarioRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMongoRepository usuarioMongoRepository;
    private final String dbEngine;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          UsuarioMongoRepository usuarioMongoRepository,
                          @Value("${app.db.engine:h2}") String dbEngine) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMongoRepository = usuarioMongoRepository;
        this.dbEngine = (dbEngine == null ? "h2" : dbEngine.toLowerCase());
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    // =========================================================
    // 1) USADO POR SPRING SECURITY (si se inyecta este servicio)
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (isMongo()) {
            // LOGIN CONTRA MONGO
            UsuarioMongo usuario = usuarioMongoRepository
                    .findByCorreo(username)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Usuario no encontrado en Mongo: " + username));

            return new User(
                    usuario.getCorreo(),
                    usuario.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        } else {
            // LOGIN CONTRA SQL
            Usuario usuario = usuarioRepository
                    .findByCorreo(username)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Usuario no encontrado en SQL: " + username));

            return new User(
                    usuario.getCorreo(),
                    usuario.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }
    }

    // =========================================================
    // 2) USADO POR UsuarioDetailsServiceImpl (si sigues usándolo)
    // =========================================================
    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {

        if (isMongo()) {
            return usuarioMongoRepository.findByCorreo(correo)
                    .map(um -> {
                        Usuario u = new Usuario();
                        // el id en Mongo es String, aquí no lo necesitamos para login
                        u.setNombre(um.getNombre());
                        u.setCorreo(um.getCorreo());
                        u.setPassword(um.getPassword());
                        return u;
                    })
                    .orElse(null);
        } else {
            return usuarioRepository.findByCorreo(correo).orElse(null);
        }
    }

    // =========================================================
    // 3) USADO POR AuthController (registro de usuarios)
    // =========================================================

    @Transactional(readOnly = true)
    public boolean existePorCorreo(String correo) {

        if (isMongo()) {
            return usuarioMongoRepository.existsByCorreo(correo);
        } else {
            return usuarioRepository.existsByCorreo(correo);
        }
    }

    @Transactional
    public void registrarUsuario(Usuario usuario) {

        String correo = usuario.getCorreo();

        if (isMongo()) {
            // ya existe -> no hacemos nada
            if (usuarioMongoRepository.existsByCorreo(correo)) {
                return;
            }

            UsuarioMongo um = new UsuarioMongo();
            um.setNombre(usuario.getNombre());
            um.setCorreo(usuario.getCorreo());
            um.setPassword(usuario.getPassword());

            usuarioMongoRepository.save(um);

        } else {
            // SQL
            if (usuarioRepository.existsByCorreo(correo)) {
                return;
            }

            usuarioRepository.save(usuario);
        }
    }
}