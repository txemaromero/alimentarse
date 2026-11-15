package es.alimentarse.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

@TestIntegracion
@Transactional
class CatalogoControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DatosDePrueba datos;

    /** Cada petición comprueba que la cuenta del token existe (D-063). */
    @BeforeEach
    void crearUsuario() {
        datos.usuario("prueba");
    }

    @Test
    void losGruposSeListanPorNombreConSusRacionesYNullParaSinLimite() {
        assertThat(mvc.get().uri("/api/grupos").header(HttpHeaders.AUTHORIZATION, tokens.usuario("prueba")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$[*].nombre",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactlyElementsOf(jdbc.queryForList(
                                        "SELECT NOMBRE FROM grupo_ingrediente ORDER BY NOMBRE", String.class)))
                .hasPathSatisfying(
                        "$[?(@.nombre == 'Fruta')].racionesSemana",
                        v -> v.assertThat().asArray().containsExactly((Object) null))
                .hasPathSatisfying(
                        "$[?(@.nombre == 'Dulces')].racionesSemana",
                        v -> v.assertThat().asArray().containsExactly(1));
    }

    @Test
    void losTiposDeRecetaSeListanPorNombre() {
        assertThat(mvc.get().uri("/api/tipos-receta").header(HttpHeaders.AUTHORIZATION, tokens.usuario("prueba")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$[*].nombre",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly("Bebida", "Desayuno", "Entrante", "Plato", "Postre", "Salsa"));
    }

    @Test
    void losCatalogosExigenSesion() {
        assertThat(mvc.get().uri("/api/grupos")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/tipos-receta")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
