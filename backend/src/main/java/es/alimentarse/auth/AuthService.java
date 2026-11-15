package es.alimentarse.auth;

import es.alimentarse.comun.Texto;
import es.alimentarse.comun.TokenSeguro;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import es.alimentarse.seguridad.CifradoPassword;
import es.alimentarse.seguridad.JwtService;
import es.alimentarse.seguridad.LimiteIntentosService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.usuario.ConfirmacionEmailService;
import es.alimentarse.usuario.PerfilDto;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro, inicio de sesión y recuperación de contraseña (6.9). */
@Service
public class AuthService {

    static final Duration VALIDEZ_TOKEN_RECUPERACION = Duration.ofHours(1);
    static final String ID_EN_USO = "Ese nombre de usuario ya está en uso";
    static final String EMAIL_EN_USO = "Ya hay una cuenta con ese email";
    static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";
    static final String TOKEN_INVALIDO = "El enlace no es válido o ha caducado. Solicita uno nuevo.";
    static final String CUENTA_BLOQUEADA = "Tu cuenta está bloqueada por la administración.";
    static final String SIN_SESION = "No hay ninguna sesión iniciada.";

    /** Igual exista o no la cuenta: no dice si la hay. */
    static final String DEMASIADOS_FALLOS = "Demasiadas contraseñas incorrectas seguidas con ese usuario. Espera unos"
            + " minutos o pide un enlace en «He olvidado la contraseña» para entrar ya.";

    private final UsuarioMapper usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final ConfirmacionEmailService confirmaciones;
    private final LimiteIntentosService limite;
    private final AppProperties.Limite fallosPorCuenta;

