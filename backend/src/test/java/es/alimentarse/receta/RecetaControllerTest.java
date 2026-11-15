package es.alimentarse.receta;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.alimentarse.support.ImagenesDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recetas contra MySQL real. Cada test parte de un fixture (dos autores, cinco recetas, una sin publicar, y algunos
 * likes) y se deshace al terminar, imágenes subidas incluidas.
 */
@TestIntegracion
@Transactional
class RecetaControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TokensDePrueba tokens;

    private int tortilla;
    private int lentejas;
    private int batido;
    private int pollo;
    private int borrador;

    @BeforeEach
    void crearFixture() {
        jdbc.update("INSERT INTO usuarios (ID, NOMBRE, APELLIDO1, EMAIL, PASSWORD) VALUES"
                + " ('autora', 'Ana', 'García', 'autora@pruebas.local', 'x'),"
                + " ('otro', 'Óscar', 'Ruiz', 'otro@pruebas.local', 'x')");
        tortilla = crearReceta(
                "Tortilla de espárragos", "autora", "Plato", true, 30, 4, "Fácil", 5, "Huevos", "Espárragos", "Sal");
        lentejas = crearReceta(
                "Lentejas con arroz", "otro", "Plato", true, 60, 6, "Media", 4, "Lentejas", "Arroz", "Cebolla");
        batido = crearReceta(
                "Batido verde", "autora", "Bebida", true, 5, 1, "Fácil", 3, "Espinacas", "Manzana", "Plátano");
        pollo = crearReceta(
                "Pollo al horno con patatas", "otro", "Plato", true, 90, 4, "Difícil", 2, "Pollo", "Patata", "Ajo");
        borrador = crearReceta("Borrador secreto", "autora", "Postre", false, 20, 2, "Fácil", 1, "Azúcar", "Huevos");
        jdbc.update(
                "INSERT INTO like_receta (USUARIO, RECETA) VALUES ('autora', ?), ('otro', ?), ('autora', ?)",
                lentejas,
                lentejas,
                pollo);
    }

    @Test
    void elListadoSoloMuestraLasPublicadasDeLaMasRecienteALaMasAntigua() {
        assertThat(get("/api/recetas", "otro"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].titulo",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly(
                                        "Pollo al horno con patatas",
                                        "Batido verde",
                                        "Lentejas con arroz",
                                        "Tortilla de espárragos"))
                .hasPathSatisfying("$.tamano", v -> v.assertThat().isEqualTo(9))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(4))
                .hasPathSatisfying("$.contenido[0].autor", v -> v.assertThat().isEqualTo("otro"))
                .hasPathSatisfying(
                        "$.contenido[0].tipo.nombre", v -> v.assertThat().isEqualTo("Plato"))
                .hasPathSatisfying("$.contenido[0].likes", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.contenido[0].meGusta", v -> v.assertThat().isEqualTo(false))
                .hasPathSatisfying("$.contenido[0].imagen", v -> v.assertThat().isNull());
    }

    @Test
    void cadaFiltroDeLaSeccion65() {
        assertThat(titulos("autora", "titulo", "ESPARRAGOS")).containsExactly("Tortilla de espárragos");
        assertThat(titulos("autora", "tipo", String.valueOf(idTipo("Bebida")))).containsExactly("Batido verde");
        assertThat(titulos("autora", "usuario", "OTR"))
                .containsExactly("Pollo al horno con patatas", "Lentejas con arroz");
        assertThat(titulos("autora", "dificultad", "Difícil")).containsExactly("Pollo al horno con patatas");
        assertThat(titulos("autora", "comensales", "4"))
                .containsExactly("Pollo al horno con patatas", "Lentejas con arroz", "Tortilla de espárragos");
        assertThat(titulos("autora", "tiempoMax", "30")).containsExactly("Batido verde", "Tortilla de espárragos");
        assertThat(titulos("autora", "titulo", "a", "dificultad", "Fácil"))
                .containsExactly("Batido verde", "Tortilla de espárragos");
    }

    @Test
    void elFiltroDeIngredientesExigeQueLaRecetaLosContengaTodos() {
        String huevos = String.valueOf(idIngrediente("Huevos"));
        String esparragos = String.valueOf(idIngrediente("Espárragos"));
        String lenteja = String.valueOf(idIngrediente("Lentejas"));

        assertThat(titulos("autora", "ingredientes", huevos + "," + esparragos))
                .containsExactly("Tortilla de espárragos");
        assertThat(titulos("autora", "ingredientes", huevos, "ingredientes", esparragos))
                .containsExactly("Tortilla de espárragos");
        assertThat(titulos("autora", "ingredientes", huevos + "," + lenteja)).isEmpty();
        // El borrador también lleva huevos, pero no está publicado
        assertThat(titulos("autora", "ingredientes", huevos)).containsExactly("Tortilla de espárragos");
    }

    @Test
    void losFiltrosNoValidosDevuelven400EnEspanol() {
        assertThat(get("/api/recetas?comensales=0", "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.comensales", v -> v.assertThat().isEqualTo("Indica al menos 1 comensal"));
        assertThat(get("/api/recetas?tipo=abc", "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.tipo", v -> v.assertThat().isEqualTo("Valor no válido"));
        assertThat(get("/api/recetas?size=0", "autora")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    /** Los textos y la lista de ingredientes de los filtros tienen tope (D-090). */
    @Test
    void losFiltrosDeTextoYLosIngredientesTienenTope() {
        assertThat(get("/api/recetas?titulo=" + "a".repeat(101), "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.titulo", v -> v.assertThat().isEqualTo("Busca con 100 caracteres como mucho"));
        assertThat(get("/api/recetas?usuario=" + "a".repeat(31), "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.usuario", v -> v.assertThat().isNotNull());
        String ingredientes = String.join(
                ",", IntStream.rangeClosed(1, 51).mapToObj(String::valueOf).toList());
        assertThat(get("/api/recetas?ingredientes=" + ingredientes, "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.ingredientes",
                        v -> v.assertThat().isEqualTo("Filtra por 50 ingredientes como mucho"));
        assertThat(get("/api/recetas/sugerencias?q=" + "a".repeat(101), "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.q", v -> v.assertThat().isEqualTo("Busca con 100 caracteres como mucho"));
        assertThat(get("/api/recetas?titulo=" + "a".repeat(100), "autora")).hasStatusOk();
    }

    @Test
    void elListadoSePaginaDeNueveEnNueve() {
        for (int i = 1; i <= 10; i++) {
            crearReceta("Receta extra " + i, "otro", "Plato", true, 10, 2, "Fácil", 10 + i, "Sal");
        }

        assertThat(get("/api/recetas", "autora"))
                .bodyJson()
                .hasPathSatisfying("$.contenido", v -> v.assertThat().asArray().hasSize(9))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(14))
                .hasPathSatisfying("$.totalPaginas", v -> v.assertThat().isEqualTo(2));
        assertThat(get("/api/recetas?page=1", "autora"))
                .bodyJson()
                .hasPathSatisfying("$.contenido", v -> v.assertThat().asArray().hasSize(5));
    }

    @Test
    void misRecetasIncluyeLasNoPublicadas() {
        assertThat(get("/api/recetas/mias", "autora"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].titulo",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly("Borrador secreto", "Batido verde", "Tortilla de espárragos"))
                .hasPathSatisfying(
                        "$.contenido[0].publicada", v -> v.assertThat().isEqualTo(false));
    }

    @Test
    void mejorValoradasYUltimasParaLaPortada() {
        assertThat(get("/api/recetas/mejor-valoradas?limit=2", "autora"))
                .bodyJson()
                .hasPathSatisfying(
                        "$[*].titulo",
                        v -> v.assertThat()
                                .asArray()
                                .containsExactly("Lentejas con arroz", "Pollo al horno con patatas"))
                .hasPathSatisfying("$[0].likes", v -> v.assertThat().isEqualTo(2))
                .hasPathSatisfying("$[0].meGusta", v -> v.assertThat().isEqualTo(true));
        assertThat(get("/api/recetas/ultimas?limit=2", "autora"))
                .bodyJson()
                .hasPathSatisfying(
                        "$[*].titulo",
                        v -> v.assertThat().asArray().containsExactly("Pollo al horno con patatas", "Batido verde"));
        assertThat(get("/api/recetas/ultimas?limit=21", "autora")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void lasSugerenciasSoloIncluyenRecetasPublicadas() {
        assertThat(get("/api/recetas/sugerencias?q=to", "autora"))
                .bodyJson()
                .hasPathSatisfying("$[0].titulo", v -> v.assertThat().isEqualTo("Tortilla de espárragos"))
                .hasPathSatisfying("$[*].titulo", v -> v.assertThat().asArray().doesNotContain("Borrador secreto"));
        assertThat(get("/api/recetas/sugerencias?q=", "autora"))
                .bodyJson()
                .hasPathSatisfying("$", v -> v.assertThat().asArray().isEmpty());
    }

    @Test
    void elDetalleIncluyeIngredientesGruposYMaximoSemanal() {
        assertThat(get("/api/recetas/" + pollo, "autora"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.instrucciones", v -> v.assertThat().isEqualTo("<p>Pasos</p>"))
                .hasPathSatisfying(
                        "$.ingredientes[*].nombre",
                        v -> v.assertThat().asArray().containsExactly("Ajo", "Patata", "Pollo"))
                .hasPathSatisfying(
                        "$.ingredientes[0].cantidad", v -> v.assertThat().isEqualTo("1"))
                .hasPathSatisfying(
                        "$.ingredientes[0].grupo.nombre", v -> v.assertThat().isEqualTo("Hortalizas"))
                .hasPathSatisfying(
                        "$.grupos[*].nombre", v -> v.assertThat().asArray().containsExactly("Carne", "Hortalizas"))
                .hasPathSatisfying("$.maxSemanal", v -> v.assertThat().isEqualTo(3))
                .hasPathSatisfying("$.likes", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.meGusta", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.esAutor", v -> v.assertThat().isEqualTo(false));
        assertThat(get("/api/recetas/" + tortilla, "autora"))
                .bodyJson()
                .hasPathSatisfying("$.maxSemanal", v -> v.assertThat().isEqualTo(4));
        assertThat(get("/api/recetas/" + batido, "autora"))
                .bodyJson()
                .hasPathSatisfying("$.maxSemanal", v -> v.assertThat().isNull());
    }

    @Test
    void unaRecetaNoPublicadaSoloExisteParaSuAutor() {
        assertThat(get("/api/recetas/" + borrador, "otro"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("NO_ENCONTRADO"));
        assertThat(get("/api/recetas/" + borrador, "autora"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.esAutor", v -> v.assertThat().isEqualTo(true))
                .hasPathSatisfying("$.publicada", v -> v.assertThat().isEqualTo(false));
    }

    @Test
    void crearConImagenLaDejaSinPublicarHastaQueElAutorLaPublica() throws Exception {
        byte[] png = ImagenesDePrueba.png();

        MvcTestResult creada = mvc.post()
                .uri("/api/recetas")
                .multipart()
                .file(parteReceta(conFotos(
                        receta("Revuelto de setas", "<p>Revuelve</p>", ingrediente("Huevos", "3")),
                        Map.of("nueva", 0, "descripcion", "El revuelto en el plato"))))
                .file(ImagenesDePrueba.parteFoto("foto.png", png))
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora"))
                .exchange();

        assertThat(creada)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.publicada", v -> v.assertThat().isEqualTo(false))
                .hasPathSatisfying("$.autor", v -> v.assertThat().isEqualTo("autora"))
                .hasPathSatisfying(
                        "$.imagen", v -> v.assertThat().asString().matches("/api/imagenes/[0-9a-f-]{36}\\.jpg"))
                .hasPathSatisfying("$.fotos.length()", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.fotos[0].descripcion", v -> v.assertThat().isEqualTo("El revuelto en el plato"));
        String cuerpo = creada.getResponse().getContentAsString();
        int id = JsonPath.read(cuerpo, "$.id");
        String imagen = JsonPath.read(cuerpo, "$.imagen");
        assertThat(creada.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/recetas/" + id);

        MvcTestResult descargada = mvc.get().uri(imagen).exchange();
        // Lo que se publica es la imagen recodificada, no el fichero subido (D-069)
        assertThat(descargada).hasStatusOk().hasContentType(MediaType.IMAGE_JPEG);
        assertThat(descargada.getResponse().getContentAsByteArray()).isNotEqualTo(png);
        assertThat(get("/api/recetas/" + id, "otro")).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(mvc.patch()
                        .uri("/api/recetas/" + id + "/publicada")
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicada\": true}"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.publicada", v -> v.assertThat().isEqualTo(true));
        assertThat(get("/api/recetas/" + id, "otro")).hasStatusOk();
    }

    @Test
    void crearSinImagenYConInstruccionesPeligrosasLasSanea() {
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(receta(
                                "Revuelto",
                                "<p>Paso <b>uno</b></p><script>alert(1)</script><a href=\"javascript:x()\">x</a>",
                                ingrediente("Huevos", "3"))))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.imagen", v -> v.assertThat().isNull())
                .hasPathSatisfying(
                        "$.instrucciones",
                        v -> v.assertThat()
                                .asString()
                                .contains("<p>Paso <b>uno</b></p>")
                                .doesNotContain("script", "javascript"));
    }

    @Test
    void crearValidaTodosLosCampos() {
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(Map.of()))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores",
                        v -> v.assertThat()
                                .asMap()
                                .containsOnlyKeys(
                                        "titulo",
                                        "instrucciones",
                                        "tipo",
                                        "tiempo",
                                        "comensales",
                                        "dificultad",
                                        "ingredientes"));

        Map<String, Object> repetidos =
                receta("Revuelto", "<p>Paso</p>", ingrediente("Huevos", "3"), ingrediente("Huevos", "2"));
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(repetidos))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.ingredientes", v -> v.assertThat().isEqualTo(RecetaService.INGREDIENTES_REPETIDOS));
    }

    @Test
    void unaImagenQueNoEsJpegPngNiWebpSeRechaza() {
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(conFotos(
                                receta("Revuelto", "<p>Paso</p>", ingrediente("Huevos", "3")), Map.of("nueva", 0))))
                        .file(ImagenesDePrueba.parteFoto(
                                "virus.png", "no soy una imagen".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores['fotos[0]']", v -> v.assertThat().asString().contains("JPEG, PNG o WebP"));
    }

    @Test
    void laGaleriaSeCreaSeReordenaYSeCambiaAlEditar() throws Exception {
        MvcTestResult creada = mvc.post()
                .uri("/api/recetas")
                .multipart()
                .file(parteReceta(conFotos(
                        receta("Galería", "<p>Paso</p>", ingrediente("Huevos", "3")),
                        Map.of("nueva", 0, "descripcion", "Primera"),
                        Map.of("nueva", 1),
                        Map.of("nueva", 2, "descripcion", "Tercera"))))
                .file(ImagenesDePrueba.parteFoto("a.png", ImagenesDePrueba.png()))
                .file(ImagenesDePrueba.parteFoto("b.jpg", ImagenesDePrueba.jpeg()))
                .file(ImagenesDePrueba.parteFoto("c.webp", ImagenesDePrueba.webp()))
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora"))
                .exchange();
        assertThat(creada).hasStatus(HttpStatus.CREATED);
        String cuerpo = creada.getResponse().getContentAsString();
        int id = JsonPath.read(cuerpo, "$.id");
        List<String> nombres = JsonPath.read(cuerpo, "$.fotos[*].nombre");
        assertThat(nombres).hasSize(3).doesNotHaveDuplicates();
        assertThat((String) JsonPath.read(cuerpo, "$.imagen")).isEqualTo("/api/imagenes/" + nombres.get(0));

        // La tercera pasa a ser la principal, la primera se queda detrás, la segunda se quita y se añade una nueva
        Map<String, Object> cambios = conFotos(
                receta("Galería", "<p>Paso</p>", ingrediente("Huevos", "3")),
                Map.of("existente", nombres.get(2), "descripcion", "Ahora la principal"),
                Map.of("existente", nombres.get(0)),
                Map.of("nueva", 0));
        assertThat(mvc.put()
                        .uri("/api/recetas/" + id)
                        .multipart()
                        .file(parteReceta(cambios))
                        .file(ImagenesDePrueba.parteFoto("d.png", ImagenesDePrueba.png()))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.imagen", v -> v.assertThat().isEqualTo("/api/imagenes/" + nombres.get(2)))
                .hasPathSatisfying("$.fotos[0].nombre", v -> v.assertThat().isEqualTo(nombres.get(2)))
                .hasPathSatisfying("$.fotos[0].descripcion", v -> v.assertThat().isEqualTo("Ahora la principal"))
                .hasPathSatisfying("$.fotos[1].nombre", v -> v.assertThat().isEqualTo(nombres.get(0)))
                .hasPathSatisfying("$.fotos[1].descripcion", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.fotos.length()", v -> v.assertThat().isEqualTo(3));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM foto_receta WHERE RECETA = ? AND NOMBRE = ?",
                        Integer.class,
                        id,
                        nombres.get(1)))
                .isZero();

        // Sin «fotos», la galería se queda como está; con una lista vacía, se quita entera
        assertThat(mvc.put()
                        .uri("/api/recetas/" + id)
                        .multipart()
                        .file(parteReceta(receta("Galería", "<p>Paso</p>", ingrediente("Huevos", "3"))))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .bodyJson()
                .hasPathSatisfying("$.fotos.length()", v -> v.assertThat().isEqualTo(3));
        assertThat(mvc.put()
                        .uri("/api/recetas/" + id)
                        .multipart()
                        .file(parteReceta(conFotos(receta("Galería", "<p>Paso</p>", ingrediente("Huevos", "3")))))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .bodyJson()
                .hasPathSatisfying("$.imagen", v -> v.assertThat().isNull())
                .hasPathSatisfying("$.fotos", v -> v.assertThat().asArray().isEmpty());
    }

    @Test
    void elVideoEsUnEnlaceAYoutubeOVimeoQueSeQuitaDejandoloVacio() {
        Map<String, Object> conVideo =
                new java.util.HashMap<>(receta("Tortilla", "<p>Paso</p>", ingrediente("Huevos", "3")));
        conVideo.put("video", " https://youtu.be/abc123 ");
        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla)
                        .multipart()
                        .file(parteReceta(conVideo))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.video", v -> v.assertThat().isEqualTo("https://youtu.be/abc123"));

        conVideo.put("video", "https://ejemplo.com/video");
        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla)
                        .multipart()
                        .file(parteReceta(conVideo))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.video", v -> v.assertThat().isEqualTo(RecetaService.VIDEO_NO_VALIDO));

        conVideo.put("video", "");
        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla)
                        .multipart()
                        .file(parteReceta(conVideo))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .bodyJson()
                .hasPathSatisfying("$.video", v -> v.assertThat().isNull());
    }

    @Test
    void lasFotosQueNoSonDeLaRecetaLasQueSobranYMasDeSeisSeRechazan() {
        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla)
                        .multipart()
                        .file(parteReceta(conFotos(
                                receta("Tortilla", "<p>Paso</p>", ingrediente("Huevos", "3")),
                                Map.of("existente", "de-otra-receta.jpg"))))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores['fotos[0].existente']", v -> v.assertThat().isEqualTo(RecetaService.FOTO_AJENA));
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(receta("Sobra", "<p>Paso</p>", ingrediente("Huevos", "3"))))
                        .file(ImagenesDePrueba.parteFoto("a.png", ImagenesDePrueba.png()))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.fotos", v -> v.assertThat().isEqualTo(RecetaService.FICHEROS_SOBRANTES));
        Map<String, Object> siete = conFotos(
                receta("Siete", "<p>Paso</p>", ingrediente("Huevos", "3")),
                Map.of("nueva", 0),
                Map.of("nueva", 1),
                Map.of("nueva", 2),
                Map.of("nueva", 3),
                Map.of("nueva", 4),
                Map.of("nueva", 5),
                Map.of("nueva", 6));
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(siete))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores.fotos", v -> v.assertThat().isEqualTo("Como mucho 6 fotos"));
    }

    @Test
    void unaRecetaTieneComoMucho50Ingredientes() {
        // D-089: sin tope, miles de ids acabarían en un IN (…) enorme
        Map<String, Object> receta = receta("Demasiados", "<p>Paso</p>");
        receta.put(
                "ingredientes",
                java.util.stream.IntStream.rangeClosed(1, 51)
                        .mapToObj(id -> Map.of("ingrediente", id, "cantidad", "1"))
                        .toList());

        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(receta))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.ingredientes", v -> v.assertThat().isEqualTo("Añade entre 1 y 50 ingredientes"));
        receta.put("ingredientes", List.of());
        assertThat(mvc.post()
                        .uri("/api/recetas")
                        .multipart()
                        .file(parteReceta(receta))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.ingredientes", v -> v.assertThat().isEqualTo("Añade entre 1 y 50 ingredientes"));
    }

    @Test
    void soloLaAutoraPuedeEditarYAlEditarSeSustituyenLosIngredientes() {
        Map<String, Object> cambios = receta(
                "Tortilla de espárragos trigueros",
                "<p>Nuevos pasos</p>",
                ingrediente("Huevos", "6"),
                ingrediente("Espárragos", "1 manojo"));

        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla)
                        .multipart()
                        .file(parteReceta(cambios))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("otro")))
                .hasStatus(HttpStatus.FORBIDDEN);

        assertThat(mvc.put()
                        .uri("/api/recetas/" + tortilla)
                        .multipart()
                        .file(parteReceta(cambios))
                        .header(HttpHeaders.AUTHORIZATION, tokens.usuario("autora")))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.titulo", v -> v.assertThat().isEqualTo("Tortilla de espárragos trigueros"))
                .hasPathSatisfying(
                        "$.ingredientes[*].nombre",
                        v -> v.assertThat().asArray().containsExactly("Espárragos", "Huevos"))
                .hasPathSatisfying("$.publicada", v -> v.assertThat().isEqualTo(true));
    }

    @Test
    void losMeGustaSonIdempotentesYSoloEnRecetasPublicadas() {
        assertThat(enviar("POST", "/api/recetas/" + batido + "/like", "otro"))
                .bodyJson()
                .hasPathSatisfying("$.likes", v -> v.assertThat().isEqualTo(1))
                .hasPathSatisfying("$.meGusta", v -> v.assertThat().isEqualTo(true));
        assertThat(enviar("POST", "/api/recetas/" + batido + "/like", "otro"))
                .bodyJson()
                .hasPathSatisfying("$.likes", v -> v.assertThat().isEqualTo(1));
        assertThat(enviar("DELETE", "/api/recetas/" + batido + "/like", "otro"))
                .bodyJson()
                .hasPathSatisfying("$.likes", v -> v.assertThat().isEqualTo(0))
                .hasPathSatisfying("$.meGusta", v -> v.assertThat().isEqualTo(false));

        assertThat(enviar("POST", "/api/recetas/" + borrador + "/like", "autora"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("RECETA_NO_PUBLICADA"));
        assertThat(enviar("POST", "/api/recetas/" + borrador + "/like", "otro")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void lasRecetasExigenSesion() {
        assertThat(mvc.get().uri("/api/recetas")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private int crearReceta(
            String titulo,
            String autor,
            String tipo,
            boolean publicada,
            int tiempo,
            int comensales,
            String dificultad,
            int haceDias,
            String... ingredientes) {
        jdbc.update(
                "INSERT INTO recetas (TITULO, INSTRUCCIONES, USUARIO, TIPO, PUBLICADA, TIEMPO, COMENSALES, DIFICULTAD,"
                        + " FECHA_CREACION) SELECT ?, '<p>Pasos</p>', ?, ID, ?, ?, ?, ?, NOW() - INTERVAL ? DAY"
                        + " FROM tipo_receta WHERE NOMBRE = ?",
                titulo,
                autor,
                publicada,
                tiempo,
                comensales,
                dificultad,
                haceDias,
                tipo);
        int id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        for (String ingrediente : ingredientes) {
            jdbc.update(
                    "INSERT INTO ingrediente_receta (INGREDIENTE, RECETA, CANTIDAD)"
                            + " SELECT ID, ?, '1' FROM ingredientes WHERE NOMBRE = ?",
                    id,
                    ingrediente);
        }
        return id;
    }

    private Map<String, Object> receta(String titulo, String instrucciones, Map<String, Object>... ingredientes) {
        Map<String, Object> receta = new HashMap<>();
        receta.put("titulo", titulo);
        receta.put("instrucciones", instrucciones);
        receta.put("tipo", idTipo("Plato"));
        receta.put("tiempo", 15);
        receta.put("comensales", 2);
        receta.put("dificultad", "Fácil");
        receta.put("ingredientes", List.of(ingredientes));
        return receta;
    }

    private Map<String, Object> ingrediente(String nombre, String cantidad) {
        return Map.of("ingrediente", idIngrediente(nombre), "cantidad", cantidad);
    }

    /** La receta con su galería («fotos», D-083). */
    @SafeVarargs
    private static Map<String, Object> conFotos(Map<String, Object> receta, Map<String, Object>... fotos) {
        Map<String, Object> conFotos = new java.util.HashMap<>(receta);
        conFotos.put("fotos", List.of(fotos));
        return conFotos;
    }

    private MockMultipartFile parteReceta(Map<String, Object> receta) {
        return new MockMultipartFile("receta", "", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(receta));
    }

    private MvcTestResult get(String uri, String usuario) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    private MvcTestResult enviar(String metodo, String uri, String usuario) {
        var peticion = "POST".equals(metodo) ? mvc.post() : mvc.delete();
        return peticion.uri(uri)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario))
                .exchange();
    }

    /** Títulos del listado con los parámetros dados en pares nombre, valor. */
    private List<String> titulos(String usuario, String... parametros) {
        var peticion = mvc.get().uri("/api/recetas").header(HttpHeaders.AUTHORIZATION, tokens.usuario(usuario));
        for (int i = 0; i < parametros.length; i += 2) {
            peticion.param(parametros[i], parametros[i + 1]);
        }
        try {
            List<String> titulos =
                    JsonPath.read(peticion.exchange().getResponse().getContentAsString(), "$.contenido[*].titulo");
            return new ArrayList<>(titulos);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private int idTipo(String nombre) {
        return jdbc.queryForObject("SELECT ID FROM tipo_receta WHERE NOMBRE = ?", Integer.class, nombre);
    }

    private int idIngrediente(String nombre) {
        return jdbc.queryForObject("SELECT ID FROM ingredientes WHERE NOMBRE = ?", Integer.class, nombre);
    }
}
