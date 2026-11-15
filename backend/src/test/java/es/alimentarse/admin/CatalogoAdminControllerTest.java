package es.alimentarse.admin;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Grupos de alimentos e ingredientes en la administración (D-065), contra MySQL real. */
@TestIntegracion
@Transactional
class CatalogoAdminControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DatosDePrueba datos;

    @Autowired
    TokensDePrueba tokens;

    @BeforeEach
    void crearUsuarios() {
        datos.admin("gestora");
        datos.usuario("comensal");
    }

    @Test
    void laListaDeGruposTraeLasRacionesSiEsBasicoYCuantosIngredientesTiene() {
        assertThat(get("/api/admin/grupos"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$[?(@.nombre == 'Carne')].racionesSemana",
                        v -> v.assertThat().asArray().containsExactly(3))
                .hasPathSatisfying(
                        "$[?(@.nombre == 'Especias')].basico",
                        v -> v.assertThat().asArray().containsExactly(true))
                .hasPathSatisfying(
                        "$[?(@.nombre == 'Fruta')].racionesSemana",
                        v -> v.assertThat().asArray().containsExactly((Object) null));
    }

    @Test
    void cambiarLasRacionesSeNotaAlMomentoEnLaRecomendacion() {
        int filete = datos.receta("Filete", "gestora", true, 3, "Ternera");
        datos.consumo("comensal", filete, Duration.ofDays(1));
        datos.consumo("comensal", filete, Duration.ofDays(2));
        int carne = idGrupo("Carne");
        assertThat(titulosRecomendados()).contains("Filete");

        assertThat(enviar(
                        "PUT",
                        "/api/admin/grupos/" + carne,
                        "{\"nombre\":\"Carne\",\"racionesSemana\":2,\"basico\":false}"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.racionesSemana", v -> v.assertThat().isEqualTo(2));

        assertThat(titulosRecomendados()).doesNotContain("Filete");
        assertThat(mvc.get().uri("/api/grupos").header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .bodyJson()
                .hasPathSatisfying(
                        "$[?(@.nombre == 'Carne')].racionesSemana",
                        v -> v.assertThat().asArray().containsExactly(2));
    }

    @Test
    void crearEditarYBorrarUnGrupo() {
        MvcTestResult creado =
                enviar("POST", "/api/admin/grupos", "{\"nombre\":\" Marisco \",\"racionesSemana\":2,\"basico\":false}");
        assertThat(creado)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.nombre", v -> v.assertThat().isEqualTo("Marisco"))
                .hasPathSatisfying("$.ingredientes", v -> v.assertThat().isEqualTo(0));
        int id = JsonPath.read(contenido(creado), "$.id");

        assertThat(enviar(
                        "POST",
                        "/api/admin/grupos",
                        "{\"nombre\":\"marisco\",\"racionesSemana\":null,\"basico\":false}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.nombre", v -> v.assertThat().isEqualTo("Ya existe un grupo con ese nombre"));
        assertThat(enviar(
                        "PUT",
                        "/api/admin/grupos/" + id,
                        "{\"nombre\":\"Marisco\",\"racionesSemana\":0,\"basico\":false}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(enviar("DELETE", "/api/admin/grupos/" + idGrupo("Carne"), null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("GRUPO_EN_USO"));
        assertThat(enviar("DELETE", "/api/admin/grupos/" + id, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(enviar("DELETE", "/api/admin/grupos/" + id, null)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void fusionarSumaLasCantidadesRepetidasYMueveElResto() {
        jdbc.update("INSERT INTO ingredientes (NOMBRE, GRUPO) SELECT 'Tomate maduro', ID FROM grupo_ingrediente"
                + " WHERE NOMBRE = 'Hortalizas'");
        int duplicado = datos.idIngrediente("Tomate maduro");
        int tomate = datos.idIngrediente("Tomate");
        int conLosDos = datos.receta("Gazpacho", "gestora", true, 2, "Tomate", "Tomate maduro");
        int soloElDuplicado = datos.receta("Salmorejo", "gestora", true, 2, "Tomate maduro");

        assertThat(enviar("POST", "/api/admin/ingredientes/" + duplicado + "/fusionar", "{\"destino\":" + tomate + "}"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.ingrediente.nombre", v -> v.assertThat().isEqualTo("Tomate"))
                .hasPathSatisfying("$.recetas", v -> v.assertThat().isEqualTo(2));

        assertThat(jdbc.queryForObject(
                        "SELECT CANTIDAD FROM ingrediente_receta WHERE RECETA = ? AND INGREDIENTE = ?",
                        String.class,
                        conLosDos,
                        tomate))
                .isEqualTo("1 + 1");
        assertThat(jdbc.queryForList(
                        "SELECT INGREDIENTE FROM ingrediente_receta WHERE RECETA = ?", Integer.class, soloElDuplicado))
                .containsExactly(tomate);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ingredientes WHERE NOMBRE = 'Tomate maduro'", Integer.class))
                .isZero();
        assertThat(enviar("POST", "/api/admin/ingredientes/" + tomate + "/fusionar", "{\"destino\":" + tomate + "}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.destino", v -> v.assertThat().isEqualTo("Elige un ingrediente distinto"));
    }

    @Test
    void soloSeBorraUnIngredienteQueNoUsaNingunaReceta() {
        jdbc.update("INSERT INTO ingredientes (NOMBRE, GRUPO) SELECT 'Tomatte', ID FROM grupo_ingrediente"
                + " WHERE NOMBRE = 'Hortalizas'");
        int errata = datos.idIngrediente("Tomatte");
        datos.receta("Revuelto", "gestora", false, 1, "Huevos");

        assertThat(enviar("DELETE", "/api/admin/ingredientes/" + datos.idIngrediente("Huevos"), null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("INGREDIENTE_EN_USO"));
        assertThat(enviar("DELETE", "/api/admin/ingredientes/" + errata, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(enviar("DELETE", "/api/admin/ingredientes/" + errata, null)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void soloLosAdministradores() {
        assertThat(mvc.get().uri("/api/admin/grupos").header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal")))
                .hasStatus(HttpStatus.FORBIDDEN);
        // Un token que dice ADMIN de alguien que no lo es en la base de datos (D-043)
        assertThat(mvc.get().uri("/api/admin/grupos").header(HttpHeaders.AUTHORIZATION, tokens.admin("comensal")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void losAlimentosDeBedcaSeBuscanPorPalabrasEnCualquierOrden() {
        assertThat(get("/api/admin/alimentos-bedca?q=trigo harina"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$[0].id", v -> v.assertThat().isEqualTo(2677))
                .hasPathSatisfying("$[0].nombre", v -> v.assertThat().isEqualTo("Harina de trigo"))
                // 1385,5 kJ / 4,184
                .hasPathSatisfying("$[0].energiaKcal", v -> v.assertThat().isEqualTo(331))
                .hasPathSatisfying("$[0].energiaKj", v -> v.assertThat().isEqualTo(1385.5))
                .hasPathSatisfying("$[0].proteinas", v -> v.assertThat().isEqualTo(10))
                // BEDCA no da las grasas de la harina
                .hasPathSatisfying("$[0].grasas", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.length()", v -> v.assertThat().isEqualTo(2));
        assertThat(get("/api/admin/alimentos-bedca?q="))
                .hasStatusOk()
                .bodyJson()
                .isEqualTo("[]");
        assertThat(mvc.get()
                        .uri("/api/admin/alimentos-bedca?q=harina")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal"))
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void conUnAlimentoDeBedcaUnIngredienteNuevoCuentaEnLaInformacionNutricional() {
        jdbc.update(
                "INSERT INTO ingredientes (NOMBRE, GRUPO) VALUES ('Alubia roja de prueba', ?)", idGrupo("Legumbres"));
        int alubia = datos.idIngrediente("Alubia roja de prueba");
        int receta = datos.receta("Alubias de prueba", "comensal", true, 1, "Alubia roja de prueba");
        jdbc.update("UPDATE ingrediente_receta SET CANTIDAD = '100 gramos' WHERE RECETA = ?", receta);
        String filtro = "/api/ingredientes?nombre=Alubia roja de prueba";

        assertThat(nutricion(receta))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ingredientes[0].estado", v -> v.assertThat().isEqualTo("SIN_DATOS"));
        assertThat(get(filtro + "&sinAlimento=true"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[0].alimento", v -> v.assertThat().isNull());

        assertThat(enviar("PUT", "/api/admin/ingredientes/" + alubia + "/nutricion", nutricion("2197")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.alimento", v -> v.assertThat().isEqualTo("Judía pinta"));
        assertThat(nutricion(receta))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ingredientes[0].estado", v -> v.assertThat().isEqualTo("CALCULADO"))
                .hasPathSatisfying(
                        "$.ingredientes[0].alimento", v -> v.assertThat().isEqualTo("Judía pinta"))
                .hasPathSatisfying("$.total.energiaKcal", v -> v.assertThat().isNotNull());
        assertThat(get(filtro + "&sinAlimento=true"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
        assertThat(get(filtro))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[0].alimento", v -> v.assertThat().isEqualTo("Judía pinta"));

        // Se cambia y se quita
        assertThat(enviar("PUT", "/api/admin/ingredientes/" + alubia + "/nutricion", nutricion("2196")))
                .bodyJson()
                .hasPathSatisfying("$.alimento", v -> v.assertThat().isEqualTo("Judía blanca"));
        assertThat(enviar("PUT", "/api/admin/ingredientes/" + alubia + "/nutricion", nutricion("null")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.alimento", v -> v.assertThat().isNull());
        assertThat(nutricion(receta))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ingredientes[0].estado", v -> v.assertThat().isEqualTo("SIN_DATOS"));
    }

    @Test
    void asignarUnAlimentoQueNoExisteOAUnIngredienteQueNoExisteFalla() {
        int tomate = datos.idIngrediente("Tomate");

        assertThat(enviar("PUT", "/api/admin/ingredientes/" + tomate + "/nutricion", nutricion("1")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.alimento", v -> v.assertThat().isEqualTo(CatalogoAdminService.ALIMENTO_INEXISTENTE));
        assertThat(enviar("PUT", "/api/admin/ingredientes/999999/nutricion", nutricion("2421")))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put()
                        .uri("/api/admin/ingredientes/" + tomate + "/nutricion")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nutricion("null"))
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ingrediente_alimento WHERE INGREDIENTE = ?", Integer.class, tomate))
                .isEqualTo(1);
    }

    @Test
    void alFusionarElDestinoHeredaElAlimentoYLasPiezasSoloSiNoLosTiene() {
        int huevos = datos.idIngrediente("Huevos");
        jdbc.update(
                "INSERT INTO ingredientes (NOMBRE, GRUPO) VALUES ('Huevo campero de prueba', ?)", idGrupo("Huevos"));
        int campero = datos.idIngrediente("Huevo campero de prueba");

        assertThat(enviar("POST", "/api/admin/ingredientes/" + huevos + "/fusionar", "{\"destino\": " + campero + "}"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.ingrediente.alimento", v -> v.assertThat().isEqualTo("Huevo de gallina fresco"));
        assertThat(jdbc.queryForObject(
                        "SELECT GRAMOS FROM peso_pieza_ingrediente WHERE INGREDIENTE = ? AND PIEZA = 'unidad'",
                        java.math.BigDecimal.class,
                        campero))
                .isEqualByComparingTo("50");

        // Si el destino ya tiene alimento, se queda con el suyo
        int tomate = datos.idIngrediente("Tomate");
        int pimiento = datos.idIngrediente("Pimiento");
        assertThat(enviar("POST", "/api/admin/ingredientes/" + tomate + "/fusionar", "{\"destino\": " + pimiento + "}"))
                .bodyJson()
                .hasPathSatisfying("$.ingrediente.alimento", v -> v.assertThat().isEqualTo("Pimiento rojo, crudo"));
        assertThat(jdbc.queryForObject(
                        "SELECT GRAMOS FROM peso_pieza_ingrediente WHERE INGREDIENTE = ? AND PIEZA = 'unidad'",
                        java.math.BigDecimal.class,
                        pimiento))
                .isEqualByComparingTo("150");
    }

    @Test
    void lasPiezasDeUnIngredienteNuevoHacenQueCuentenSusUnidades() {
        jdbc.update(
                "INSERT INTO ingredientes (NOMBRE, GRUPO) VALUES ('Atún en lata de prueba', ?)", idGrupo("Pescado"));
        int atun = datos.idIngrediente("Atún en lata de prueba");
        int receta = datos.receta("Ensalada de prueba", "comensal", true, 1, "Atún en lata de prueba");
        jdbc.update("UPDATE ingrediente_receta SET CANTIDAD = 'una lata' WHERE RECETA = ?", receta);
        String uri = "/api/admin/ingredientes/" + atun + "/nutricion";

        assertThat(get(uri))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.alimento", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.piezas", v -> v.assertThat().asArray().isEmpty());

        // «Atún al natural» (2133) y una lata de 60 g: «una lata» son 60 g
        assertThat(enviar("PUT", uri, "{\"alimento\": 2133, \"piezas\": [{\"pieza\": \" Lata \", \"gramos\": 60}]}"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.alimento", v -> v.assertThat().isEqualTo("Atún, al natural"));
        assertThat(get(uri))
                .bodyJson()
                .hasPathSatisfying("$.alimento.id", v -> v.assertThat().isEqualTo(2133))
                .hasPathSatisfying("$.piezas[0].pieza", v -> v.assertThat().isEqualTo("lata"))
                .hasPathSatisfying("$.piezas[0].gramos", v -> v.assertThat().isEqualTo(60.0));
        assertThat(nutricion(receta))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ingredientes[0].estado", v -> v.assertThat().isEqualTo("CALCULADO"))
                .hasPathSatisfying(
                        "$.ingredientes[0].gramos", v -> v.assertThat().isEqualTo(60.0));

        // Con una lista vacía se quitan las piezas
        assertThat(enviar("PUT", uri, "{\"alimento\": 2133, \"piezas\": []}")).hasStatusOk();
        assertThat(nutricion(receta))
                .bodyJson()
                .hasPathSatisfying(
                        "$.ingredientes[0].estado", v -> v.assertThat().isEqualTo("SIN_PESO"));
    }

    @Test
    void lasPiezasSeValidanCampoACampo() {
        String uri = "/api/admin/ingredientes/" + datos.idIngrediente("Tomate") + "/nutricion";

        assertThat(enviar(
                        "PUT",
                        uri,
                        "{\"alimento\": 2421, \"piezas\": [{\"pieza\": \"2 latas\", \"gramos\": 0},"
                                + " {\"pieza\": \"unidad\", \"gramos\": 120.25}]}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores['piezas[0].pieza']", v -> v.assertThat().isEqualTo("Una sola palabra, sin números"))
                .hasPathSatisfying(
                        "$.errores['piezas[0].gramos']", v -> v.assertThat().isEqualTo("Debe pesar más de 0 g"))
                .hasPathSatisfying(
                        "$.errores['piezas[1].gramos']", v -> v.assertThat().isEqualTo("Como mucho un decimal"));
        assertThat(enviar(
                        "PUT", uri, "{\"alimento\": 2421, \"piezas\": [{\"pieza\": \"cucharadas\", \"gramos\": 10}]}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores['piezas[0].pieza']",
                        v -> v.assertThat().isEqualTo(CatalogoAdminService.PIEZA_GENERAL));
        assertThat(enviar("PUT", uri, "{\"alimento\": 2421}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.piezas", v -> v.assertThat().isNotNull());
    }

    private static String nutricion(String alimento) {
        return "{\"alimento\": " + alimento + ", \"piezas\": []}";
    }

    private MvcTestResult nutricion(int receta) {
        return mvc.get()
                .uri("/api/recetas/" + receta + "/nutricion")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal"))
                .exchange();
    }

    private List<String> titulosRecomendados() {
        MvcTestResult respuesta = mvc.get()
                .uri("/api/recetas/recomendadas")
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario("comensal"))
                .exchange();
        return JsonPath.read(contenido(respuesta), "$.contenido[*].titulo");
    }

    private MvcTestResult get(String uri) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, tokens.admin("gestora"))
                .exchange();
    }

    private MvcTestResult enviar(String metodo, String uri, String cuerpo) {
        var peticion = switch (metodo) {
            case "POST" -> mvc.post();
            case "PUT" -> mvc.put();
            default -> mvc.delete();
        };
        peticion.uri(uri).header(HttpHeaders.AUTHORIZATION, tokens.admin("gestora"));
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return peticion.exchange();
    }

    private int idGrupo(String nombre) {
        return jdbc.queryForObject("SELECT ID FROM grupo_ingrediente WHERE NOMBRE = ?", Integer.class, nombre);
    }

    private static String contenido(MvcTestResult respuesta) {
        try {
            return respuesta.getResponse().getContentAsString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
