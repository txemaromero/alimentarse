package es.alimentarse.consumo;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@TestIntegracion
@Transactional
class ConsumoControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    private int tortilla;
    private int ajenaSinPublicar;
    private int propiaSinPublicar;

    @BeforeEach
    void crearFixture() {
        datos.usuario("comensal");
        datos.usuario("otra");
        tortilla = datos.receta("Tortilla", "otra", true, 3, "Huevos", "Patata");
        ajenaSinPublicar = datos.receta("Borrador ajeno", "otra", false, 2, "Huevos");
        propiaSinPublicar = datos.receta("Borrador propio", "comensal", false, 1, "Patata");
    }

    @Test
    void sinFechaSeRegistraAhoraConLaTarjetaDeLaReceta() throws Exception {
        Instant antes = Instant.now().minusSeconds(2);

        MvcTestResult creado = registrar("comensal", tortilla, null);

        assertThat(creado)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.receta.titulo", v -> v.assertThat().isEqualTo("Tortilla"))
                .hasPathSatisfying("$.receta.autor", v -> v.assertThat().isEqualTo("otra"));
        String cuerpo = creado.getResponse().getContentAsString();
        Instant fecha = Instant.parse(JsonPath.read(cuerpo, "$.fecha"));
        assertThat(fecha).isBetween(antes, Instant.now().plusSeconds(1));
        assertThat(creado.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("/api/consumos/" + JsonPath.read(cuerpo, "$.id"));
    }

    /** Como mucho 50 consumos registrados al día por usuario (D-090). */
    @Test
    void cadaUsuarioRegistraComoMucho50ConsumosAlDia() {
        for (int i = 1; i <= ConsumoService.AL_DIA; i++) {
            assertThat(registrar("comensal", tortilla, null)).hasStatus(HttpStatus.CREATED);
        }

        MvcTestResult otro = registrar("comensal", tortilla, null);
        assertThat(otro)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("LIMITE_CONSUMOS"));
        assertThat(otro.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
        // El tope es de cada usuario
        assertThat(registrar("otra", tortilla, null)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void conFechaPasadaSeGuardaEsaFecha() {
        assertThat(registrar("comensal", tortilla, "2026-11-01T10:15:30Z"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.fecha", v -> v.assertThat().isEqualTo("2026-11-01T10:15:30Z"));
    }

    @Test
    void unaFechaFuturaSeRechaza() {
        assertThat(registrar(
                        "comensal",
                        tortilla,
                        Instant.now().plus(Duration.ofDays(1)).toString()))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.fecha", v -> v.assertThat().isEqualTo(ConsumoService.FECHA_FUTURA));
    }

    @Test
    void soloSePuedenConsumirRecetasVisibles() {
        assertThat(registrar("comensal", ajenaSinPublicar, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(registrar("comensal", 999_999, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(registrar("comensal", propiaSinPublicar, null)).hasStatus(HttpStatus.CREATED);
        assertThat(registrar("comensal", null, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.receta", v -> v.assertThat().isEqualTo("Elige la receta"));
    }

    @Test
    void elHistorialEsDelMasRecienteAlMasAntiguoYSoloDelUsuario() {
        datos.consumo("comensal", tortilla, Duration.ofDays(3));
        datos.consumo("comensal", propiaSinPublicar, Duration.ofHours(1));
        datos.consumo("comensal", tortilla, Duration.ofDays(1));
        datos.consumo("otra", tortilla, Duration.ofMinutes(5));

        assertThat(get("/api/consumos?size=2", "comensal"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].receta.titulo",
                        v -> v.assertThat().asArray().containsExactly("Borrador propio", "Tortilla"))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(3))
                .hasPathSatisfying("$.totalPaginas", v -> v.assertThat().isEqualTo(2));
    }

    @Test
    void sePuedeCambiarLaFechaDeUnConsumoPropio() {
        long consumo = datos.consumo("comensal", tortilla, Duration.ofDays(1));

        assertThat(cambiarFecha("comensal", consumo, "2026-11-02T20:30:00Z"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.fecha", v -> v.assertThat().isEqualTo("2026-11-02T20:30:00Z"));
        assertThat(cambiarFecha(
                        "comensal",
                        consumo,
                        Instant.now().plus(Duration.ofHours(2)).toString()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sePuedeBorrarUnConsumoPropioYDesapareceDelHistorial() {
        long consumo = datos.consumo("comensal", tortilla, Duration.ofDays(1));

        assertThat(mvc.delete()
                        .uri("/api/consumos/" + consumo)
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(get("/api/consumos", "comensal"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
    }

    @Test
    void losConsumosAjenosNoExisten() {
        long ajeno = datos.consumo("otra", tortilla, Duration.ofDays(1));

        assertThat(cambiarFecha("comensal", ajeno, "2026-11-02T20:30:00Z")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete()
                        .uri("/api/consumos/" + ajeno)
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void losConsumosExigenSesion() {
        assertThat(mvc.get().uri("/api/consumos")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult registrar(String usuario, Integer receta, String fecha) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("receta", receta);
        cuerpo.put("fecha", fecha);
        return mvc.post()
                .uri("/api/consumos")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))
                .exchange();
    }

    private MvcTestResult cambiarFecha(String usuario, long consumo, String fecha) {
        return mvc.put()
                .uri("/api/consumos/" + consumo)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("fecha", fecha)))
                .exchange();
    }

    private MvcTestResult get(String uri, String usuario) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }
}
