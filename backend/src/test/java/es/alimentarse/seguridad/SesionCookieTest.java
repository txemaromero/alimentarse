package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Sesión en cookie, CSRF y anulación de sesiones (D-070). */
@TestIntegracion
@Transactional
class SesionCookieTest {

    private static final String CLAVE = "clave-de-cookie";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JsonMapper json;

    @BeforeEach
    void crearUsuario() {
        datos.usuario("galletas");
        jdbc.update("UPDATE usuarios SET PASSWORD = ? WHERE ID = 'galletas'", passwordEncoder.encode(CLAVE));
    }

    @Test
    void elLoginDejaElTokenEnUnaCookieInaccesibleParaElJavascript() throws Exception {
        MvcTestResult login = login();

        assertThat(login)
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.usuario.id", v -> v.assertThat().isEqualTo("galletas"))
                .hasPathSatisfying("$.caduca", v -> v.assertThat().isNotNull())
                .doesNotHavePath("$.token");
        String cookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .startsWith(CookieSesion.NOMBRE + "=")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api", "Max-Age=28800");
        assertThat(login.getResponse().getContentAsString()).doesNotContain(valorDe(login));
    }

    @Test
    void laCookieAutenticaLasPeticiones() {
        assertThat(mvc.get().uri("/api/usuarios/me").cookie(sesion(login())))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.id", v -> v.assertThat().isEqualTo("galletas"));
    }

    @Test
    void conCookieUnaEscrituraExigeElTokenCsrf() {
        Cookie sesion = sesion(login());

        assertThat(mvc.put()
                        .uri("/api/usuarios/me/password")
                        .cookie(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("ACCESO_DENEGADO"));
    }

    /** Otra cabecera Authorization (la de un proxy con usuario y contraseña) no evita el CSRF de la cookie (D-086). */
    @Test
    void conCookieYOtraCabeceraAuthorizationTambienSeExigeElTokenCsrf() {
        assertThat(mvc.put()
                        .uri("/api/usuarios/me/password")
                        .cookie(sesion(login()))
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXN1YXJpbzpjbGF2ZQ==")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("ACCESO_DENEGADO"));
    }

    @Test
    void conElTokenCsrfDeLaCookieLaEscrituraSeAcepta() {
        Cookie sesion = sesion(login());
        Cookie xsrf = mvc.get()
                .uri("/api/usuarios/me")
                .cookie(sesion)
                .exchange()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();

        assertThat(mvc.put()
                        .uri("/api/usuarios/me/password")
                        .cookie(sesion, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("actual", "incorrecta", "nueva", "otra-clave-larga"))))
                // Pasa el CSRF: el error ya es de la propia petición
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("PASSWORD_INCORRECTA"));
    }

    /** Si cambiara en cada respuesta, dos peticiones simultáneas podrían acabar en un 403. */
    @Test
    void elTokenCsrfNoCambiaEntrePeticiones() {
        Cookie sesion = sesion(login());
        Cookie xsrf = mvc.get()
                .uri("/api/usuarios/me")
                .cookie(sesion)
                .exchange()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        MvcTestResult siguiente =
                mvc.get().uri("/api/usuarios/me").cookie(sesion, xsrf).exchange();

        assertThat(siguiente).hasStatusOk();
        assertThat(siguiente.getResponse().getCookie("XSRF-TOKEN")).isNull();
    }

    @Test
    void conLaCabeceraAuthorizationNoHaceFaltaCsrf() {
        assertThat(mvc.put()
                        .uri("/api/usuarios/me/password")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("galletas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("actual", "incorrecta", "nueva", "otra-clave-larga"))))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unaEscrituraSinSesionEsUn401() {
        assertThat(mvc.post()
                        .uri("/api/consumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void elLogoutBorraLaCookie() {
        MvcTestResult salida =
                mvc.post().uri("/api/auth/logout").cookie(sesion(login())).exchange();

        assertThat(salida).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(salida.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(CookieSesion.NOMBRE + "=;")
                .contains("Max-Age=0");
    }

    @Test
    void cambiarLaContrasenaAnulaLasOtrasSesionesYRenuevaLaPropia() {
        String otroDispositivo = bearer(login());

        MvcTestResult cambio = mvc.put()
                .uri("/api/usuarios/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(login()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("actual", CLAVE, "nueva", "otra-clave-larga")))
                .exchange();

        assertThat(cambio).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(usandoToken(otroDispositivo)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/usuarios/me").cookie(sesion(cambio))).hasStatusOk();
    }

    @Test
    void restablecerLaContrasenaAnulaLasSesiones() {
        String sesionAbierta = bearer(login());
        jdbc.update(
                "UPDATE usuarios SET TOKEN_PASSWORD = SHA2('enlace', 256), TOKEN_EXPIRACION = NOW() + INTERVAL 1 HOUR"
                        + " WHERE ID = 'galletas'");

        assertThat(mvc.post()
                        .uri("/api/auth/password/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("token", "enlace", "password", "otra-clave-larga"))))
                .hasStatusOk();

        assertThat(usandoToken(sesionAbierta)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cambiarElRolAnulaLaSesionDelAfectado() {
        datos.admin("jefa");
        String sesionAbierta = bearer(login());

        assertThat(mvc.put()
                        .uri("/api/admin/usuarios/galletas/admin")
                        .header(HttpHeaders.AUTHORIZATION, tokens.admin("jefa"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admin\": true}"))
                .hasStatusOk();

        assertThat(usandoToken(sesionAbierta)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/usuarios/me").cookie(sesion(login()))).hasStatusOk();
    }

    private MvcTestResult usandoToken(String cabecera) {
        return mvc.get()
                .uri("/api/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, cabecera)
                .exchange();
    }

    private MvcTestResult login() {
        return mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(Map.of("usuario", "galletas", "password", CLAVE)))
                .exchange();
    }

    private String cuerpo(Map<String, String> campos) {
        return json.writeValueAsString(campos);
    }

    private static String valorDe(MvcTestResult respuesta) {
        return respuesta.getResponse().getCookie(CookieSesion.NOMBRE).getValue();
    }

    private static Cookie sesion(MvcTestResult respuesta) {
        return new Cookie(CookieSesion.NOMBRE, valorDe(respuesta));
    }

    private static String bearer(MvcTestResult respuesta) {
        return "Bearer " + valorDe(respuesta);
    }
}
