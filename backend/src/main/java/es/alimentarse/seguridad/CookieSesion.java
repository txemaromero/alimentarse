package es.alimentarse.seguridad;

import es.alimentarse.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie con el JWT de la sesión (D-070). {@code HttpOnly}: el JavaScript de la página no puede leerla, así que un XSS
 * no se lleva la sesión. {@code SameSite=Strict} y la protección CSRF evitan que otra web la use.
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "toca_sesion";

    /** El registro con Google a medio hacer (D-085): solo lo leen las rutas que lo completan. */
    public static final String NOMBRE_REGISTRO_GOOGLE = "toca_registro_google";

    /** La vinculación con Google a medio hacer (D-086): tampoco la leen otras rutas. */
    public static final String NOMBRE_VINCULAR_GOOGLE = "toca_vincular_google";

    private static final String RUTA_REGISTRO_GOOGLE = "/api/auth/google";

    /** Solo se envía a la API: ni al HTML ni a los ficheros estáticos. */
    private static final String RUTA = "/api";

    private final boolean segura;
    private final Duration validez;

    public CookieSesion(AppProperties propiedades) {
        this.segura = propiedades.jwt().cookieSegura();
        this.validez = Duration.ofHours(propiedades.jwt().expiracionHoras());
    }

    /** Valor de la cabecera {@code Set-Cookie} que inicia la sesión. */
    public String crear(String token) {
        return base(token).maxAge(validez).build().toString();
    }

    /** Valor de la cabecera {@code Set-Cookie} que la borra. */
    public String borrar() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    /** Cabecera {@code Set-Cookie} con el registro con Google pendiente; dura lo que su token. */
    public String crearRegistroGoogle(String token) {
        return pendienteGoogle(NOMBRE_REGISTRO_GOOGLE, token)
                .maxAge(JwtService.VALIDEZ_REGISTRO_GOOGLE)
                .build()
                .toString();
    }

    public String borrarRegistroGoogle() {
        return pendienteGoogle(NOMBRE_REGISTRO_GOOGLE, "")
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    /** Cabecera {@code Set-Cookie} con la vinculación con Google pendiente (D-086); dura lo que su token. */
    public String crearVinculacionGoogle(String token) {
        return pendienteGoogle(NOMBRE_VINCULAR_GOOGLE, token)
                .maxAge(JwtService.VALIDEZ_REGISTRO_GOOGLE)
                .build()
                .toString();
    }

    public String borrarVinculacionGoogle() {
        return pendienteGoogle(NOMBRE_VINCULAR_GOOGLE, "")
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    private ResponseCookie.ResponseCookieBuilder pendienteGoogle(String nombre, String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path(RUTA_REGISTRO_GOOGLE);
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path(RUTA);
    }
}
