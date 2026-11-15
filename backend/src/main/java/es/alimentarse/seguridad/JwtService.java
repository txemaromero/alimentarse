package es.alimentarse.seguridad;

import es.alimentarse.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Emite y valida los JWT de sesión: {@code sub} = id de usuario, con su nombre, sus roles y la versión de sesión. */
@Service
public class JwtService {

    static final int BYTES_MINIMOS_SECRETO = 32;
    private static final String CLAIM_NOMBRE = "nombre";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_VERSION = "ver";

    /** Destinatario de los tokens de sesión; los demás tokens llevan otro y nunca valen como sesión (D-086). */
    private static final String AUDIENCIA_SESION = "sesion";

    /** Destinatario del token de un registro con Google pendiente. */
    private static final String AUDIENCIA_REGISTRO_GOOGLE = "registro-google";

    /** Destinatario del token de una vinculación con Google pendiente (D-086). */
    private static final String AUDIENCIA_VINCULAR_GOOGLE = "vincular-google";

    private static final String CLAIM_USUARIO = "usuario";

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_APELLIDOS = "apellidos";

    /** Lo que dura un registro o una vinculación con Google a medio hacer (D-085, D-086). */
    public static final Duration VALIDEZ_REGISTRO_GOOGLE = Duration.ofMinutes(15);

    private final SecretKey clave;
    private final Duration validez;
    private final Clock reloj;

    /** Token emitido y cuándo caduca. */
    public record TokenEmitido(String valor, Instant caduca) {}

    /**
     * Contenido de un token válido.
     *
     * @param version versión de sesión de la cuenta al emitirlo (D-070)
     * @param caduca cuándo termina la sesión
     */
    public record SesionToken(UsuarioAutenticado usuario, int version, Instant caduca) {}

    /**
     * Lo que se sabe de quien empieza a registrarse con Google (D-085): su cuenta de Google y lo que Google dice de
     * ella.
     *
     * @param apellidos los que da Google, juntos, o vacío
     */
    public record RegistroGoogle(String googleId, String email, String nombre, String apellidos) {}

    /**
     * Quien ha vuelto de Google con el email de una cuenta de Toca sin vincular (D-086): para vincularlas tiene que
     * escribir la contraseña de esa cuenta.
     *
     * @param usuario el id de esa cuenta
     */
    public record VinculacionGoogle(String googleId, String usuario, String email) {}

    public JwtService(AppProperties propiedades, Clock reloj) {
        this.clave = claveDesde(propiedades.jwt().secreto());
        this.validez = Duration.ofHours(propiedades.jwt().expiracionHoras());
        this.reloj = reloj;
    }

    public TokenEmitido emitir(UsuarioAutenticado usuario, int version) {
        Instant ahora = reloj.instant();
        Instant caduca = ahora.plus(validez);
        String valor = Jwts.builder()
                .subject(usuario.id())
                .audience()
                .add(AUDIENCIA_SESION)
                .and()
                .claim(CLAIM_NOMBRE, usuario.nombre())
                .claim(CLAIM_ROLES, usuario.roles())
                .claim(CLAIM_VERSION, version)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(caduca))
                .signWith(clave)
                .compact();
        return new TokenEmitido(valor, caduca);
    }

    /**
     * Devuelve el contenido del token, o vacío si la firma no es válida, ha caducado, está mal formado o es anterior a
     * la versión de sesión (D-070).
     */
    public Optional<SesionToken> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireAudience(AUDIENCIA_SESION)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer version = claims.get(CLAIM_VERSION, Integer.class);
            if (version == null) {
                return Optional.empty();
            }
            List<?> roles = claims.get(CLAIM_ROLES, List.class);
            UsuarioAutenticado usuario = new UsuarioAutenticado(
                    claims.getSubject(),
                    claims.get(CLAIM_NOMBRE, String.class),
                    roles == null
                            ? List.of()
                            : roles.stream().map(String::valueOf).toList());
            return Optional.of(
                    new SesionToken(usuario, version, claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Token del registro con Google pendiente: firmado como las sesiones, pero con otro destinatario y sin «ver». */
    public String emitirRegistroGoogle(RegistroGoogle registro) {
        return emitirPendienteGoogle(
                AUDIENCIA_REGISTRO_GOOGLE,
                registro.googleId(),
                Map.of(
                        CLAIM_EMAIL,
                        registro.email(),
                        CLAIM_NOMBRE,
                        registro.nombre(),
                        CLAIM_APELLIDOS,
                        registro.apellidos()));
    }

    /** El registro con Google pendiente, o vacío si el token no vale, ha caducado o no es de un registro. */
    public Optional<RegistroGoogle> validarRegistroGoogle(String token) {
        return leerPendienteGoogle(token, AUDIENCIA_REGISTRO_GOOGLE)
                .filter(claims -> claims.get(CLAIM_EMAIL, String.class) != null)
                .map(claims -> new RegistroGoogle(
                        claims.getSubject(),
                        claims.get(CLAIM_EMAIL, String.class),
                        Objects.requireNonNullElse(claims.get(CLAIM_NOMBRE, String.class), ""),
                        Objects.requireNonNullElse(claims.get(CLAIM_APELLIDOS, String.class), "")));
    }

    /** Token de la vinculación con Google pendiente (D-086), con su propio destinatario. */
    public String emitirVinculacionGoogle(VinculacionGoogle vinculacion) {
        return emitirPendienteGoogle(
                AUDIENCIA_VINCULAR_GOOGLE,
                vinculacion.googleId(),
                Map.of(CLAIM_USUARIO, vinculacion.usuario(), CLAIM_EMAIL, vinculacion.email()));
    }

    /** La vinculación con Google pendiente, o vacío si el token no vale, ha caducado o no es de una vinculación. */
    public Optional<VinculacionGoogle> validarVinculacionGoogle(String token) {
        return leerPendienteGoogle(token, AUDIENCIA_VINCULAR_GOOGLE)
                .filter(claims -> claims.get(CLAIM_USUARIO, String.class) != null
                        && claims.get(CLAIM_EMAIL, String.class) != null)
                .map(claims -> new VinculacionGoogle(
                        claims.getSubject(),
                        claims.get(CLAIM_USUARIO, String.class),
                        claims.get(CLAIM_EMAIL, String.class)));
    }

    private String emitirPendienteGoogle(String audiencia, String googleId, Map<String, String> datos) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(googleId)
                .audience()
                .add(audiencia)
                .and()
                .claims()
                .add(datos)
                .and()
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(VALIDEZ_REGISTRO_GOOGLE)))
                .signWith(clave)
                .compact();
    }

    private Optional<Claims> leerPendienteGoogle(String token, String audiencia) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireAudience(audiencia)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject() == null ? Optional.empty() : Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static SecretKey claveDesde(String secretoBase64) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretoBase64.strip());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "JWT_SECRET debe estar en Base64. Genera uno con: openssl rand -base64 48", e);
        }
        if (bytes.length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits (32 bytes una vez decodificado)"
                    + " y tiene " + bytes.length + " bytes. Genera uno con: openssl rand -base64 48");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
