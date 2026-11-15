package es.alimentarse.seguridad;

import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga el usuario para el login, que admite nombre de usuario o email. Los ids no pueden contener «@» (6.9), así que
 * un «@» indica que es un email. Guarda también el hash rehecho de una contraseña con un coste antiguo (D-090).
 */
@Service
public class UsuarioDetallesService implements UserDetailsService, UserDetailsPasswordService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDetallesService.class);

    private final UsuarioMapper usuarios;

    public UsuarioDetallesService(UsuarioMapper usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String idOEmail) {
        Optional<Usuario> encontrado =
                idOEmail.contains("@") ? usuarios.buscarPorEmail(idOEmail) : usuarios.buscarPorId(idOEmail);
        Usuario usuario = encontrado.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(usuario.id())
                .password(usuario.password())
                .roles(usuarios.rolesDe(usuario.id()).toArray(String[]::new))
                .build();
    }

    /**
     * Tras un login correcto, si el hash tiene menos coste que el configurado ({@link CifradoPassword#coste}), Spring
     * cifra otra vez la contraseña y la guarda aquí. Es la misma contraseña: no sube la versión de sesión (D-070), así que no cierra las
     * demás sesiones, ni anula un enlace de recuperación. Solo se guarda si el hash no ha cambiado mientras tanto, para no
     * deshacer un cambio de contraseña simultáneo. Si no se puede guardar, el login sigue: se intentará en el siguiente.
     */
    @Override
    public UserDetails updatePassword(UserDetails usuario, String hashNuevo) {
        try {
            if (usuarios.rehacerHashPassword(usuario.getUsername(), usuario.getPassword(), hashNuevo) == 1) {
                return User.withUserDetails(usuario).password(hashNuevo).build();
            }
        } catch (DataAccessException e) {
            log.warn(
                    "No se ha podido guardar el hash rehecho de una contraseña: {}",
                    e.getClass().getSimpleName());
            log.debug("Detalle del error al rehacer el hash", e);
        }
        return usuario;
    }
}