    public AuthService(
            UsuarioMapper usuarios,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwt,
            ApplicationEventPublisher eventos,
            Clock reloj,
            ConfirmacionEmailService confirmaciones,
            LimiteIntentosService limite,
            AppProperties propiedades) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.eventos = eventos;
        this.reloj = reloj;
        this.confirmaciones = confirmaciones;
        this.limite = limite;
        this.fallosPorCuenta = propiedades.limiteIntentos().loginCuenta();
    }

    @Transactional
    public PerfilDto registrar(RegistroRequest datos) {
        Usuario usuario = crearCuenta(
                datos.id(),
                datos.nombre(),
                datos.apellido1(),
                datos.apellido2(),
                datos.email(),
                passwordEncoder.encode(datos.password()),
                null);
        return PerfilDto.de(usuario, List.of(UsuarioAutenticado.ROL_USER));
    }

    /**
     * Crea la cuenta con el rol USER y manda el correo de bienvenida: el alta normal y la de quien entra por primera
     * vez con Google (D-085). 409 si el nombre de usuario o el email ya los usa otra cuenta. En el alta normal, el
     * correo lleva el enlace para confirmar el email; con Google ya está verificado (D-088).
     */
    Usuario crearCuenta(
            String idPedido,
            String nombre,
            String apellido1,
            @Nullable String apellido2,
            String emailPedido,
            String passwordCifrada,
            @Nullable String googleId) {
        String id = idPedido.strip();
        String email = emailPedido.strip();
        Map<String, String> errores = new LinkedHashMap<>();
        if (usuarios.existeId(id)) {
            errores.put("id", ID_EN_USO);
        }
        if (usuarios.existeEmail(email, null)) {
            errores.put("email", EMAIL_EN_USO);
        }
        if (!errores.isEmpty()) {
            throw ExcepcionApi.conflicto("Ya existe una cuenta con esos datos.", errores);
        }

        Usuario usuario =
                Usuario.nuevo(id, nombre.strip(), apellido1.strip(), Texto.opcional(apellido2), email, passwordCifrada);
        usuarios.insertar(usuario);
        usuarios.asignarRol(id, UsuarioAutenticado.ROL_USER);
        String tokenConfirmacion = null;
        if (googleId != null) {
            usuarios.vincularGoogle(id, googleId);
            usuarios.marcarEmailVerificado(id);
        } else {
            tokenConfirmacion = confirmaciones.prepararEnlace(id, null);
        }
        eventos.publishEvent(new UsuarioRegistrado(id, usuario.nombre(), email, tokenConfirmacion));
        return usuario;
    }

    /**
     * Una contraseña que nadie conoce, para las cuentas creadas con Google (D-085): quien quiera entrar también con
     * contraseña la crea con «¿Olvidaste tu contraseña?».
     */
    String passwordAleatoria() {
        return passwordEncoder.encode(TokenSeguro.nuevo());
    }

    /**
     * Los datos de la sesión vigente (D-085): {@code yo} solo llega si el filtro JWT ha aceptado el token (cuenta
     * activa y versión de sesión al día, D-063 y D-070), y el token da su caducidad. 401 si no hay sesión.
     */
    public LoginResponse datosSesion(@Nullable String token, @Nullable UsuarioAutenticado yo) {
        JwtService.SesionToken sesion = Optional.ofNullable(yo)
                .flatMap(usuario -> Optional.ofNullable(token).flatMap(jwt::validar))
                .filter(valida -> valida.usuario().id().equals(yo.id()))
                .orElseThrow(() -> ExcepcionApi.noAutorizado("SIN_SESION", SIN_SESION));
        boolean emailVerificado =
                usuarios.buscarPorId(yo.id()).map(Usuario::emailVerificado).orElse(false);
        return new LoginResponse(
                sesion.caduca(), new LoginResponse.UsuarioSesion(yo.id(), yo.nombre(), yo.roles(), emailVerificado));
    }

    /** Un token nuevo para una sesión ya abierta, con la versión de sesión actual (tras confirmar un email, D-089). */
    String tokenRenovado(UsuarioAutenticado yo) {
        return jwt.emitir(
                        new UsuarioAutenticado(yo.id(), yo.nombre(), usuarios.rolesDe(yo.id())),
                        usuarios.versionSesion(yo.id()))
                .valor();
    }

    /** La sesión de un usuario ya identificado (con su contraseña o con Google): el token y lo que ve el frontend. */
    SesionIniciada sesion(Usuario usuario) {
        String id = usuario.id();
        List<String> roles = usuarios.rolesDe(id);
        JwtService.TokenEmitido token =
                jwt.emitir(new UsuarioAutenticado(id, usuario.nombre(), roles), usuarios.versionSesion(id));
        return new SesionIniciada(
                token.valor(),
                new LoginResponse(
                        token.caduca(),
                        new LoginResponse.UsuarioSesion(id, usuario.nombre(), roles, usuario.emailVerificado())));
    }

    /**
     * Con el usuario o el email que ha agotado sus contraseñas fallidas (desde cualquier IP), 429 sin comprobar nada
     * (D-090).
     *
     * <p>No es de solo lectura: si el hash de la contraseña tiene un coste antiguo, se rehace al entrar (D-090). Una
     * contraseña de más de 72 bytes no puede ser la de nadie: bcrypt solo compararía el principio, y al rehacer el hash
     * se negaría a cifrarla.
     */
    @Transactional
    public SesionIniciada login(LoginRequest datos) {
        String escrito = datos.usuario().strip();
        Duration espera = limite.agotado(claveFallos(escrito), fallosPorCuenta);
        if (espera.isPositive()) {
            throw ExcepcionApi.limiteAlcanzado("DEMASIADOS_INTENTOS", DEMASIADOS_FALLOS, espera);
        }
        if (!CifradoPassword.cabe(datos.password())) {
            throw fallo(escrito);
        }
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(escrito, datos.password()));
        } catch (AuthenticationException e) {
            throw fallo(escrito);
        }
        String id = autenticacion.getName();
        Usuario usuario = usuarios.buscarPorId(id)
                .orElseThrow(() -> ExcepcionApi.noAutorizado("CREDENCIALES_INVALIDAS", CREDENCIALES_INVALIDAS));
        // Después de comprobar la contraseña: a quien no la sabe no se le dice si la cuenta está bloqueada (D-063)
        if (usuario.bloqueado()) {
            throw ExcepcionApi.prohibido("CUENTA_BLOQUEADA", CUENTA_BLOQUEADA);
        }
        return sesion(usuario);
    }

    /**
     * Si el email está registrado, guarda el hash de un token nuevo con caducidad de 1 hora y lo envía por correo. Si
     * no lo está, no hace nada: el llamante responde lo mismo en ambos casos para no revelar qué emails existen.
     */
    @Transactional
    public void solicitarRecuperacion(String email) {
        usuarios.buscarPorEmail(email.strip()).ifPresent(usuario -> {
            String token = nuevoToken();
            usuarios.guardarTokenPassword(
                    usuario.id(), sha256(token), reloj.instant().plus(VALIDEZ_TOKEN_RECUPERACION));
            eventos.publishEvent(new RecuperacionPasswordSolicitada(usuario.nombre(), usuario.email(), token));
        });
    }

    /**
     * Cambia la contraseña con un token de recuperación válido, que deja de servir. Anula las sesiones abiertas (D-070).
     * El enlace llegó al email de la cuenta, así que ese email queda confirmado (D-088).
     */
    @Transactional
    public void restablecer(RestablecerPasswordRequest datos) {
        Usuario usuario = usuarios.buscarPorTokenPassword(sha256(datos.token().strip()))
                .filter(u -> u.tokenExpiracion() != null && reloj.instant().isBefore(u.tokenExpiracion()))
                .orElseThrow(() -> ExcepcionApi.peticionIncorrecta("TOKEN_INVALIDO", TOKEN_INVALIDO, null));
        usuarios.actualizarPassword(usuario.id(), passwordEncoder.encode(datos.password()));
        usuarios.marcarEmailVerificado(usuario.id());
        // Quien ha agotado los fallos de su cuenta (o se los ha agotado otro) puede entrar ya (D-090)
        limite.olvidar(claveFallos(usuario.id()));
        limite.olvidar(claveFallos(usuario.email()));
    }

    /**
     * Contraseñas fallidas en el login, por lo que se escribe (usuario o email) y no por la cuenta (D-090): si los dos
     * compartieran contador, agotándolo con un nombre de usuario, que es público, se sabría si un email es suyo. La
     * colación de la tabla ya junta las mayúsculas y los acentos.
     */
    private static String claveFallos(String usuarioOEmail) {
        return "login-cuenta:" + usuarioOEmail;
    }

    /** Cuenta el fallo aunque el login se deshaga ({@link LimiteIntentosService#registrarFallo}). */
    private ExcepcionApi fallo(String escrito) {
        limite.registrarFallo(claveFallos(escrito), fallosPorCuenta);
        return ExcepcionApi.noAutorizado("CREDENCIALES_INVALIDAS", CREDENCIALES_INVALIDAS);
    }

    private String nuevoToken() {
        return TokenSeguro.nuevo();
    }

    /** En la base de datos solo se guarda el SHA-256 del token, en hexadecimal (6.9). */
    static String sha256(String texto) {
        return TokenSeguro.sha256(texto);
    }
}
