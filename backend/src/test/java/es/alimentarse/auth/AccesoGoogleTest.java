package es.alimentarse.auth;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.seguridad.CookieSesion;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceso con Google contra MySQL real (D-085). Google no interviene: la ida se comprueba en la redirección y la vuelta
 * se simula con el usuario de OpenID Connect que daría Spring Security tras validar el token de Google.
 */
@TestIntegracion
@Transactional
class AccesoGoogleTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ManejadorAccesoGoogle manejador;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void conCredencialesHayBotonYElEnlaceLlevaAGoogleConPkce() {
        assertThat(mvc.get().uri("/api/auth/proveedores"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.google", v -> v.assertThat().isEqualTo(true));

        MvcTestResult ida = mvc.get().uri("/oauth2/authorization/google").exchange();

        assertThat(ida).hasStatus(HttpStatus.FOUND);
        assertThat(ida.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains(
                        "response_type=code",
                        "client_id=id-de-prueba",
                        "scope=openid%20email%20profile",
                        "state=",
                        "code_challenge=",
                        "code_challenge_method=S256",
                        "redirect_uri=http://localhost/login/oauth2/code/google");
    }

    @Test
    void quienYaTieneCuentaConEseEmailLaVinculaConSuContrasenaYDespuesEntraDirectamente() {
        datos.usuario("ana");
        jdbc.update("UPDATE usuarios SET PASSWORD = ? WHERE ID = 'ana'", passwordEncoder.encode("clave-de-ana"));

        MockHttpServletResponse vuelta = volverDeGoogle("google-ana", "ana@pruebas.local", true);

        // Todavía no entra ni se vincula: tiene que demostrar que la cuenta de Toca es suya (D-086)
        assertThat(vuelta.getRedirectedUrl()).isEqualTo(ManejadorAccesoGoogle.VINCULAR);
        assertThat(vuelta.getHeaders(HttpHeaders.SET_COOKIE)).noneMatch(c -> c.startsWith(CookieSesion.NOMBRE + "="));
        assertThat(jdbc.queryForObject("SELECT GOOGLE_ID FROM usuarios WHERE ID = 'ana'", String.class))
                .isNull();
        Cookie vinculacion =
                new Cookie(CookieSesion.NOMBRE_VINCULAR_GOOGLE, cookie(vuelta, CookieSesion.NOMBRE_VINCULAR_GOOGLE));
        assertThat(mvc.get().uri("/api/auth/google/vincular").cookie(vinculacion))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo("ana@pruebas.local"));

        assertThat(vincular(vinculacion, "otra"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.password", v -> v.assertThat().isNotNull());
        MvcTestResult vinculada = vincular(vinculacion, "clave-de-ana");
        assertThat(vinculada)
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.usuario.id", v -> v.assertThat().isEqualTo("ana"));
        assertThat(jdbc.queryForObject("SELECT GOOGLE_ID FROM usuarios WHERE ID = 'ana'", String.class))
                .isEqualTo("google-ana");
        // Sabe la contraseña y Google ha verificado el mismo email: queda confirmado (D-088)
        assertThat(jdbc.queryForObject("SELECT EMAIL_VERIFICADO FROM usuarios WHERE ID = 'ana'", Boolean.class))
                .isTrue();
        assertThat(vinculada.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(c -> c.startsWith(CookieSesion.NOMBRE_VINCULAR_GOOGLE + "=;"));
        String sesion = cookie(vinculada.getResponse(), CookieSesion.NOMBRE);
        assertThat(mvc.get().uri("/api/auth/sesion").cookie(new Cookie(CookieSesion.NOMBRE, sesion)))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.usuario.id", v -> v.assertThat().isEqualTo("ana"))
                .hasPathSatisfying(
                        "$.usuario.roles", v -> v.assertThat().asArray().containsExactly("USER"))
                .hasPathSatisfying("$.caduca", v -> v.assertThat().isNotNull());
        assertThat(mvc.get().uri("/api/usuarios/me").cookie(new Cookie(CookieSesion.NOMBRE, sesion)))
                .bodyJson()
                .hasPathSatisfying("$.google", v -> v.assertThat().isEqualTo(true));

        // La próxima vez la reconoce por su cuenta de Google, aunque cambie el email
        assertThat(volverDeGoogle("google-ana", "otro@gmail.com", true).getRedirectedUrl())
                .isEqualTo(ManejadorAccesoGoogle.ENTRAR);
        assertThat(mvc.get().uri("/api/auth/sesion")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void alguienNuevoEligeSuNombreDeUsuarioYEntraConUnaCuentaVinculada() {
        datos.usuario("ocupado");
        MockHttpServletResponse vuelta = volverDeGoogle("google-nuria", "nuria@gmail.com", true);

        assertThat(vuelta.getRedirectedUrl()).isEqualTo(ManejadorAccesoGoogle.REGISTRO);
        Cookie registro =
                new Cookie(CookieSesion.NOMBRE_REGISTRO_GOOGLE, cookie(vuelta, CookieSesion.NOMBRE_REGISTRO_GOOGLE));
        assertThat(mvc.get().uri("/api/auth/google/registro").cookie(registro))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo("nuria@gmail.com"))
                .hasPathSatisfying("$.nombre", v -> v.assertThat().isEqualTo("Nuria"))
                .hasPathSatisfying("$.apellido1", v -> v.assertThat().isEqualTo("Vidal"))
                .hasPathSatisfying("$.apellido2", v -> v.assertThat().isEqualTo("Soto"));

        assertThat(completar(registro, "ocupado"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.errores.id", v -> v.assertThat().isEqualTo(AuthService.ID_EN_USO));

        MvcTestResult creada = completar(registro, "nuria");
        assertThat(creada)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.usuario.id", v -> v.assertThat().isEqualTo("nuria"))
                .hasPathSatisfying(
                        "$.usuario.roles", v -> v.assertThat().asArray().containsExactly("USER"));
        List<String> cookies = creada.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(c -> c.startsWith(CookieSesion.NOMBRE + "=ey"));
        assertThat(cookies).anyMatch(c -> c.startsWith(CookieSesion.NOMBRE_REGISTRO_GOOGLE + "=;"));
        Map<String, Object> fila = jdbc.queryForMap("SELECT EMAIL, GOOGLE_ID, NOMBRE FROM usuarios WHERE ID = 'nuria'");
        assertThat(fila)
                .containsEntry("EMAIL", "nuria@gmail.com")
                .containsEntry("GOOGLE_ID", "google-nuria")
                .containsEntry("NOMBRE", "Nuria");
    }

    @Test
    void conElEmailConfirmadoSeVinculaSinPedirLaContrasena() {
        datos.usuario("berta");
        jdbc.update("UPDATE usuarios SET EMAIL_VERIFICADO = TRUE WHERE ID = 'berta'");

        MockHttpServletResponse vuelta = volverDeGoogle("google-berta", "berta@pruebas.local", true);

        assertThat(vuelta.getRedirectedUrl()).isEqualTo(ManejadorAccesoGoogle.ENTRAR);
        assertThat(jdbc.queryForObject("SELECT GOOGLE_ID FROM usuarios WHERE ID = 'berta'", String.class))
                .isEqualTo("google-berta");
        assertThat(mvc.get()
                        .uri("/api/auth/sesion")
                        .cookie(new Cookie(CookieSesion.NOMBRE, cookie(vuelta, CookieSesion.NOMBRE))))
                .bodyJson()
                .hasPathSatisfying("$.usuario.id", v -> v.assertThat().isEqualTo("berta"))
                .hasPathSatisfying(
                        "$.usuario.emailVerificado", v -> v.assertThat().isEqualTo(true));
    }

    @Test
    void sinVinculacionPendienteNoHayNadaQueVincular() {
        assertThat(mvc.get().uri("/api/auth/google/vincular"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("VINCULACION_GOOGLE_CADUCADA"));
        assertThat(vincular(new Cookie(CookieSesion.NOMBRE_VINCULAR_GOOGLE, "falso"), "x"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void sinRegistroPendienteNoHayNadaQueCompletar() {
        assertThat(mvc.get().uri("/api/auth/google/registro"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("REGISTRO_GOOGLE_CADUCADO"));
        assertThat(completar(new Cookie(CookieSesion.NOMBRE_REGISTRO_GOOGLE, "falso"), "nadie"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void sinEmailVerificadoOConLaCuentaBloqueadaVuelveAlLoginConElMotivo() {
        datos.usuario("bloqueada");
        jdbc.update("UPDATE usuarios SET BLOQUEADO = TRUE WHERE ID = 'bloqueada'");

        assertThat(volverDeGoogle("google-x", "x@gmail.com", false).getRedirectedUrl())
                .isEqualTo("/login?google=" + AccesoGoogleService.EMAIL_NO_VERIFICADO);
        assertThat(volverDeGoogle("google-b", "bloqueada@pruebas.local", true).getRedirectedUrl())
                .isEqualTo("/login?google=" + AccesoGoogleService.BLOQUEADA);
    }

    @Test
    void siCancelaOFallaEnGoogleVuelveAlLoginConUnAviso() throws Exception {
        assertThat(fallo("access_denied")).isEqualTo("/login?google=cancelado");
        assertThat(fallo("invalid_token_response")).isEqualTo("/login?google=error");
    }

    private MockHttpServletResponse volverDeGoogle(String id, String email, boolean verificado) {
        Instant ahora = Instant.now();
        OidcIdToken token = OidcIdToken.withTokenValue("token-de-google")
                .subject(id)
                .claim("email", email)
                .claim("email_verified", verificado)
                .claim("given_name", "Nuria")
                .claim("family_name", "Vidal Soto")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(60))
                .build();
        DefaultOidcUser usuario = new DefaultOidcUser(List.of(new OidcUserAuthority(token)), token);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.getSession(true);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        try {
            manejador.onAuthenticationSuccess(
                    peticion,
                    respuesta,
                    new OAuth2AuthenticationToken(usuario, usuario.getAuthorities(), AccesoGoogleConfig.REGISTRO));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        // La sesión HTTP solo servía para el intercambio con Google
        assertThat(peticion.getSession(false)).isNull();
        return respuesta;
    }

    private String fallo(String codigo) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        manejador.onAuthenticationFailure(
                peticion, respuesta, new OAuth2AuthenticationException(new OAuth2Error(codigo)));
        return respuesta.getRedirectedUrl();
    }

    private MvcTestResult vincular(Cookie vinculacion, String password) {
        return mvc.post()
                .uri("/api/auth/google/vincular")
                .cookie(vinculacion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"" + password + "\"}")
                .exchange();
    }

    private MvcTestResult completar(Cookie registro, String id) {
        return mvc.post()
                .uri("/api/auth/google/registro")
                .cookie(registro)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": \"" + id
                        + "\", \"nombre\": \"Nuria\", \"apellido1\": \"Vidal\", \"apellido2\": null}")
                .exchange();
    }

    private static String cookie(MockHttpServletResponse respuesta, String nombre) {
        return respuesta.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith(nombre + "="))
                .map(c -> c.substring(nombre.length() + 1, c.indexOf(';')))
                .findFirst()
                .orElseThrow();
    }
}
