package es.alimentarse.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

/** «¿Qué toca hoy?» (6.3) contra MySQL real. */
@TestIntegracion
@Transactional
class RecomendacionControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    private int tarta;
    private int macarrones;

    @BeforeEach
    void crearFixture() {
        datos.usuario("autor");
        datos.usuario("comensal");
        tarta = datos.receta("Tarta de chocolate", "autor", true, 5, "Chocolate blanco", "Harina");
        macarrones = datos.receta("Macarrones con tomate", "autor", true, 4, "Macarrones", "Tomate");
        datos.receta("Ensalada de lechuga y tomate", "autor", true, 6, "Lechuga", "Tomate");
        datos.receta("Sopa de verduras", "autor", true, 1, "Puerro", "Zanahoria");
        datos.receta("Borrador sin publicar", "autor", false, 1, "Lechuga");
        datos.like("autor", tarta);
        datos.like("comensal", tarta);
        datos.like("autor", macarrones);
    }

    @Test
    void sinConsumosSeRecomiendanTodasLasPublicadasPorMeGustaYFecha() {
        assertThat(recomendadas(""))
                .containsExactly(
                        "Tarta de chocolate",
                        "Macarrones con tomate",
                        // Sin likes: la más reciente primero
                        "Sopa de verduras",
                        "Ensalada de lechuga y tomate");
    }

    @Test
    void conElGrupoDulcesAgotadoNoSeRecomiendaNadaConDulces() {
        datos.consumo("comensal", tarta, Duration.ofHours(3));

        assertThat(recomendadas(""))
                .containsExactly("Macarrones con tomate", "Sopa de verduras", "Ensalada de lechuga y tomate");
    }

    @Test
    void unConsumoFueraDeLaVentanaNoAgotaElGrupo() {
        datos.consumo("comensal", tarta, Duration.ofDays(8));

        assertThat(recomendadas("")).contains("Tarta de chocolate");
    }

    @Test
    void alBorrarElConsumoElGrupoVuelveAEstarDisponible() {
        long consumo = datos.consumo("comensal", tarta, Duration.ofHours(3));
        assertThat(recomendadas("")).doesNotContain("Tarta de chocolate");

        assertThat(mvc.delete()
                        .uri("/api/consumos/" + consumo)
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(recomendadas("")).contains("Tarta de chocolate");
    }

    @Test
    void laPastaSeAgotaConSuCuartaRacionSemanal() {
        for (int dia = 1; dia <= 3; dia++) {
            datos.consumo("comensal", macarrones, Duration.ofDays(dia));
        }
        assertThat(recomendadas("")).contains("Macarrones con tomate");

        datos.consumo("comensal", macarrones, Duration.ofHours(1));

        assertThat(recomendadas("")).doesNotContain("Macarrones con tomate");
    }

    @Test
    void losFiltrosDeLaSeccion65SeCombinanConLaExclusion() {
        datos.consumo("comensal", tarta, Duration.ofHours(3));
        int tomate = datos.idIngrediente("Tomate");

        assertThat(recomendadas("ingredientes=" + tomate))
                .containsExactly("Macarrones con tomate", "Ensalada de lechuga y tomate");
        assertThat(recomendadas("titulo=CHOCOLATE")).isEmpty();
        assertThat(recomendadas("titulo=sopa")).containsExactly("Sopa de verduras");
    }

    /** D-067: los grupos poco consumidos primero; «me gusta» solo decide dentro de cada tramo. */
    @Test
    void lasRecetasDeGruposYaConsumidosVanDetrasAunqueGustenMas() {
        datos.consumo("comensal", macarrones, Duration.ofHours(3)); // Pasta 1/4

        assertThat(recomendadas(""))
                .containsExactly(
                        "Tarta de chocolate",
                        "Sopa de verduras",
                        "Ensalada de lechuga y tomate",
                        "Macarrones con tomate");
    }

    @Test
    void lasDeGruposCercaDelLimiteVanDespuesDeTodas() {
        int pollo = datos.receta("Pollo asado", "autor", true, 3, "Pollo");
        datos.consumo("comensal", pollo, Duration.ofDays(1)); // Carne 1/3: verde
        for (int dia = 1; dia <= 3; dia++) {
            datos.consumo("comensal", macarrones, Duration.ofDays(dia)); // Pasta 3/4: ámbar
        }

        assertThat(recomendadas(""))
                .containsExactly(
                        "Tarta de chocolate",
                        "Sopa de verduras",
                        "Ensalada de lechuga y tomate",
                        "Pollo asado",
                        "Macarrones con tomate");
    }

    @Test
    void cadaRecetaExplicaComoVanSusGrupos() {
        datos.consumo("comensal", macarrones, Duration.ofHours(3));

        assertThat(mvc.get()
                        .uri("/api/recetas/recomendadas?titulo=macarrones")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .bodyJson()
                // Los datos de la tarjeta siguen en el primer nivel
                .hasPathSatisfying("$.contenido[0].titulo", v -> v.assertThat().isEqualTo("Macarrones con tomate"))
                .doesNotHavePath("$.contenido[0].receta")
                .hasPathSatisfying(
                        "$.contenido[0].grupos[0].grupo.nombre",
                        v -> v.assertThat().isEqualTo("Pasta"))
                .hasPathSatisfying(
                        "$.contenido[0].grupos[0].consumidas",
                        v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying(
                        "$.contenido[0].grupos[1].grupo.nombre",
                        v -> v.assertThat().isEqualTo("Hortalizas"));
    }

    @Test
    void elImpactoDiceComoQuedariaCadaGrupoConLimite() {
        for (int dia = 1; dia <= 3; dia++) {
            datos.consumo("comensal", macarrones, Duration.ofDays(dia));
        }

        assertThat(mvc.get()
                        .uri("/api/recetas/" + macarrones + "/impacto")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .bodyJson()
                .hasPathSatisfying("$", v -> v.assertThat().asArray().hasSize(1))
                .hasPathSatisfying("$[0].antes.consumidas", v -> v.assertThat().isEqualTo(3))
                .hasPathSatisfying(
                        "$[0].despues.consumidas", v -> v.assertThat().isEqualTo(4))
                .hasPathSatisfying("$[0].despues.color", v -> v.assertThat().isEqualTo("ROJO"));
    }

    @Test
    void elImpactoRespetaLaVisibilidadYExigeSesion() {
        int borrador = datos.receta("Borrador ajeno", "autor", false, 1, "Pollo");

        assertThat(mvc.get()
                        .uri("/api/recetas/" + borrador + "/impacto")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/recetas/" + macarrones + "/impacto")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void laRecomendacionEsPaginadaYExigeSesion() {
        assertThat(mvc.get()
                        .uri("/api/recetas/recomendadas?size=2&page=1")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .bodyJson()
                .hasPathSatisfying("$.contenido", v -> v.assertThat().asArray().hasSize(2))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(4));
        assertThat(mvc.get().uri("/api/recetas/recomendadas")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private List<String> recomendadas(String consulta) {
        try {
            String cuerpo = mvc.get()
                    .uri("/api/recetas/recomendadas?" + consulta)
                    .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal"))
                    .exchange()
                    .getResponse()
                    .getContentAsString();
            List<String> titulos = JsonPath.read(cuerpo, "$.contenido[*].titulo");
            return new ArrayList<>(titulos);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
