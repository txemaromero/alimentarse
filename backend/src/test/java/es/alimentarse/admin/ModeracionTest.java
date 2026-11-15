package es.alimentarse.admin;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
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

/** Moderación (D-063): recetas ocultas y usuarios bloqueados, contra MySQL real. */
@TestIntegracion
@Transactional
class ModeracionTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    PasswordEncoder passwordEncoder;

    private int tortilla;

    @BeforeEach
    void crearFixture() {
        datos.admin("moderadora");
        datos.usuario("autora");
        datos.usuario("lectora");
        tortilla = datos.receta("Tortilla sospechosa", "autora", true, 2, "Huevos");
    }

    @Test
    void unaRecetaOcultaSoloLaVenSuAutorConElMotivoYLosAdministradores() {
        assertThat(ocultar(tortilla, true, " Publicidad encubierta "))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.oculta", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.motivoOculta", v -> v.assertThat().isEqualTo("Publicidad encubierta"));

        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("lectora"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get("/api/recetas", tokens.usuario("lectora")))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
        assertThat(get("/api/recetas/sugerencias?q=tortilla", tokens.usuario("lectora")))
                .bodyJson()
                .isEqualTo("[]");
        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("autora")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.motivoOculta", v -> v.assertThat().isEqualTo("Publicidad encubierta"));
        assertThat(get("/api/recetas/mias", tokens.usuario("autora")))
                .bodyJson()
                .hasPathSatisfying("$.contenido[0].oculta", v -> v.assertThat().isEqualTo(true));
        assertThat(get("/api/recetas/" + tortilla, tokens.admin("moderadora"))).hasStatusOk();
        assertThat(get("/api/admin/recetas/ocultas", tokens.admin("moderadora")))
                .bodyJson()
                .hasPathSatisfying("$.contenido[0].titulo", v -> v.assertThat().isEqualTo("Tortilla sospechosa"));
    }

    @Test
    void mientrasEsteOcultaSuAutorNoPuedePublicarla() {
        ocultar(tortilla, true, "Spam");

        assertThat(mvc.patch()
                        .uri("/api/recetas/" + tortilla + "/publicada")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicada\": true}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("RECETA_OCULTA"));
    }

    @Test
    void alVolverAMostrarlaLaVenTodosYSePierdeElMotivo() {
        ocultar(tortilla, true, "Spam");

        assertThat(ocultar(tortilla, false, null))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.motivoOculta", v -> v.assertThat().isNull());
        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("lectora"))).hasStatusOk();
        assertThat(get("/api/admin/recetas/ocultas", tokens.admin("moderadora")))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
    }

    @Test
    void ocultarExigeMotivoYQueLaRecetaSeaVisible() {
        assertThat(ocultar(tortilla, true, " "))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.motivo", v -> v.assertThat().isEqualTo("Indica el motivo"));
        assertThat(ocultar(tortilla, true, "x".repeat(501))).hasStatus(HttpStatus.BAD_REQUEST);
        int borrador = datos.receta("Borrador", "autora", false, 1, "Huevos");
        assertThat(ocultar(borrador, true, "Spam")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(ocultar(999_999, true, "Spam")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void soloLosAdministradoresModeran() {
        assertThat(mvc.put()
                        .uri("/api/admin/recetas/" + tortilla + "/oculta")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("lectora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oculta\": true, \"motivo\": \"x\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
        // Un token que aún dice ADMIN, de alguien que ya no lo es en la base de datos (D-043)
        assertThat(get("/api/admin/recetas/ocultas", tokens.admin("lectora"))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void unUsuarioBloqueadoPierdeSuSesionAlMomento() {
        String sesionAbierta = tokens.usuario("lectora");
        assertThat(get("/api/recetas", sesionAbierta)).hasStatusOk();

        assertThat(bloquear("lectora", true))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.bloqueado", v -> v.assertThat().isEqualTo(true));

        assertThat(get("/api/recetas", sesionAbierta))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("CUENTA_BLOQUEADA"));

        // Al desbloquearla, la sesión de antes del bloqueo no vuelve a valer: tiene que entrar otra vez (D-089)
        assertThat(bloquear("lectora", false)).hasStatusOk();
        assertThat(get("/api/recetas", sesionAbierta)).hasStatus(HttpStatus.UNAUTHORIZED);
        Integer version =
                jdbc.queryForObject("SELECT VERSION_SESION FROM usuarios WHERE ID = 'lectora'", Integer.class);
        assertThat(get("/api/recetas", tokens.usuario("lectora", version))).hasStatusOk();
    }

    @Test
    void unUsuarioBloqueadoNoPuedeIniciarSesion() {
        jdbc.update(
                "UPDATE usuarios SET PASSWORD = ?, BLOQUEADO = TRUE WHERE ID = 'lectora'",
                passwordEncoder.encode("demo1234"));

        assertThat(mvc.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"lectora\", \"password\": \"demo1234\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("CUENTA_BLOQUEADA"));
        // Con una contraseña incorrecta no se revela que está bloqueada
        assertThat(mvc.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"lectora\", \"password\": \"otra-cosa\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void niUnoMismoNiLosAdministradoresSePuedenBloquear() {
        datos.admin("otra-admin");

        assertThat(bloquear("moderadora", true))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("PROPIO_BLOQUEO"));
        assertThat(bloquear("otra-admin", true))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("ADMIN_NO_BLOQUEABLE"));
        assertThat(bloquear("nadie", true)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put()
                        .uri("/api/admin/usuarios/lectora/bloqueado")
                        .header(HttpHeaders.AUTHORIZATION, tokens.admin("moderadora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aUnUsuarioBloqueadoNoSeLeHaceAdministrador() {
        bloquear("lectora", true);

        assertThat(mvc.put()
                        .uri("/api/admin/usuarios/lectora/admin")
                        .header(HttpHeaders.AUTHORIZATION, tokens.admin("moderadora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admin\": true}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("USUARIO_BLOQUEADO"));
    }

    @Test
    void elTokenDeUnaCuentaQueYaNoExisteNoVale() {
        assertThat(get("/api/recetas", tokens.usuario("fantasma")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("NO_AUTENTICADO"));
    }

    private MvcTestResult ocultar(int receta, boolean oculta, String motivo) {
        String cuerpo = motivo == null
                ? "{\"oculta\": " + oculta + "}"
                : "{\"oculta\": " + oculta + ", \"motivo\": \"" + motivo + "\"}";
        return mvc.put()
                .uri("/api/admin/recetas/" + receta + "/oculta")
                .header(HttpHeaders.AUTHORIZATION, tokens.admin("moderadora"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .exchange();
    }

    private MvcTestResult bloquear(String usuario, boolean bloqueado) {
        return mvc.put()
                .uri("/api/admin/usuarios/" + usuario + "/bloqueado")
                .header(HttpHeaders.AUTHORIZATION, tokens.admin("moderadora"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bloqueado\": " + bloqueado + "}")
                .exchange();
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, token).exchange();
    }
}
