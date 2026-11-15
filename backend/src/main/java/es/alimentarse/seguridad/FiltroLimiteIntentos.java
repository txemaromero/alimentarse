package es.alimentarse.seguridad;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita por IP las peticiones que comprueban una contraseña, envían correos, crean cuentas o empiezan el acceso con
 * Google (D-086). Cuenta todas las peticiones, también las mal formadas, antes de validar nada. Las rutas se comparan
 * ya decodificadas, como las compara Spring MVC: {@code /api/auth/%6Cogin} también es el login. Detrás de un proxy, la
 * IP es la que da Tomcat con {@code server.forward-headers-strategy: native}.
 */
public class FiltroLimiteIntentos extends OncePerRequestFilter {

    /** Unas rutas y el límite que comparten, con su propio contador por IP. */
    record Regla(
            String contador,
            RequestMatcher peticiones,
            Function<AppProperties.LimiteIntentos, AppProperties.Limite> limite) {}

    private static final PathPatternRequestMatcher.Builder RUTAS = PathPatternRequestMatcher.withDefaults();

    static final List<Regla> REGLAS = List.of(
            new Regla("login", RUTAS.matcher(HttpMethod.POST, "/api/auth/login"), AppProperties.LimiteIntentos::login),
            // Comprueban la contraseña de la cuenta: mismo límite que el login, con su propio contador
            new Regla(
                    "password",
                    new OrRequestMatcher(
                            RUTAS.matcher(HttpMethod.POST, "/api/auth/google/vincular"),
                            RUTAS.matcher(HttpMethod.PUT, "/api/usuarios/me"),
                            RUTAS.matcher(HttpMethod.PUT, "/api/usuarios/me/password"),
                            RUTAS.matcher(HttpMethod.POST, "/api/usuarios/me/desvincular-google"),
                            RUTAS.matcher(HttpMethod.POST, "/api/usuarios/me/baja")),
                    AppProperties.LimiteIntentos::login),
            new Regla(
                    "olvido",
                    RUTAS.matcher(HttpMethod.POST, "/api/auth/password/olvido"),
                    AppProperties.LimiteIntentos::olvidoPassword),
            // Envía un correo, como el olvido (D-088)
            new Regla(
                    "confirmacion",
                    RUTAS.matcher(HttpMethod.POST, "/api/usuarios/me/email/confirmacion"),
                    AppProperties.LimiteIntentos::olvidoPassword),
            // Crean cuentas y envían el correo de bienvenida
            new Regla(
                    "registro",
                    new OrRequestMatcher(
                            RUTAS.matcher(HttpMethod.POST, "/api/auth/registro"),
                            RUTAS.matcher(HttpMethod.POST, "/api/auth/google/registro")),
                    AppProperties.LimiteIntentos::registro),
            // Cada inicio del acceso con Google guarda una sesión HTTP en el servidor
            new Regla(
                    "google",
                    RUTAS.matcher(HttpMethod.GET, "/oauth2/authorization/**"),
                    AppProperties.LimiteIntentos::accesoGoogle));

    private final LimiteIntentosService limite;
    private final AppProperties.LimiteIntentos limites;
    private final RespuestasErrorSeguridad respuestas;

    public FiltroLimiteIntentos(
            LimiteIntentosService limite, AppProperties propiedades, RespuestasErrorSeguridad respuestas) {
        this.limite = limite;
        this.limites = propiedades.limiteIntentos();
        this.respuestas = respuestas;
    }

    static Optional<Regla> regla(HttpServletRequest peticion) {
        return REGLAS.stream().filter(r -> r.peticiones().matches(peticion)).findFirst();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return regla(peticion).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        Regla regla = regla(peticion).orElseThrow();
        try {
            limite.registrarIntento(
                    regla.contador() + ":" + peticion.getRemoteAddr(),
                    regla.limite().apply(limites));
        } catch (ExcepcionApi e) {
            respuestas.escribir(respuesta, e);
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }
}
