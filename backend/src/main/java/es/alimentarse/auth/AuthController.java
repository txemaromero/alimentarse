package es.alimentarse.auth;

import es.alimentarse.config.GoogleProperties;
import es.alimentarse.seguridad.CookieSesion;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.usuario.ConfirmacionEmailService;
import es.alimentarse.usuario.EmailConfirmadoDto;
import es.alimentarse.usuario.PerfilDto;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación. El login y el olvido de contraseña tienen límite de intentos por IP (filtro de
 * seguridad).
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    static final String MENSAJE_OLVIDO =
            "Si el email está registrado, recibirás un correo con un enlace para restablecer la contraseña.";
    static final String MENSAJE_RESTABLECIDA = "Contraseña cambiada. Ya puedes iniciar sesión.";

    private final AuthService auth;
    private final CookieSesion cookies;
    private final AccesoGoogleService acceso;
    private final GoogleProperties google;
    private final ConfirmacionEmailService confirmaciones;

    AuthController(
            AuthService auth,
            CookieSesion cookies,
            AccesoGoogleService acceso,
            GoogleProperties google,
            ConfirmacionEmailService confirmaciones) {
        this.auth = auth;
        this.cookies = cookies;
        this.acceso = acceso;
        this.google = google;
        this.confirmaciones = confirmaciones;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    PerfilDto registro(@Valid @RequestBody RegistroRequest datos) {
        return auth.registrar(datos);
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest datos) {
        SesionIniciada sesion = auth.login(datos);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.crear(sesion.token()))
                .body(sesion.respuesta());
    }

    /** Las formas de entrar que hay, para mostrar el botón de Google (D-085). */
    @GetMapping("/proveedores")
    ProveedoresDto proveedores() {
        return new ProveedoresDto(google.habilitado());
    }

    /**
     * Los datos de la sesión de la cookie, para quien vuelve de entrar con Google (D-085): el login normal ya los da en
     * su respuesta. 401 si no hay sesión.
     */
    @GetMapping("/sesion")
    LoginResponse sesion(
            @CookieValue(name = CookieSesion.NOMBRE, required = false) @Nullable String token,
            @AuthenticationPrincipal @Nullable UsuarioAutenticado yo) {
        return auth.datosSesion(token, yo);
    }

    /** El email de la cuenta que se va a vincular con Google (D-086); 404 si no hay vinculación pendiente. */
    @GetMapping("/google/vincular")
    DatosVinculacionGoogleDto vinculacionGooglePendiente(
            @CookieValue(name = CookieSesion.NOMBRE_VINCULAR_GOOGLE, required = false) @Nullable String token) {
        return acceso.vinculacionPendiente(token);
    }

    /** Vincula la cuenta de Google con la de Toca de su email, con la contraseña de esta, y entra (D-086). */
    @PostMapping("/google/vincular")
    ResponseEntity<LoginResponse> vincularGoogle(
            @CookieValue(name = CookieSesion.NOMBRE_VINCULAR_GOOGLE, required = false) @Nullable String token,
            @Valid @RequestBody VincularGoogleRequest datos) {
        SesionIniciada sesion = acceso.vincular(token, datos.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.crear(sesion.token()))
                .header(HttpHeaders.SET_COOKIE, cookies.borrarVinculacionGoogle())
                .body(sesion.respuesta());
    }

    /** Lo que da Google de quien termina de registrarse (D-085); 404 si no hay registro pendiente o ha caducado. */
    @GetMapping("/google/registro")
    DatosRegistroGoogleDto registroGooglePendiente(
            @CookieValue(name = CookieSesion.NOMBRE_REGISTRO_GOOGLE, required = false) @Nullable String token) {
        return acceso.pendiente(token);
    }

    /** Termina el registro con Google: crea la cuenta y entra con ella (D-085). */
    @PostMapping("/google/registro")
    ResponseEntity<LoginResponse> completarRegistroGoogle(
            @CookieValue(name = CookieSesion.NOMBRE_REGISTRO_GOOGLE, required = false) @Nullable String token,
            @Valid @RequestBody RegistroGoogleRequest datos) {
        SesionIniciada sesion = acceso.completarRegistro(token, datos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookies.crear(sesion.token()))
                .header(HttpHeaders.SET_COOKIE, cookies.borrarRegistroGoogle())
                .body(sesion.respuesta());
    }

    /** Borra la cookie de sesión de este navegador: el JavaScript no puede hacerlo (D-070). */
    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.borrar())
                .build();
    }

    /** Responde siempre lo mismo, exista o no el email (D-021). */
    @PostMapping("/password/olvido")
    MensajeRespuesta olvidoPassword(@Valid @RequestBody OlvidoPasswordRequest datos) {
        auth.solicitarRecuperacion(datos.email());
        return new MensajeRespuesta(MENSAJE_OLVIDO);
    }

    /** Confirma el email con el enlace del correo (D-088). Sin sesión: puede abrirse en otro dispositivo. */
    /**
     * Confirmar un email nuevo cierra las sesiones abiertas de la cuenta (D-089). Si el enlace se abre en un navegador
     * con la sesión de esa misma cuenta, esa sesión sigue: recibe un token nuevo.
     */
    @PostMapping("/email/confirmar")
    ResponseEntity<EmailConfirmadoDto> confirmarEmail(
            @AuthenticationPrincipal @Nullable UsuarioAutenticado yo, @Valid @RequestBody ConfirmarEmailRequest datos) {
        EmailConfirmadoDto confirmado = confirmaciones.confirmar(datos.token());
        if (yo == null || !yo.id().equals(confirmado.usuario())) {
            return ResponseEntity.ok(confirmado);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.crear(auth.tokenRenovado(yo)))
                .body(confirmado);
    }

    @PostMapping("/password/restablecer")
    MensajeRespuesta restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest datos) {
        auth.restablecer(datos);
        return new MensajeRespuesta(MENSAJE_RESTABLECIDA);
    }
}
