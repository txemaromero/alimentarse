package es.alimentarse.auth;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.config.AppProperties;
import es.alimentarse.config.ContrasenasProperties;
import es.alimentarse.seguridad.CookieSesion;
import es.alimentarse.support.BuzonDePrueba;
import es.alimentarse.support.TestIntegracion;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

@TestIntegracion
class AuthControllerTest {

    private static final AtomicInteger CONTADOR = new AtomicInteger();
    private static final int COSTE_ANTIGUO = 4;
    private static final Pattern TOKEN_EN_ENLACE = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    BuzonDePrueba buzon;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ContrasenasProperties contrasenas;

    @Autowired
    AppProperties propiedades;

    @Test
    void elRegistroCreaLaCuentaConRolUserYEnviaElCorreoDeBienvenida() throws Exception {
        String id = nuevoId();

        assertThat(registrar(id, id + "@pruebas.local", "clave-segura"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.id", v -> v.assertThat().isEqualTo(id))
                .hasPathSatisfying("$.roles", v -> v.assertThat().asArray().containsExactly("USER"))
                .doesNotHavePath("$.password");

        assertThat(jdbc.queryForObject("SELECT PASSWORD FROM usuarios WHERE ID = ?", String.class, id))
                .startsWith("$2a$");
        assertThat(buzon.esperarCorreoPara(id + "@pruebas.local").getSubject()).contains("bienvenida");
    }

    /** La bienvenida lleva el enlace para confirmar el email, que sirve una vez y sin sesión (D-088). */
    @Test
    void elEnlaceDeLaBienvenidaConfirmaElEmail() throws Exception {
        String id = nuevoId();
        registrar(id, id + "@pruebas.local", "clave-segura");
        assertThat(jdbc.queryForObject("SELECT EMAIL_VERIFICADO FROM usuarios WHERE ID = ?", Boolean.class, id))
                .isFalse();

        Matcher enlace = Pattern.compile("confirmar-email\\?token=([A-Za-z0-9_-]+)")
                .matcher(BuzonDePrueba.html(buzon.esperarCorreoPara(id + "@pruebas.local", "bienvenida")));
        assertThat(enlace.find()).isTrue();
        assertThat(post("/api/auth/email/confirmar", Map.of("token", enlace.group(1))))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@pruebas.local"));

        assertThat(jdbc.queryForObject("SELECT EMAIL_VERIFICADO FROM usuarios WHERE ID = ?", Boolean.class, id))
                .isTrue();
        assertThat(post("/api/auth/email/confirmar", Map.of("token", enlace.group(1))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("TOKEN_INVALIDO"));
        assertThat(login(id, "clave-segura"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.usuario.emailVerificado", v -> v.assertThat().isEqualTo(true));
    }

    @Test
    void registrarseConIdYEmailOcupadosDevuelve409ConAmbosCampos() {
        String id = nuevoId();
        registrar(id, id + "@pruebas.local", "clave-segura");

        assertThat(registrar(id.toUpperCase(), id.toUpperCase() + "@PRUEBAS.LOCAL", "clave-segura"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("CONFLICTO"))
                .hasPathSatisfying("$.errores.id", v -> v.assertThat().isEqualTo(AuthService.ID_EN_USO))
                .hasPathSatisfying("$.errores.email", v -> v.assertThat().isEqualTo(AuthService.EMAIL_EN_USO));
    }

    @Test
    void elRegistroValidaCadaCampoConMensajesEnEspanol() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", "a@");
        datos.put("nombre", "");
        datos.put("email", "no-es-un-email");
        datos.put("password", "corta");

        assertThat(post("/api/auth/registro", datos))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("VALIDACION"))
                .hasPathSatisfying(
                        "$.errores",
                        v -> v.assertThat().asMap().containsOnlyKeys("id", "nombre", "apellido1", "email", "password"))
                .hasPathSatisfying(
                        "$.errores.apellido1", v -> v.assertThat().isEqualTo("El primer apellido es obligatorio"));
    }

    @Test
    void unCuerpoQueNoEsJsonDevuelve400() {
        assertThat(mvc.post()
                        .uri("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("PETICION_INCORRECTA"));
    }

    @Test
    void seIniciaSesionConElIdOConElEmailYElTokenDaAccesoALaApi() throws Exception {
        String id = nuevoId();
        registrar(id, id + "@pruebas.local", "clave-segura");

        assertThat(login(id, "clave-segura"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.usuario.id", v -> v.assertThat().isEqualTo(id))
                .hasPathSatisfying(
                        "$.usuario.roles", v -> v.assertThat().asArray().containsExactly("USER"));

        String token = login(id.toUpperCase() + "@Pruebas.local", "clave-segura")
                .getResponse()
                .getCookie(CookieSesion.NOMBRE)
                .getValue();
        assertThat(mvc.get().uri("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.id", v -> v.assertThat().isEqualTo(id));
    }

    @Test
    void unaContrasenaIncorrectaDevuelve401ConMensajeGenerico() {
        String id = nuevoId();
        registrar(id, id + "@pruebas.local", "clave-segura");

        assertThat(login(id, "otra-clave"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("CREDENCIALES_INVALIDAS"))
                .hasPathSatisfying("$.mensaje", v -> v.assertThat().isEqualTo(AuthService.CREDENCIALES_INVALIDAS));
        assertThat(login("no-existe", "otra-clave")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** 40 «ñ» caben en 72 caracteres, pero no en los 72 bytes de bcrypt: 400 y no 500 (D-090). */
    @Test
    void elRegistroRechazaUnaContrasenaDeMasDe72Bytes() {
        String id = nuevoId();

        assertThat(registrar(id, id + "@pruebas.local", "ñ".repeat(40)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.password",
                        v -> v.assertThat()
                                .isEqualTo("La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan"
                                        + " como más de un carácter"));
    }

    /**
     * Un hash de un coste anterior se rehace al entrar, sin cerrar las demás sesiones (D-090). En los tests el coste es
     * 5, y el anterior, 4, el mínimo de bcrypt.
     */
    @Test
    void alEntrarConUnHashAntiguoSeRehaceConElCosteActual() {
        assertThat(contrasenas.costeBcrypt()).isGreaterThan(COSTE_ANTIGUO);
        String id = nuevoId();
        assertThat(registrar(id, id + "@pruebas.local", "clave-segura")).hasStatus(HttpStatus.CREATED);
        jdbc.update("UPDATE usuarios SET PASSWORD = ? WHERE ID = ?", hashAntiguo("clave-segura"), id);
        Integer version = versionSesion(id);

        assertThat(login(id, "clave-segura")).hasStatusOk();

        assertThat(hashPassword(id)).startsWith("$2a$%02d$".formatted(contrasenas.costeBcrypt()));
        assertThat(versionSesion(id)).isEqualTo(version);
        assertThat(login(id, "clave-segura")).hasStatusOk();
    }

    /**
     * bcrypt solo compara los 72 primeros bytes: lo que pase de ahí no es la contraseña de nadie. Además, con un hash
     * antiguo, rehacerlo con ella daba 500 (D-090).
     */
    @Test
    void unaContrasenaDeMasDe72BytesNoEntraAunqueEmpieceComoLaBuena() {
        String id = nuevoId();
        String password = "a".repeat(72);
        assertThat(registrar(id, id + "@pruebas.local", password)).hasStatus(HttpStatus.CREATED);
        jdbc.update("UPDATE usuarios SET PASSWORD = ? WHERE ID = ?", hashAntiguo(password), id);

        assertThat(login(id, password + "ñ"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("CREDENCIALES_INVALIDAS"));
        assertThat(login(id, password)).hasStatusOk();
    }

    /**
     * Agotadas las contraseñas fallidas de un usuario (5 en los tests, 20 en producción), 429 aunque cambie la IP y
     * aunque llegue la buena. El enlace de recuperación lo pone a cero (D-090).
     */
    @Test
    void lasContrasenasFallidasDeUnUsuarioTienenTopeDesdeCualquierIp() throws Exception {
        String id = nuevoId();
        String email = id + "@pruebas.local";
        assertThat(registrar(id, email, "clave-segura")).hasStatus(HttpStatus.CREATED);
        for (int i = 0; i < fallosPorCuenta(); i++) {
            assertThat(loginDesde("10.77." + i + ".1", id, "mala-" + i)).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        MvcTestResult agotado = loginDesde("10.77.99.1", id, "clave-segura");
        assertThat(agotado)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("DEMASIADOS_INTENTOS"))
                .hasPathSatisfying("$.mensaje", v -> v.assertThat().isEqualTo(AuthService.DEMASIADOS_FALLOS));
        assertThat(agotado.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
        // El email cuenta aparte: si compartieran contador, se sabría que es de ese usuario
        assertThat(login(email, "clave-segura")).hasStatusOk();

        olvido(email);
        String token = tokenDelCorreo(email);
        assertThat(post("/api/auth/password/restablecer", Map.of("token", token, "password", "clave-nueva")))
                .hasStatusOk();
        assertThat(login(id, "clave-nueva")).hasStatusOk();
    }

    /** Con un usuario que no existe pasa lo mismo: el tope no dice si hay cuenta. */
    @Test
    void elTopeDeFallosNoDiceSiLaCuentaExiste() {
        String nadie = "nadie-" + nuevoId();
        for (int i = 0; i < fallosPorCuenta(); i++) {
            assertThat(loginDesde("10.78." + i + ".1", nadie, "x")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        assertThat(loginDesde("10.78.99.1", nadie, "x"))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.mensaje", v -> v.assertThat().isEqualTo(AuthService.DEMASIADOS_FALLOS));
    }

    @Test
    void elOlvidoDeContrasenaRespondeIgualExistaONoElEmail() throws Exception {
        String id = nuevoId();
        String email = id + "@pruebas.local";
        registrar(id, email, "clave-segura");

        MvcTestResult existente = olvido(email);
        MvcTestResult desconocido = olvido("nadie-" + id + "@pruebas.local");

        assertThat(existente).hasStatusOk();
        assertThat(desconocido).hasStatusOk();
        assertThat(existente.getResponse().getContentAsString())
                .isEqualTo(desconocido.getResponse().getContentAsString())
                .contains(AuthController.MENSAJE_OLVIDO);

        String token = tokenDelCorreo(email);
        Map<String, Object> fila =
                jdbc.queryForMap("SELECT TOKEN_PASSWORD, TOKEN_EXPIRACION FROM usuarios WHERE ID = ?", id);
        assertThat(fila.get("TOKEN_PASSWORD")).isEqualTo(AuthService.sha256(token));
        Instant expiracion = ((java.sql.Timestamp) fila.get("TOKEN_EXPIRACION")).toInstant();
        assertThat(Duration.between(Instant.now(), expiracion))
                .isBetween(Duration.ofMinutes(58), Duration.ofMinutes(61));
    }

    @Test
    void elEnlaceDeRestablecerSoloSirveUnaVez() throws Exception {
        String id = nuevoId();
        String email = id + "@pruebas.local";
        registrar(id, email, "clave-segura");
        olvido(email);
        String token = tokenDelCorreo(email);

        assertThat(post("/api/auth/password/restablecer", Map.of("token", token, "password", "clave-nueva")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.mensaje", v -> v.assertThat().isEqualTo(AuthController.MENSAJE_RESTABLECIDA));
        assertThat(login(id, "clave-nueva")).hasStatusOk();
        assertThat(login(id, "clave-segura")).hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(post("/api/auth/password/restablecer", Map.of("token", token, "password", "otra-clave-mas")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("TOKEN_INVALIDO"));
    }

    @Test
    void elLoginTieneUnLimiteDeIntentosPorIp() {
        RequestPostProcessor ip = desdeIp("10.20.30.40");
        // application-test.yml: 100 intentos por minuto. Las peticiones mal formadas también cuentan.
        for (int i = 0; i < 100; i++) {
            assertThat(mvc.post()
                            .uri("/api/auth/login")
                            .with(ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        MvcTestResult bloqueada = mvc.post()
                .uri("/api/auth/login")
                .with(ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange();

        assertThat(bloqueada)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("DEMASIADOS_INTENTOS"));
        assertThat(Integer.parseInt(bloqueada.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
                .isBetween(1, 60);
        // Otra IP no está afectada
        assertThat(login("otro", "x")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void elOlvidoDeContrasenaTieneUnLimiteDeCincoIntentosPorIp() {
        RequestPostProcessor ip = desdeIp("10.20.30.41");
        for (int i = 0; i < 5; i++) {
            assertThat(mvc.post()
                            .uri("/api/auth/password/olvido")
                            .with(ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", "x" + i + "@pruebas.local"))))
                    .hasStatusOk();
        }

        assertThat(mvc.post()
                        .uri("/api/auth/password/olvido")
                        .with(ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "x@pruebas.local"))))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    private MvcTestResult registrar(String id, String email, String password) {
        return post(
                "/api/auth/registro",
                Map.of("id", id, "nombre", "Prueba", "apellido1", "Test", "email", email, "password", password));
    }

    private MvcTestResult login(String usuario, String password) {
        return post("/api/auth/login", Map.of("usuario", usuario, "password", password));
    }

    /** 5 en application-test.yml. */
    private int fallosPorCuenta() {
        return propiedades.limiteIntentos().loginCuenta().maximo();
    }

    private MvcTestResult loginDesde(String ip, String usuario, String password) {
        return mvc.post()
                .uri("/api/auth/login")
                .with(desdeIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("usuario", usuario, "password", password)))
                .exchange();
    }

    /** Desde una IP propia para no consumir el límite de intentos de otros tests. */
    private MvcTestResult olvido(String email) {
        return mvc.post()
                .uri("/api/auth/password/olvido")
                .with(desdeIp("10.99." + CONTADOR.incrementAndGet() % 250 + ".1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email)))
                .exchange();
    }

    private MvcTestResult post(String uri, Map<String, ?> cuerpo) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))
                .exchange();
    }

    private String tokenDelCorreo(String email) {
        Matcher enlace = TOKEN_EN_ENLACE.matcher(BuzonDePrueba.html(buzon.esperarCorreoPara(email, "Restablece")));
        assertThat(enlace.find())
                .as("El correo debe llevar el enlace con el token")
                .isTrue();
        return enlace.group(1);
    }

    private static RequestPostProcessor desdeIp(String ip) {
        return peticion -> {
            peticion.setRemoteAddr(ip);
            return peticion;
        };
    }

    /** Un hash como los de antes de subir el coste. */
    private static String hashAntiguo(String password) {
        return new BCryptPasswordEncoder(COSTE_ANTIGUO).encode(password);
    }

    private Integer versionSesion(String id) {
        return jdbc.queryForObject("SELECT VERSION_SESION FROM usuarios WHERE ID = ?", Integer.class, id);
    }

    private String hashPassword(String id) {
        return jdbc.queryForObject("SELECT PASSWORD FROM usuarios WHERE ID = ?", String.class, id);
    }

    private static String nuevoId() {
        return "u" + (System.currentTimeMillis() % 100_000_000) + "-" + CONTADOR.incrementAndGet();
    }
}
