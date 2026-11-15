package es.alimentarse.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.seguridad.CookieSesion;
import es.alimentarse.support.BuzonDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import tools.jackson.databind.json.JsonMapper;

@TestIntegracion
class UsuarioControllerTest {

    private static final AtomicInteger CONTADOR = new AtomicInteger();
    private static final Pattern TOKEN_EN_ENLACE = Pattern.compile("confirmar-email\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    BuzonDePrueba buzon;

    private String id;
    private String token;

    @BeforeEach
    void crearUsuario() {
        id = "perfil" + CONTADOR.incrementAndGet() + "-" + System.currentTimeMillis() % 1_000_000;
        jdbc.update(
                "INSERT INTO usuarios (ID, NOMBRE, APELLIDO1, APELLIDO2, EMAIL, PASSWORD) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                "Lucía",
                "Fernández",
                "Ruiz",
                id + "@pruebas.local",
                passwordEncoder.encode("clave-actual"));
        jdbc.update("INSERT INTO usuario_rol (USUARIO, ROL) SELECT ?, ID FROM roles WHERE NOMBRE = 'USER'", id);
        token = tokens.usuario(id);
    }

    @Test
    void sinTokenLaApiDevuelve401ConElFormatoDeError() {
        assertThat(mvc.get().uri("/api/usuarios/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("NO_AUTENTICADO"))
                .hasPathSatisfying("$.mensaje", v -> v.assertThat().asString().isNotBlank());
    }

    @Test
    void unaCabeceraAcceptQueNoSePuedeAtenderDa406() {
        // D-089: sin manejador propio era un 500, con la traza en el log
        assertThat(mvc.get()
                        .uri("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .accept(MediaType.parseMediaType("text/csv")))
                .hasStatus(HttpStatus.NOT_ACCEPTABLE);
    }

    @Test
    void unTokenInvalidoDevuelve401() {
        assertThat(mvc.get().uri("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer no-vale"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void elPerfilDevuelveLosDatosDelUsuarioSinLaContrasena() {
        assertThat(mvc.get().uri("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, token))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.id", v -> v.assertThat().isEqualTo(id))
                .hasPathSatisfying("$.apellido2", v -> v.assertThat().isEqualTo("Ruiz"))
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@pruebas.local"))
                .doesNotHavePath("$.password");
    }

    @Test
    void cambiarElEmailPideLaContrasenaYNoSeUsaHastaQueSeConfirmaConElEnlace() {
        assertThat(put(
                        "/api/usuarios/me",
                        Map.of("nombre", "Lucía María", "apellido1", "Fernández", "email", id + "@nuevo.local")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.password", v -> v.assertThat().isEqualTo(UsuarioService.PASSWORD_PARA_EMAIL));

        assertThat(put(
                        "/api/usuarios/me",
                        Map.of(
                                "nombre",
                                "Lucía María",
                                "apellido1",
                                "Fernández",
                                "email",
                                id + "@nuevo.local",
                                "password",
                                "clave-actual")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.nombre", v -> v.assertThat().isEqualTo("Lucía María"))
                .hasPathSatisfying("$.apellido2", v -> v.assertThat().isNull())
                // Sigue con el anterior hasta que se confirme el nuevo (D-088)
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@pruebas.local"))
                .hasPathSatisfying("$.emailPendiente", v -> v.assertThat().isEqualTo(id + "@nuevo.local"));

        String enlace = tokenDelEnlace(buzon.esperarCorreoPara(id + "@nuevo.local", "Confirma tu nuevo email"));
        MvcTestResult confirmado = mvc.post()
                .uri("/api/auth/email/confirmar")
                .header(HttpHeaders.AUTHORIZATION, token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", enlace)))
                .exchange();
        assertThat(confirmado)
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.usuario", v -> v.assertThat().isEqualTo(id))
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@nuevo.local"));
        // Las sesiones abiertas se cierran; la del navegador que confirma sigue con un token nuevo (D-089)
        assertThat(mvc.get().uri("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, token()))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        Cookie renovada = confirmado.getResponse().getCookie(CookieSesion.NOMBRE);
        assertThat(renovada).isNotNull();
        assertThat(mvc.get().uri("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + renovada.getValue()))
                .bodyJson()
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@nuevo.local"))
                .hasPathSatisfying("$.emailPendiente", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.emailVerificado", v -> v.assertThat().isEqualTo(true));
        assertThat(BuzonDePrueba.html(buzon.esperarCorreoPara(id + "@pruebas.local", "Ha cambiado el email")))
                .contains(id + "@nuevo.local");
        // El enlace solo sirve una vez
        assertThat(confirmar(enlace)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sePuedeReenviarElEnlaceOCancelarElCambio() {
        assertThat(post("/api/usuarios/me/email/confirmacion", Map.of()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@pruebas.local"));
        String alta = tokenDelEnlace(buzon.esperarCorreoPara(id + "@pruebas.local", "Confirma tu email"));

        put(
                "/api/usuarios/me",
                Map.of(
                        "nombre",
                        "Lucía",
                        "apellido1",
                        "Fernández",
                        "email",
                        id + "@otro.local",
                        "password",
                        "clave-actual"));
        String cambio = tokenDelEnlace(buzon.esperarCorreoPara(id + "@otro.local", "Confirma tu nuevo email"));
        // Un enlace a la vez: el del alta ya no vale
        assertThat(confirmar(alta)).hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(mvc.delete().uri("/api/usuarios/me/email/pendiente").header(HttpHeaders.AUTHORIZATION, token()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.email", v -> v.assertThat().isEqualTo(id + "@pruebas.local"))
                .hasPathSatisfying("$.emailPendiente", v -> v.assertThat().isNull());
        assertThat(confirmar(cambio)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void conElEmailConfirmadoNoHayNadaQueReenviar() {
        jdbc.update("UPDATE usuarios SET EMAIL_VERIFICADO = TRUE WHERE ID = ?", id);

        assertThat(post("/api/usuarios/me/email/confirmacion", Map.of()))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("EMAIL_YA_CONFIRMADO"));
    }

    @Test
    void sinCambiarElEmailNoHaceFaltaLaContrasena() {
        assertThat(put(
                        "/api/usuarios/me",
                        Map.of("nombre", "Lucía", "apellido1", "Fernández", "email", id + "@pruebas.local")))
                .hasStatusOk();
    }

    @Test
    void desvincularGoogleConLaContrasenaYAvisaPorCorreo() {
        jdbc.update("UPDATE usuarios SET GOOGLE_ID = ? WHERE ID = ?", "google-" + id, id);

        assertThat(post("/api/usuarios/me/desvincular-google", Map.of("password", "incorrecta")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.password", v -> v.assertThat().isNotNull());
        assertThat(post("/api/usuarios/me/desvincular-google", Map.of("password", "clave-actual")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.google", v -> v.assertThat().isEqualTo(false));

        assertThat(jdbc.queryForObject("SELECT GOOGLE_ID FROM usuarios WHERE ID = ?", String.class, id))
                .isNull();
        assertThat(buzon.esperarCorreoPara(id + "@pruebas.local", "ya no está vinculada con Google"))
                .isNotNull();
    }

    @Test
    void noSePuedeUsarElEmailDeOtraCuenta() {
        jdbc.update(
                "INSERT INTO usuarios (ID, NOMBRE, APELLIDO1, EMAIL, PASSWORD) VALUES (?, 'Otra', 'Cuenta', ?, 'x')",
                id + "b",
                id + "b@pruebas.local");

        assertThat(put(
                        "/api/usuarios/me",
                        Map.of(
                                "nombre",
                                "Lucía",
                                "apellido1",
                                "Fernández",
                                "email",
                                id + "b@pruebas.local",
                                "password",
                                "clave-actual")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.errores.email", v -> v.assertThat().isEqualTo(UsuarioService.EMAIL_EN_USO));
    }

    @Test
    void cambiarLaContrasenaExigeLaActualYDespuesSeUsaLaNueva() {
        assertThat(put("/api/usuarios/me/password", Map.of("actual", "incorrecta", "nueva", "clave-nueva")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.actual", v -> v.assertThat().isEqualTo(UsuarioService.PASSWORD_ACTUAL_INCORRECTA));

        assertThat(put("/api/usuarios/me/password", Map.of("actual", "clave-actual", "nueva", "clave-nueva")))
                .hasStatus(HttpStatus.NO_CONTENT);
        String hash = jdbc.queryForObject("SELECT PASSWORD FROM usuarios WHERE ID = ?", String.class, id);
        assertThat(passwordEncoder.matches("clave-nueva", hash)).isTrue();
    }

    @Test
    void lasRutasDeAdministracionExigenElRolAdmin() {
        assertThat(mvc.get().uri("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("ACCESO_DENEGADO"));
        // Un token con ADMIN no basta: el rol se comprueba también en la base de datos (D-043, AdminControllerTest)
        assertThat(mvc.get().uri("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, tokens.admin(id)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void lasRutasQueNoSonDeLaApiNoExigenSesion() {
        assertThat(mvc.get()
                        .uri("/una-ruta-de-angular")
                        .exchange()
                        .getResponse()
                        .getStatus())
                .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private MvcTestResult confirmar(String tokenEnlace) {
        return mvc.post()
                .uri("/api/auth/email/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", tokenEnlace)))
                .exchange();
    }

    private String token() {
        return token;
    }

    private static String tokenDelEnlace(jakarta.mail.internet.MimeMessage correo) {
        Matcher enlace = TOKEN_EN_ENLACE.matcher(BuzonDePrueba.html(correo));
        assertThat(enlace.find()).isTrue();
        return enlace.group(1);
    }

    private MvcTestResult post(String uri, Map<String, ?> cuerpo) {
        return mvc.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))
                .exchange();
    }

    private MvcTestResult put(String uri, Map<String, ?> cuerpo) {
        return mvc.put()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))
                .exchange();
    }
}
