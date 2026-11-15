package es.alimentarse.receta;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

/** «Recetas que me gustan» (D-062), contra MySQL real. */
@TestIntegracion
@Transactional
class MeGustanTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    TokensDePrueba tokens;

    private int tortilla;
    private int lentejas;
    private int batido;

    @BeforeEach
    void crearFixture() {
        datos.usuario("autora");
        datos.usuario("fan");
        tortilla = datos.receta("Tortilla de patata", "autora", true, 9, "Huevos", "Patata");
        lentejas = datos.receta("Lentejas estofadas", "autora", true, 8, "Lentejas");
        batido = datos.receta("Batido de fresa", "autora", true, 7, "Fresa");
        datos.receta("Sin me gusta", "autora", true, 1, "Huevos");
        // La fecha del «me gusta», no la de la receta, decide el orden
        datos.like("fan", lentejas, Duration.ofDays(3));
        datos.like("fan", tortilla, Duration.ofDays(1));
        datos.like("fan", batido, Duration.ofDays(2));
        datos.like("autora", batido, Duration.ofDays(5));
    }

    @Test
    void sonLasQueMeGustanDeLaMasRecienteALaMasAntigua() {
        assertThat(titulos("fan")).containsExactly("Tortilla de patata", "Batido de fresa", "Lentejas estofadas");
        assertThat(titulos("autora")).containsExactly("Batido de fresa");
        assertThat(mvc.get().uri("/api/recetas/me-gustan").header(HttpHeaders.AUTHORIZATION, tokens.usuario("fan")))
                .bodyJson()
                .hasPathSatisfying("$.contenido[0].meGusta", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(3));
    }

    @Test
    void unMeGustaNuevoVaElPrimero() {
        assertThat(mvc.delete()
                        .uri("/api/recetas/" + lentejas + "/like")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("fan")))
                .hasStatusOk();
        assertThat(mvc.post()
                        .uri("/api/recetas/" + lentejas + "/like")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("fan")))
                .hasStatusOk();

        assertThat(titulos("fan")).containsExactly("Lentejas estofadas", "Tortilla de patata", "Batido de fresa");
    }

    @Test
    void lasQueDejanDeEstarPublicadasNoSalen() {
        jdbc.update("UPDATE recetas SET PUBLICADA = FALSE WHERE ID = ?", tortilla);

        assertThat(titulos("fan")).containsExactly("Batido de fresa", "Lentejas estofadas");
    }

    @Test
    void admiteLosFiltrosDelListado() {
        assertThat(titulos("fan", "titulo", "FRESA")).containsExactly("Batido de fresa");
        assertThat(titulos("fan", "ingredientes", String.valueOf(datos.idIngrediente("Huevos"))))
                .containsExactly("Tortilla de patata");
        assertThat(mvc.get()
                        .uri("/api/recetas/me-gustan")
                        .param("comensales", "0")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("fan")))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private List<String> titulos(String usuario, String... parametros) {
        var peticion =
                mvc.get().uri("/api/recetas/me-gustan").header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario));
        for (int i = 0; i < parametros.length; i += 2) {
            peticion.param(parametros[i], parametros[i + 1]);
        }
        try {
            return JsonPath.read(peticion.exchange().getResponse().getContentAsString(), "$.contenido[*].titulo");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
