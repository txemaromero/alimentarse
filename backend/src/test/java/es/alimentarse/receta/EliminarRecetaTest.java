package es.alimentarse.receta;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
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

/** Eliminar una receta propia (D-061), contra MySQL real. */
@TestIntegracion
@Transactional
class EliminarRecetaTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    TokensDePrueba tokens;

    private int tortilla;

    @BeforeEach
    void crearFixture() {
        datos.usuario("autora");
        datos.usuario("comensal");
        tortilla = datos.receta("Tortilla de patata", "autora", true, 3, "Huevos", "Patata");
        datos.like("comensal", tortilla);
    }

    @Test
    void unaRecetaQueNadieHaConsumidoSeBorraDelTodo() {
        assertThat(eliminar(tortilla, "autora")).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(contar("SELECT COUNT(*) FROM recetas WHERE ID = ?")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM ingrediente_receta WHERE RECETA = ?"))
                .isZero();
        assertThat(contar("SELECT COUNT(*) FROM like_receta WHERE RECETA = ?")).isZero();
        assertThat(get("/api/recetas/" + tortilla, "autora")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void unaRecetaConsumidaDesapareceParaTodosPeroSusConsumosSiguenContando() {
        datos.consumo("comensal", tortilla, Duration.ofDays(1));

        assertThat(eliminar(tortilla, "autora")).hasStatus(HttpStatus.NO_CONTENT);

        Map<String, Object> fila =
                jdbc.queryForMap("SELECT ELIMINADA, PUBLICADA, IMAGEN, TITULO FROM recetas WHERE ID = ?", tortilla);
        assertThat(fila)
                .containsEntry("ELIMINADA", true)
                .containsEntry("PUBLICADA", false)
                .containsEntry("TITULO", "Tortilla de patata");
        assertThat(fila.get("IMAGEN")).isNull();
        assertThat(contar("SELECT COUNT(*) FROM like_receta WHERE RECETA = ?")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM ingrediente_receta WHERE RECETA = ?"))
                .isEqualTo(2);

        // Nadie la ve: ni el detalle, ni los listados, ni «Mis recetas»
        assertThat(get("/api/recetas/" + tortilla, "autora")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get("/api/recetas/" + tortilla, "comensal")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get("/api/recetas", "comensal"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
        assertThat(get("/api/recetas/mias", "autora"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));

        // El histórico la sigue mostrando, marcada, y cuenta en los grupos de los últimos 7 días
        assertThat(get("/api/consumos", "comensal"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[0].receta.titulo", v -> v.assertThat().isEqualTo("Tortilla de patata"))
                .hasPathSatisfying(
                        "$.contenido[0].receta.eliminada", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying(
                        "$.contenido[0].receta.imagen", v -> v.assertThat().isNull());
        assertThat(get("/api/alimentacion/resumen", "comensal"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.grupos7Dias[?(@.grupo.nombre == 'Huevos')].consumidas",
                        v -> v.assertThat().asArray().containsExactly(1));

        // Ni se puede volver a consumir ni darle «me gusta»
        assertThat(mvc.post()
                        .uri("/api/consumos")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receta\": " + tortilla + "}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.post()
                        .uri("/api/recetas/" + tortilla + "/like")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(eliminar(tortilla, "autora")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void soloElAutorPuedeEliminarla() {
        assertThat(eliminar(tortilla, "comensal")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(contar("SELECT COUNT(*) FROM recetas WHERE ID = ?")).isEqualTo(1);
        assertThat(eliminar(999_999, "autora")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/recetas/" + tortilla)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unBorradorAjenoDa404ParaNoRevelarQueExiste() {
        int borrador = datos.receta("Borrador", "autora", false, 1, "Huevos");

        assertThat(eliminar(borrador, "comensal")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(eliminar(borrador, "autora")).hasStatus(HttpStatus.NO_CONTENT);
    }

    private MvcTestResult eliminar(int id, String usuario) {
        return mvc.delete()
                .uri("/api/recetas/" + id)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private MvcTestResult get(String uri, String usuario) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class, tortilla);
    }
}
