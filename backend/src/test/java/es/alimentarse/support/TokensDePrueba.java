package es.alimentarse.support;

import es.alimentarse.seguridad.JwtService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import java.util.List;

/**
 * Cabeceras {@code Authorization} válidas para los tests, sin pasar por el login (ni por su límite de intentos).
 */
public class TokensDePrueba {

    private final JwtService jwt;

    public TokensDePrueba(JwtService jwt) {
        this.jwt = jwt;
    }

    /** Token de un usuario con rol USER. */
    public String usuario(String id) {
        return bearer(id, UsuarioAutenticado.ROL_USER);
    }

    /** Token de un usuario con rol USER y la versión de sesión dada: la de la base de datos, si se han anulado sesiones. */
    public String usuario(String id, int version) {
        return "Bearer "
                + jwt.emitir(new UsuarioAutenticado(id, id, List.of(UsuarioAutenticado.ROL_USER)), version)
                        .valor();
    }

    /** Token de un usuario con roles USER y ADMIN. */
    public String admin(String id) {
        return bearer(id, UsuarioAutenticado.ROL_USER, UsuarioAutenticado.ROL_ADMIN);
    }

    public String bearer(String id, String... roles) {
        return "Bearer "
                + jwt.emitir(new UsuarioAutenticado(id, id, List.of(roles)), 0).valor();
    }
}
