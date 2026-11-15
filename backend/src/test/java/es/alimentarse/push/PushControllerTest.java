package es.alimentarse.push;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.EnvioPushDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Avisos push contra MySQL real, con el envío en memoria (D-078). */
@TestIntegracion
@Transactional
class PushControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JsonMapper json;

    @Autowired
    EnvioPushDePrueba envio;

    @Autowired
    PushService push;

    /** El envío en memoria se comparte entre tests: cada uno usa sus propios endpoints. */
    private String movil;

    private String portatil;
    private int lentejas;

    @BeforeEach
    void crearFixture() {
        datos.usuario("ana");
        datos.usuario("benito");
        datos.usuario("autora");
        movil = "https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID();
        portatil = "https://wns2-par02p.notify.windows.com/w/?token=" + UUID.randomUUID();
        lentejas = datos.receta("Lentejas avisadas", "autora", true, 3, "Lentejas");
    }

    @Test
    void daLaClavePublicaParaSuscribirse() {
        assertThat(mvc.get().uri("/api/push/clave").header(HttpHeaders.AUTHORIZATION, tokens.usuario("ana")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.clavePublica", v -> v.assertThat().isEqualTo(CifradoPushTest.SERVIDOR_PUBLICA));
    }

    @Test
    void suscribirseYProbarLosAvisos() {
        assertThat(suscribir("ana", movil)).hasStatus(HttpStatus.NO_CONTENT);
        // Suscribirse otra vez desde el mismo navegador no lo duplica
        assertThat(suscribir("ana", movil)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(contar("ana")).isEqualTo(1);

        assertThat(probar("ana"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.enviados", v -> v.assertThat().isEqualTo(1));
        assertThat(envio.enviadosA(movil))
                .singleElement()
                .satisfies(aviso -> assertThat(aviso.mensaje()).contains("\"title\":\"Avisos de Toca\""));
    }

    @Test
    void enUnEquipoCompartidoLosAvisosSonDeQuienSeSuscribioElUltimo() {
        suscribir("ana", movil);
        suscribir("benito", movil);

        assertThat(contar("ana")).isZero();
        assertThat(contar("benito")).isEqualTo(1);
    }

    @Test
    void desuscribirseLoQuitaSoloDeEseNavegador() {
        suscribir("ana", movil);
        suscribir("ana", portatil);

        assertThat(mvc.delete()
                        .uri("/api/push/suscripcion?endpoint={endpoint}", movil)
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("ana")))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(contar("ana")).isEqualTo(1);
        assertThat(probar("ana")).hasStatusOk();
        assertThat(envio.enviadosA(movil)).isEmpty();
        assertThat(envio.enviadosA(portatil)).hasSize(1);
    }

    @Test
    void rechazaLosServiciosQueNoSonDeAvisosYLasClavesMalFormadas() {
        assertThat(suscribir("ana", "https://localhost:8080/api/admin/usuarios"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.endpoint", v -> v.assertThat().isEqualTo(PushService.SERVICIO_NO_ADMITIDO));
        assertThat(mvc.put()
                        .uri("/api/push/suscripcion")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("endpoint", movil, "p256dh", "", "auth", "x"))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.p256dh", v -> v.assertThat().isEqualTo("Falta la clave del navegador"));
        assertThat(contar("ana")).isZero();
    }

    @Test
    void sinNavegadoresSuscritosLaPruebaEsUnConflicto() {
        assertThat(probar("ana"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("SIN_SUSCRIPCIONES"));
    }

    @Test
    void recuerdaUnaVezLaComidaPlanificadaDentroDeLaProximaHora() {
        suscribir("ana", movil);
        datos.comidaPlanificada("ana", lentejas, Duration.ofMinutes(30));
        // Fuera de la antelación: todavía no
        datos.comidaPlanificada("ana", lentejas, Duration.ofHours(3));

        push.avisarComidas();
        push.avisarComidas();

        assertThat(envio.enviadosA(movil)).singleElement().satisfies(aviso -> {
            assertThat(aviso.mensaje()).contains("Lentejas avisadas").contains("\"url\":\"/recetas/" + lentejas + "\"");
            assertThat(aviso.validez()).isBetween(Duration.ofMinutes(28), Duration.ofMinutes(31));
        });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM usuario_receta_planificada WHERE USUARIO = 'ana' AND AVISADA IS NOT NULL",
                        Integer.class))
                .isEqualTo(1);
    }

    @Test
    void noRecuerdaLasRecetasQueYaNoEstanDisponiblesNiAUsuariosBloqueados() {
        suscribir("ana", movil);
        suscribir("benito", portatil);
        int borrador = datos.receta("Borrador ajeno", "autora", false, 1, "Lentejas");
        datos.comidaPlanificada("ana", borrador, Duration.ofMinutes(30));
        datos.comidaPlanificada("benito", lentejas, Duration.ofMinutes(30));
        jdbc.update("UPDATE usuarios SET BLOQUEADO = TRUE WHERE ID = 'benito'");

        push.avisarComidas();

        assertThat(envio.enviadosA(movil)).isEmpty();
        assertThat(envio.enviadosA(portatil)).isEmpty();
    }

    @Test
    void unaSuscripcionQueElServicioYaNoConoceSeBorra() {
        suscribir("ana", movil);
        envio.responder(movil, EnvioPush.Resultado.CADUCADA);
        datos.comidaPlanificada("ana", lentejas, Duration.ofMinutes(30));

        push.avisarComidas();

        assertThat(contar("ana")).isZero();
    }

    @Test
    void todoExigeSesion() {
        assertThat(mvc.get().uri("/api/push/clave")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/api/push/prueba")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult suscribir(String usuario, String endpoint) {
        return mvc.put()
                .uri("/api/push/suscripcion")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "endpoint",
                        endpoint,
                        "p256dh",
                        CifradoPushTest.NAVEGADOR_PUBLICA,
                        "auth",
                        CifradoPushTest.AUTH,
                        "zona",
                        "Europe/Madrid")))
                .exchange();
    }

    private MvcTestResult probar(String usuario) {
        return mvc.post()
                .uri("/api/push/prueba")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private int contar(String usuario) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM suscripcion_push WHERE USUARIO = ?", Integer.class, usuario);
    }
}
