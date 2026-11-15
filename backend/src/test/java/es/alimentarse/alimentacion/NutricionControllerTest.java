package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Estudio nutricional con la composición de BEDCA de la migración V13 (D-079), contra MySQL real. */
@TestIntegracion
@Transactional
class NutricionControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    JdbcTemplate jdbc;

    private int bizcocho;

    @BeforeEach
    void crearFixture() {
        datos.usuario("autora");
        datos.usuario("ana");
        datos.admin("moderadora");
        // Dos comensales
        bizcocho = datos.receta("Bizcocho nutritivo", "autora", true, 1, "Harina", "Huevos", "Sal", "Levadura");
        cantidad(bizcocho, "Harina", "200 gramos de harina");
        cantidad(bizcocho, "Huevos", "2");
        cantidad(bizcocho, "Sal", "1 pizca");
        cantidad(bizcocho, "Levadura", "1 sobre");
    }

    @Test
    void estimaLaRecetaConLosIngredientesQueSePuedenContar() {
        assertThat(get(bizcocho, tokens.usuario("ana")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.comensales", v -> v.assertThat().isEqualTo(2))
                // 200 g de harina de trigo + 2 huevos de 50 g
                .hasPathSatisfying("$.total.energiaKcal", v -> v.assertThat().isEqualTo(811))
                .hasPathSatisfying("$.total.energiaKj", v -> v.assertThat().isEqualTo(3394))
                .hasPathSatisfying("$.total.proteinas", v -> v.assertThat().isEqualTo(32.5))
                .hasPathSatisfying("$.total.sal", v -> v.assertThat().isEqualTo(0.37))
                .hasPathSatisfying(
                        "$.porRacion.energiaKcal", v -> v.assertThat().isEqualTo(406))
                .hasPathSatisfying("$.porRacion.hidratos", v -> v.assertThat().isEqualTo(71.5))
                .hasPathSatisfying(
                        "$.ingredientes[*].nombre",
                        v -> v.assertThat().asInstanceOf(LIST).containsExactly("Harina", "Huevos", "Levadura", "Sal"))
                .hasPathSatisfying(
                        "$.ingredientes[*].estado",
                        v -> v.assertThat()
                                .asInstanceOf(LIST)
                                .containsExactly("CALCULADO", "CALCULADO", "SIN_DATOS", "SIN_PESO"))
                .hasPathSatisfying(
                        "$.ingredientes[0].alimento", v -> v.assertThat().isEqualTo("Harina de trigo"))
                .hasPathSatisfying(
                        "$.ingredientes[0].cantidad", v -> v.assertThat().isEqualTo("200 gramos de harina"))
                .hasPathSatisfying(
                        "$.ingredientes[1].gramos", v -> v.assertThat().isEqualTo(100.0))
                .hasPathSatisfying(
                        "$.ingredientes[2].alimento", v -> v.assertThat().isNull())
                .hasPathSatisfying(
                        "$.ingredientes[3].gramos", v -> v.assertThat().isNull());
    }

    @Test
    void sinIngredientesQueCuentenNoHayTotales() {
        int receta = datos.receta("Solo levadura", "autora", true, 1, "Levadura");

        assertThat(get(receta, tokens.usuario("ana")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.porRacion", v -> v.assertThat().isNull())
                .hasPathSatisfying(
                        "$.ingredientes[0].estado", v -> v.assertThat().isEqualTo("SIN_DATOS"));
    }

    @Test
    void soloSiSePuedeVerLaReceta() {
        int borrador = datos.receta("Borrador nutritivo", "autora", false, 1, "Harina");
        jdbc.update("UPDATE recetas SET OCULTA = TRUE, MOTIVO_OCULTA = 'Prueba' WHERE ID = ?", bizcocho);

        assertThat(get(borrador, tokens.usuario("ana"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(borrador, tokens.usuario("autora"))).hasStatusOk();
        assertThat(get(bizcocho, tokens.usuario("ana"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(bizcocho, tokens.admin("moderadora"))).hasStatusOk();
        assertThat(get(999_999, tokens.usuario("ana"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/recetas/" + bizcocho + "/nutricion").exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void losNutrientesDeLaSemanaSumanUnaRacionPorConsumoDeLosUltimos7Dias() {
        datos.consumo("ana", bizcocho, Duration.ofDays(2));
        datos.consumo("ana", bizcocho, Duration.ofHours(3));
        // Fuera de la ventana: no cuenta
        datos.consumo("ana", bizcocho, Duration.ofDays(8));
        int sinDatos = datos.receta("Solo levadura", "autora", true, 1, "Levadura");
        datos.consumo("ana", sinDatos, Duration.ofDays(1));

        assertThat(mvc.get()
                        .uri("/api/alimentacion/nutricion")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("ana"))
                        .exchange())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.consumos", v -> v.assertThat().isEqualTo(3))
                .hasPathSatisfying("$.estimados", v -> v.assertThat().isEqualTo(2))
                // Dos raciones del bizcocho para dos: el bizcocho entero
                .hasPathSatisfying("$.total.energiaKcal", v -> v.assertThat().isEqualTo(811))
                .hasPathSatisfying(
                        "$.mediaDiaria.energiaKcal", v -> v.assertThat().isEqualTo(116))
                .hasPathSatisfying("$.referencia.energia", v -> v.assertThat().isEqualTo(6));

        assertThat(mvc.get()
                        .uri("/api/alimentacion/nutricion")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora"))
                        .exchange())
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.consumos", v -> v.assertThat().isEqualTo(0))
                .hasPathSatisfying("$.total", v -> v.assertThat().isNull());
        assertThat(mvc.get().uri("/api/alimentacion/nutricion").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void cantidad(int receta, String ingrediente, String cantidad) {
        jdbc.update(
                "UPDATE ingrediente_receta SET CANTIDAD = ? WHERE RECETA = ? AND INGREDIENTE = ?",
                cantidad,
                receta,
                datos.idIngrediente(ingrediente));
    }

    private MvcTestResult get(int receta, String token) {
        return mvc.get()
                .uri("/api/recetas/" + receta + "/nutricion")
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange();
    }
}
