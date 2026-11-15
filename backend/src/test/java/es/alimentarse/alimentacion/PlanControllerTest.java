package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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

/** Planificador semanal contra MySQL real (D-068). */
@TestIntegracion
@Transactional
class PlanControllerTest {

    private static final Instant HOY = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private static final Instant MANANA = HOY.plus(Duration.ofDays(1));
    private static final Instant PASADO = HOY.plus(Duration.ofDays(2));

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

    private int pollo;
    private int borrador;

    @BeforeEach
    void crearFixture() {
        datos.usuario("autor");
        datos.usuario("planificadora");
        datos.usuario("otra");
        pollo = datos.receta("Pollo asado", "autor", true, 3, "Pollo", "Patata");
        borrador = datos.receta("Borrador ajeno", "autor", false, 1, "Pollo");
    }

    @Test
    void lasComidasSeAnadenYCuentanEnLosIndicadores() {
        assertThat(anadir("planificadora", pollo, MANANA))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.receta.titulo", v -> v.assertThat().isEqualTo("Pollo asado"))
                .hasPathSatisfying("$.disponible", v -> v.assertThat().isEqualTo(true));

        assertThat(plan("planificadora"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.comidas[0].fecha", v -> v.assertThat().isEqualTo(MANANA.toString()))
                .hasPathSatisfying(
                        "$.comidas[0].excesos", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying(
                        "$.indicadores[0].grupo.nombre", v -> v.assertThat().isEqualTo("Carne"))
                .hasPathSatisfying(
                        "$.indicadores[0].consumidas", v -> v.assertThat().isEqualTo(1));
    }

    @Test
    void cadaUsuarioVeSoloSuPlan() {
        anadir("planificadora", pollo, MANANA);

        assertThat(plan("otra"))
                .bodyJson()
                .hasPathSatisfying("$.comidas", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying(
                        "$.indicadores", v -> v.assertThat().asArray().isEmpty());
    }

    @Test
    void avisaDelExcesoAlPlanificarDemasiado() {
        datos.consumo("planificadora", pollo, Duration.ofDays(1));
        datos.consumo("planificadora", pollo, Duration.ofDays(2));
        datos.consumo("planificadora", pollo, Duration.ofDays(3));

        assertThat(anadir("planificadora", pollo, MANANA))
                .bodyJson()
                .hasPathSatisfying(
                        "$.excesos[0].grupo.nombre", v -> v.assertThat().isEqualTo("Carne"))
                .hasPathSatisfying(
                        "$.excesos[0].consumidas", v -> v.assertThat().isEqualTo(4));
    }

    @Test
    void unHuecoSoloAdmiteUnaReceta() {
        anadir("planificadora", pollo, MANANA);

        assertThat(anadir("planificadora", pollo, MANANA))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("HUECO_OCUPADO"));
        assertThat(anadir("otra", pollo, MANANA)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void validaLaRecetaYLaFecha() {
        assertThat(anadir("planificadora", pollo, HOY.minus(Duration.ofHours(2))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.fecha", v -> v.assertThat().isEqualTo(PlanService.FECHA_PASADA));
        assertThat(anadir("planificadora", borrador, MANANA)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(anadir("planificadora", 999_999, MANANA)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void elIntervaloEsObligatorioYValido() {
        String sesion = tokens.usuario("planificadora");
        assertThat(mvc.get().uri("/api/plan").header(HttpHeaders.AUTHORIZATION, sesion))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/api/plan?desde=ayer&hasta=hoy").header(HttpHeaders.AUTHORIZATION, sesion))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get()
                        .uri("/api/plan?desde=" + PASADO + "&hasta=" + MANANA)
                        .header(HttpHeaders.AUTHORIZATION, sesion))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void borrarQuitaLaComidaSoloASuDueno() {
        long id = idDe(anadir("planificadora", pollo, MANANA));

        assertThat(borrar("otra", id)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(contarPlan("planificadora")).isEqualTo(1);

        assertThat(borrar("planificadora", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(contarPlan("planificadora")).isZero();
    }

    @Test
    void consumirLaPasaALosConsumosYLaQuitaDelPlan() {
        long id = idDe(anadir("planificadora", pollo, MANANA));

        assertThat(consumir("otra", id)).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(consumir("planificadora", id))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.receta.titulo", v -> v.assertThat().isEqualTo("Pollo asado"));
        assertThat(contarPlan("planificadora")).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM usuario_receta_consumida WHERE USUARIO = 'planificadora'", Long.class))
                .isEqualTo(1);
    }

    @Test
    void siElAutorLaDespublicaSigueEnElPlanPeroNoCuenta() {
        anadir("planificadora", pollo, MANANA);
        assertThat(mvc.patch()
                        .uri("/api/recetas/" + pollo + "/publicada")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicada\": false}"))
                .hasStatusOk();

        assertThat(plan("planificadora"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.comidas[0].disponible", v -> v.assertThat().isEqualTo(false))
                .hasPathSatisfying(
                        "$.indicadores", v -> v.assertThat().asArray().isEmpty());
    }

    @Test
    void alBorrarDelTodoLaRecetaDesapareceDeLosPlanes() {
        anadir("planificadora", pollo, MANANA);

        assertThat(mvc.delete().uri("/api/recetas/" + pollo).header(HttpHeaders.AUTHORIZATION, tokens.usuario("autor")))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(contarPlan("planificadora")).isZero();
    }

    @Test
    void todoExigeSesion() {
        assertThat(mvc.get().uri("/api/plan?desde=" + HOY + "&hasta=" + MANANA)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.delete().uri("/api/plan/1")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void laListaDeLaCompraReuneLosIngredientesDeLasComidasPlanificadas() {
        jdbc.update("UPDATE ingrediente_receta SET CANTIDAD = '1 unidad' WHERE RECETA = ?", pollo);
        datos.comidaPlanificada("planificadora", pollo, Duration.ofDays(1));
        datos.comidaPlanificada("planificadora", pollo, Duration.ofDays(2));
        // Lo de otra persona no cuenta
        datos.comidaPlanificada("otra", pollo, Duration.ofDays(1));

        assertThat(compra("planificadora", HOY, HOY.plus(Duration.ofDays(7))))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.comidas.length()", v -> v.assertThat().isEqualTo(2))
                .hasPathSatisfying("$.comidas[0].titulo", v -> v.assertThat().isEqualTo("Pollo asado"))
                .hasPathSatisfying("$.noDisponibles", v -> v.assertThat().isEqualTo(0))
                .hasPathSatisfying(
                        "$.secciones[*].grupo.nombre",
                        v -> v.assertThat().asArray().containsExactly("Carne", "Hortalizas"))
                .hasPathSatisfying(
                        "$.secciones[0].articulos[0].nombre",
                        v -> v.assertThat().isEqualTo("Pollo"))
                .hasPathSatisfying(
                        "$.secciones[0].articulos[0].total", v -> v.assertThat().isEqualTo("2 unidades"))
                .hasPathSatisfying(
                        "$.secciones[0].articulos[0].cantidades[0].veces",
                        v -> v.assertThat().isEqualTo(2));
    }

    @Test
    void laListaDeLaCompraNoLlevaLasRecetasQueYaNoSePuedenVer() {
        datos.comidaPlanificada("planificadora", pollo, Duration.ofDays(1));
        jdbc.update("UPDATE recetas SET PUBLICADA = FALSE WHERE ID = ?", pollo);

        assertThat(compra("planificadora", HOY, HOY.plus(Duration.ofDays(7))))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.comidas", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying("$.secciones", v -> v.assertThat().asArray().isEmpty())
                .hasPathSatisfying("$.noDisponibles", v -> v.assertThat().isEqualTo(1));
    }

    @Test
    void laListaDeLaCompraExigeSesionYUnIntervaloValido() {
        assertThat(mvc.get()
                        .uri("/api/plan/compra?desde=" + HOY + "&hasta=" + MANANA)
                        .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(compra("planificadora", MANANA, HOY)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(compra("planificadora", HOY, HOY.plus(Duration.ofDays(40)))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MvcTestResult plan(String usuario) {
        return mvc.get()
                .uri("/api/plan?desde=" + HOY + "&hasta=" + HOY.plus(Duration.ofDays(7)))
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private MvcTestResult compra(String usuario, Instant desde, Instant hasta) {
        return mvc.get()
                .uri("/api/plan/compra?desde=" + desde + "&hasta=" + hasta)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private MvcTestResult anadir(String usuario, int receta, Instant fecha) {
        return mvc.post()
                .uri("/api/plan")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("receta", receta, "fecha", fecha.toString())))
                .exchange();
    }

    private MvcTestResult borrar(String usuario, long id) {
        return mvc.delete()
                .uri("/api/plan/" + id)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private MvcTestResult consumir(String usuario, long id) {
        return mvc.post()
                .uri("/api/plan/" + id + "/consumir")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private long contarPlan(String usuario) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario_receta_planificada WHERE USUARIO = ?", Long.class, usuario);
    }

    private static long idDe(MvcTestResult respuesta) {
        try {
            Number id = JsonPath.read(respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
            return id.longValue();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
