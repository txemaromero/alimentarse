package es.alimentarse.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

/**
 * Autentica la petición si trae un JWT válido, en la cookie de sesión o en {@code Authorization: Bearer}, y la cuenta
 * sigue activa y con esa versión de sesión. Si no, no hace nada: las rutas protegidas responderán 401. Si la cuenta
 * está bloqueada, lo deja anotado en la petición para que el 401 lo diga (D-063, D-070).
 */
class FiltroJwt extends OncePerRequestFilter {

    /** Atributo de la petición con el {@link EstadoCuentas.Estado} de una cuenta que no puede usar su sesión. */
    static final String CUENTA_RECHAZADA = FiltroJwt.class.getName() + ".CUENTA_RECHAZADA";

    static final String PREFIJO = "Bearer ";

    private final JwtService jwt;
    private final EstadoCuentas cuentas;

    FiltroJwt(JwtService jwt, EstadoCuentas cuentas) {
        this.jwt = jwt;
        this.cuentas = cuentas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String token = tokenDe(peticion);
        if (token != null) {
            jwt.validar(token).ifPresent(sesion -> {
                UsuarioAutenticado usuario = sesion.usuario();
                EstadoCuentas.Estado estado = cuentas.de(usuario.id(), sesion.version());
                if (estado == EstadoCuentas.Estado.ACTIVA) {
                    SecurityContextHolder.getContext()
                            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                    usuario, null, usuario.autoridades()));
                } else {
                    peticion.setAttribute(CUENTA_RECHAZADA, estado);
                }
            });
        }
        cadena.doFilter(peticion, respuesta);
    }

    private static String tokenDe(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            return cabecera.substring(PREFIJO.length()).strip();
        }
        Cookie cookie = WebUtils.getCookie(peticion, CookieSesion.NOMBRE);
        return cookie == null ? null : cookie.getValue();
    }
}
