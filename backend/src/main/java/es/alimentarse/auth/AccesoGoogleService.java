package es.alimentarse.auth;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.seguridad.JwtService;
import es.alimentarse.usuario.AvisoCuenta;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qué pasa cuando alguien vuelve de entrar con Google (D-085, D-086):
 *
 * <ul>
 *   <li>si esa cuenta de Google ya está vinculada a un usuario, entra con él;
 *   <li>si no, y Google ha verificado su email, y ese email es de un usuario que lo ha confirmado (D-088), se vinculan
 *       y entra;
 *   <li>si ese usuario no lo ha confirmado, puede vincularlos escribiendo una vez su contraseña ({@link #vincular}):
 *       sin ella, quien hubiera creado una cuenta con el email de otro se quedaría con su acceso con Google (D-086);
 *   <li>si no, empieza un registro: elige su nombre de usuario y completa sus datos ({@link #completarRegistro}).
 * </ul>
 *
 * <p>Las cuentas bloqueadas (D-063) no entran, tampoco con Google.
 */
@Service
public class AccesoGoogleService {

    /** Motivos de rechazo, para el aviso del login ({@code /login?google=…}). */
    public static final String BLOQUEADA = "bloqueada";

    public static final String EMAIL_NO_VERIFICADO = "email-no-verificado";
    public static final String OTRA_CUENTA_DE_GOOGLE = "otra-cuenta";

    static final String REGISTRO_CADUCADO = "El acceso con Google ha caducado. Vuelve a entrar con Google.";
    static final String PASSWORD_INCORRECTA = "La contraseña no es correcta";
    static final String YA_VINCULADA = "Tu cuenta ya está vinculada con otra cuenta de Google.";

    private final UsuarioMapper usuarios;
    private final AuthService auth;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventos;

    public AccesoGoogleService(
            UsuarioMapper usuarios,
            AuthService auth,
            JwtService jwt,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.auth = auth;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
    }

    /** El resultado de volver de Google. */
    public sealed interface Resultado permits Entra, Vincular, Registro, Rechazado {}

    /** Entra con su usuario. */
    public record Entra(SesionIniciada sesion) implements Resultado {}

    /** Ya tiene cuenta con ese email: para vincularla tiene que escribir su contraseña. El token va en una cookie. */
    public record Vincular(String token) implements Resultado {}

    /** Tiene que terminar el registro: el token va en una cookie hasta que lo haga. */
    public record Registro(String token) implements Resultado {}

    /** No puede entrar, por uno de los motivos de esta clase. */
    public record Rechazado(String motivo) implements Resultado {}

    @Transactional
    public Resultado acceder(CuentaGoogle cuenta) {
        Optional<Usuario> vinculado = usuarios.buscarPorGoogleId(cuenta.id());
        if (vinculado.isPresent()) {
            return entrar(vinculado.get());
        }
        // Sin email verificado no se vincula ni se registra: el email sería de otra persona
        if (!cuenta.emailVerificado()
                || cuenta.email() == null
                || cuenta.email().isBlank()) {
            return new Rechazado(EMAIL_NO_VERIFICADO);
        }
        String email = cuenta.email().strip();
        // La colación de la base de datos no distingue acentos: aquí cuenta el email exacto (salvo mayúsculas)
        Optional<Usuario> mismoEmail =
                usuarios.buscarPorEmail(email).filter(usuario -> usuario.email().equalsIgnoreCase(email));
        if (mismoEmail.isPresent()) {
            Usuario usuario = mismoEmail.get();
            if (usuario.bloqueado()) {
                return new Rechazado(BLOQUEADA);
            }
            if (usuario.googleId() != null) {
                // Ya está vinculado a otra cuenta de Google con el mismo email (Google lo reasignó): no se cambia
                return new Rechazado(OTRA_CUENTA_DE_GOOGLE);
            }
            // Con el email confirmado en Toca y verificado por Google, es de la misma persona (D-088)
            if (usuario.emailVerificado()) {
                if (usuarios.vincularGoogle(usuario.id(), cuenta.id()) != 1) {
                    return new Rechazado(OTRA_CUENTA_DE_GOOGLE);
                }
                eventos.publishEvent(
                        new AvisoCuenta(usuario.nombre(), usuario.email(), AvisoCuenta.Motivo.GOOGLE_VINCULADO, null));
                return new Entra(auth.sesion(usuario));
            }
            return new Vincular(
                    jwt.emitirVinculacionGoogle(new JwtService.VinculacionGoogle(cuenta.id(), usuario.id(), email)));
        }
        return new Registro(jwt.emitirRegistroGoogle(new JwtService.RegistroGoogle(
                cuenta.id(), email, cuenta.nombre().strip(), cuenta.apellidos().strip())));
    }

    /** El email de la cuenta que se va a vincular; 404 si no hay una vinculación pendiente o ha caducado. */
    @Transactional(readOnly = true)
    public DatosVinculacionGoogleDto vinculacionPendiente(@Nullable String token) {
        return new DatosVinculacionGoogleDto(vinculacion(token).email());
    }

    /**
     * Vincula la cuenta de Google con el usuario de su email si la contraseña es la suya, y entra (D-086). Avisa por
     * correo a su dueño.
     */
    @Transactional
    public SesionIniciada vincular(@Nullable String token, String password) {
        JwtService.VinculacionGoogle vinculacion = vinculacion(token);
        Usuario usuario = usuarios.buscarPorId(vinculacion.usuario())
                .orElseThrow(() -> ExcepcionApi.noEncontrado("VINCULACION_GOOGLE_CADUCADA", REGISTRO_CADUCADO));
        if (!passwordEncoder.matches(password, usuario.password())) {
            throw ExcepcionApi.peticionIncorrecta(
                    "PASSWORD_INCORRECTA", PASSWORD_INCORRECTA + ".", Map.of("password", PASSWORD_INCORRECTA));
        }
        // Después de comprobar la contraseña, como en el login (D-063)
        if (usuario.bloqueado()) {
            throw ExcepcionApi.prohibido("CUENTA_BLOQUEADA", AuthService.CUENTA_BLOQUEADA);
        }
        if (vinculacion.googleId().equals(usuario.googleId())) {
            return auth.sesion(usuario);
        }
        // Solo si sigue sin vincular: otra petición a la vez, o con otra cuenta de Google, no la sustituye
        if (usuario.googleId() != null || usuarios.vincularGoogle(usuario.id(), vinculacion.googleId()) != 1) {
            throw ExcepcionApi.conflicto("YA_VINCULADA", YA_VINCULADA, Map.of());
        }
        // Sabe la contraseña y Google ha verificado ese mismo email: queda confirmado (D-088)
        if (!usuario.emailVerificado() && usuario.email().equalsIgnoreCase(vinculacion.email())) {
            usuarios.marcarEmailVerificado(usuario.id());
        }
        eventos.publishEvent(
                new AvisoCuenta(usuario.nombre(), usuario.email(), AvisoCuenta.Motivo.GOOGLE_VINCULADO, null));
        return auth.sesion(usuario);
    }

    /** Lo que da Google, para rellenar el formulario; 404 si no hay un registro pendiente o ha caducado. */
    @Transactional(readOnly = true)
    public DatosRegistroGoogleDto pendiente(@Nullable String token) {
        JwtService.RegistroGoogle registro = registro(token);
        String[] apellidos = registro.apellidos().strip().split("\\s+", 2);
        return new DatosRegistroGoogleDto(
                registro.email(), registro.nombre(), apellidos[0], apellidos.length > 1 ? apellidos[1] : null);
    }

    /**
     * Crea la cuenta vinculada a Google, con el email de Google y una contraseña que nadie conoce, y entra con ella. Si
     * la cuenta de Google ya tiene usuario, el registro ya no vale (D-086): el token no sirve para entrar otra vez.
     */
    @Transactional
    public SesionIniciada completarRegistro(@Nullable String token, RegistroGoogleRequest datos) {
        JwtService.RegistroGoogle registro = registro(token);
        if (usuarios.buscarPorGoogleId(registro.googleId()).isPresent()) {
            throw ExcepcionApi.noEncontrado("REGISTRO_GOOGLE_CADUCADO", REGISTRO_CADUCADO);
        }
        Usuario usuario = auth.crearCuenta(
                datos.id(),
                datos.nombre(),
                datos.apellido1(),
                datos.apellido2(),
                registro.email(),
                auth.passwordAleatoria(),
                registro.googleId());
        return auth.sesion(usuario);
    }

    private Resultado entrar(Usuario usuario) {
        if (usuario.bloqueado()) {
            return new Rechazado(BLOQUEADA);
        }
        return new Entra(auth.sesion(usuario));
    }

    private JwtService.RegistroGoogle registro(@Nullable String token) {
        return Optional.ofNullable(token)
                .flatMap(jwt::validarRegistroGoogle)
                .orElseThrow(() -> ExcepcionApi.noEncontrado("REGISTRO_GOOGLE_CADUCADO", REGISTRO_CADUCADO));
    }

    private JwtService.VinculacionGoogle vinculacion(@Nullable String token) {
        return Optional.ofNullable(token)
                .flatMap(jwt::validarVinculacionGoogle)
                .orElseThrow(() -> ExcepcionApi.noEncontrado("VINCULACION_GOOGLE_CADUCADA", REGISTRO_CADUCADO));
    }
}
