package es.alimentarse.receta;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

/** Valoraciones de las recetas contra MySQL real (D-077). */
@TestIntegracion
@Transactional
class ValoracionControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    JsonMapper json;

    private int tortilla;
    private int borrador;

    @BeforeEach
    void crearFixture() {
        datos.usuario("autora");
        datos.usuario("ana");
        datos.usuario("benito");
        datos.admin("moderadora");
        tortilla = datos.receta("Tortilla valorada", "autora", true, 3, "Huevos", "Patata");
        borrador = datos.receta("Borrador sin publicar", "autora", false, 1, "Huevos");
    }

    @Test
    void cadaUnoValoraUnaVezYLaMediaSeVeEnElDetalleYEnLasTarjetas() {
        assertThat(valorar("ana", tortilla, 4, "Muy jugosa"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.nota", v -> v.assertThat().isEqualTo(4.0))
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.mia.comentario", v -> v.assertThat().isEqualTo("Muy jugosa"));
        assertThat(valorar("benito", tortilla, 5, null))
                .bodyJson()
                .hasPathSatisfying("$.nota", v -> v.assertThat().isEqualTo(4.5))
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(2));

        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("ana")))
                .bodyJson()
                .hasPathSatisfying("$.nota", v -> v.assertThat().isEqualTo(4.5))
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(2))
                .hasPathSatisfying("$.miValoracion.nota", v -> v.assertThat().isEqualTo(4));
        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("autora")))
                .bodyJson()
                .hasPathSatisfying("$.miValoracion", v -> v.assertThat().isNull());
        assertThat(get("/api/recetas?titulo=Tortilla valorada", tokens.usuario("autora")))
                .bodyJson()
                .hasPathSatisfying("$.contenido[0].nota", v -> v.assertThat().isEqualTo(4.5))
                .hasPathSatisfying(
                        "$.contenido[0].valoraciones", v -> v.assertThat().isEqualTo(2));
    }

    @Test
    void laListaVaDeLaMasRecienteALaMasAntiguaYPorPaginas() {
        valorar("ana", tortilla, 4, "Primera");
        valorar("benito", tortilla, 2, "Segunda");

        assertThat(get("/api/recetas/" + tortilla + "/valoraciones?size=1", tokens.usuario("autora")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.contenido[0].usuario", v -> v.assertThat().isEqualTo("benito"))
                .hasPathSatisfying(
                        "$.contenido[0].comentario", v -> v.assertThat().isEqualTo("Segunda"))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(2))
                .hasPathSatisfying("$.totalPaginas", v -> v.assertThat().isEqualTo(2));
    }

    @Test
    void cambiarLaPropiaLaSustituyeYBorrarlaLaQuita() {
        valorar("ana", tortilla, 4, "Muy jugosa");

        assertThat(valorar("ana", tortilla, 2, "  "))
                .bodyJson()
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.nota", v -> v.assertThat().isEqualTo(2.0))
                .hasPathSatisfying("$.mia.comentario", v -> v.assertThat().isNull());

        assertThat(borrar("ana", tortilla))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.nota", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(0))
                .hasPathSatisfying("$.mia", v -> v.assertThat().isNull());
        assertThat(borrar("ana", tortilla)).hasStatusOk();
    }

    @Test
    void nadieValoraSuPropiaRecetaNiUnaQueNoVe() {
        assertThat(valorar("autora", tortilla, 5, null))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("PROPIA_RECETA"));
        assertThat(valorar("ana", borrador, 5, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get("/api/recetas/" + borrador + "/valoraciones", tokens.usuario("ana")))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void laNotaVaDe1A5YElComentarioTieneUnLimite() {
        assertThat(valorar("ana", tortilla, 6, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.nota", v -> v.assertThat().isEqualTo("La nota va de 1 a 5"));
        assertThat(valorar("ana", tortilla, 0, null)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(valorar("ana", tortilla, null, null))
                .bodyJson()
                .hasPathSatisfying("$.errores.nota", v -> v.assertThat().isEqualTo("Elige una nota"));
        assertThat(valorar("ana", tortilla, 3, "x".repeat(1001)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.comentario",
                        v -> v.assertThat().isEqualTo("El comentario no puede superar los 1000 caracteres"));
    }

    @Test
    void unaValoracionOcultaNoSeVeNiCuentaHastaQueSeVuelveAMostrar() {
        valorar("ana", tortilla, 4, "Muy jugosa");
        long spam = id(valorar("benito", tortilla, 1, "Visita mi web"), "$.mia.id");

        assertThat(ocultar("moderadora", spam, true))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.oculta", v -> v.assertThat().isEqualTo(true));

        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("ana")))
                .bodyJson()
                .hasPathSatisfying("$.nota", v -> v.assertThat().isEqualTo(4.0))
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(1));
        assertThat(get("/api/recetas/" + tortilla + "/valoraciones", tokens.usuario("ana")))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].usuario", v -> v.assertThat().asArray().containsExactly("ana"));
        // La administración las sigue viendo, marcadas, y su autor ve la suya como oculta
        assertThat(get("/api/recetas/" + tortilla + "/valoraciones", tokens.admin("moderadora")))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[?(@.usuario == 'benito')].oculta",
                        v -> v.assertThat().asArray().containsExactly(true));
        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("benito")))
                .bodyJson()
                .hasPathSatisfying("$.miValoracion.oculta", v -> v.assertThat().isEqualTo(true));
        // Cambiarla no la vuelve a mostrar
        assertThat(valorar("benito", tortilla, 2, "Otra vez"))
                .bodyJson()
                .hasPathSatisfying("$.mia.oculta", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(1));

        assertThat(ocultar("moderadora", spam, false)).hasStatusOk();
        assertThat(get("/api/recetas/" + tortilla, tokens.usuario("ana")))
                .bodyJson()
                .hasPathSatisfying("$.valoraciones", v -> v.assertThat().isEqualTo(2));
    }

    @Test
    void soloLaAdministracionOcultaValoraciones() {
        long una = id(valorar("ana", tortilla, 4, null), "$.mia.id");

        assertThat(ocultar("benito", una, true)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(ocultar("moderadora", 999_999, true)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put()
                        .uri("/api/admin/valoraciones/" + una + "/oculta")
                        .header(HttpHeaders.AUTHORIZATION, tokens.admin("moderadora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void lasMejorValoradasVanPorLaMediaPonderada() {
        int unaSolaDeCinco = datos.receta("Con una sola de cinco", "autora", true, 2, "Huevos");
        int variasDeCuatro = datos.receta("Con varias de cuatro", "autora", true, 2, "Huevos");
        datos.usuario("carla");
        valorar("ana", unaSolaDeCinco, 5, null);
        valorar("ana", variasDeCuatro, 4, null);
        valorar("benito", variasDeCuatro, 4, null);
        valorar("carla", variasDeCuatro, 5, null);

        List<String> titulos = JsonPath.read(
                contenido(get("/api/recetas/mejor-valoradas?limit=20", tokens.usuario("ana"))), "$[*].titulo");
        assertThat(titulos.indexOf("Con varias de cuatro")).isLessThan(titulos.indexOf("Con una sola de cinco"));
        // Sin valoraciones, por debajo de las bien valoradas
        assertThat(titulos.indexOf("Con una sola de cinco")).isLessThan(titulos.indexOf("Tortilla valorada"));
    }

    @Test
    void todoExigeSesion() {
        assertThat(mvc.get().uri("/api/recetas/" + tortilla + "/valoraciones").exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla + "/valoracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\": 4}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult valorar(String usuario, int receta, Integer nota, String comentario) {
        Map<String, Object> cuerpo = new java.util.HashMap<>();
        cuerpo.put("nota", nota);
        cuerpo.put("comentario", comentario);
        return mvc.put()
                .uri("/api/recetas/" + receta + "/valoracion")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))
                .exchange();
    }

    private MvcTestResult borrar(String usuario, int receta) {
        return mvc.delete()
                .uri("/api/recetas/" + receta + "/valoracion")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private MvcTestResult ocultar(String admin, long id, boolean oculta) {
        return mvc.put()
                .uri("/api/admin/valoraciones/" + id + "/oculta")
                .header(HttpHeaders.AUTHORIZATION, tokens.admin(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oculta\": " + oculta + "}")
                .exchange();
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, token).exchange();
    }

    private static String contenido(MvcTestResult respuesta) {
        return new String(respuesta.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static long id(MvcTestResult respuesta, String ruta) {
        return ((Number) JsonPath.read(contenido(respuesta), ruta)).longValue();
    }
}
