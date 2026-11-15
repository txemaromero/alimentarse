package es.alimentarse.auth;

import es.alimentarse.seguridad.CookieSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * La vuelta de Google (D-085). La sesión HTTP solo sirve para el intercambio con Google (el {@code state} y el PKCE):
 * se invalida al terminar, y la de Toca es la cookie con el JWT de siempre (D-070). Después, el navegador va a una
 * pantalla de Angular:
 *
 * <ul>
 *   <li>{@code /login/google}, que pide los datos de la sesión y entra;
 *   <li>{@code /login/google/vincular}, para vincular su cuenta de Toca escribiendo su contraseña (D-086);
 *   <li>{@code /registro/google}, para terminar el registro;
 *   <li>{@code /login?google=motivo}, que explica por qué no ha podido entrar.
 * </ul>
 */
@Component
public class ManejadorAccesoGoogle implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    static final String ENTRAR = "/login/google";
    static final String REGISTRO = "/registro/google";
    static final String VINCULAR = "/login/google/vincular";
    static final String RECHAZO = "/login?google=";

    private final AccesoGoogleService acceso;
    private final CookieSesion cookies;

    public ManejadorAccesoGoogle(AccesoGoogleService acceso, CookieSesion cookies) {
        this.acceso = acceso;
        this.cookies = cookies;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest peticion, HttpServletResponse respuesta, Authentication autenticacion)
            throws IOException {
        terminarIntercambio(peticion);
        if (!(autenticacion.getPrincipal() instanceof OidcUser usuario)) {
            respuesta.sendRedirect(RECHAZO + "error");
            return;
        }
        CuentaGoogle cuenta = new CuentaGoogle(
                usuario.getSubject(),
                Objects.requireNonNullElse(usuario.getEmail(), ""),
                Boolean.TRUE.equals(usuario.getEmailVerified()),
                Objects.requireNonNullElse(usuario.getGivenName(), ""),
                Objects.requireNonNullElse(usuario.getFamilyName(), ""));
        switch (acceso.acceder(cuenta)) {
            case AccesoGoogleService.Entra entra -> {
                respuesta.addHeader(
                        HttpHeaders.SET_COOKIE, cookies.crear(entra.sesion().token()));
                respuesta.sendRedirect(ENTRAR);
            }
            case AccesoGoogleService.Vincular vincular -> {
                respuesta.addHeader(HttpHeaders.SET_COOKIE, cookies.crearVinculacionGoogle(vincular.token()));
                respuesta.sendRedirect(VINCULAR);
            }
            case AccesoGoogleService.Registro registro -> {
                respuesta.addHeader(HttpHeaders.SET_COOKIE, cookies.crearRegistroGoogle(registro.token()));
                respuesta.sendRedirect(REGISTRO);
            }
            case AccesoGoogleService.Rechazado rechazado -> respuesta.sendRedirect(RECHAZO + rechazado.motivo());
        }
    }

    /** Google ha dado un error, o la persona ha cancelado ({@code access_denied}). */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException error)
            throws IOException {
        terminarIntercambio(peticion);
        boolean cancelado = error instanceof OAuth2AuthenticationException oauth
                && "access_denied".equals(oauth.getError().getErrorCode());
        respuesta.sendRedirect(RECHAZO + (cancelado ? "cancelado" : "error"));
    }

    private static void terminarIntercambio(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }
}
