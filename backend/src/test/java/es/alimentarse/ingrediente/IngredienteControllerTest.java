package es.alimentarse.ingrediente;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.DatosDePrueba;
import es.alimentarse.support.TestIntegracion;
import es.alimentarse.support.TokensDePrueba;
import java.util.HashMap;
import java.util.List;
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

/** Cada test se deshace al terminar: las peticiones de MockMvc se ejecutan dentro de su transacción. */
@TestIntegracion
@Transactional
class IngredienteControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TokensDePrueba tokens;

    @Autowired
    DatosDePrueba datos;

    /** Cada petición comprueba que la cuenta del token existe (D-063). */
    @BeforeEach
    void crearUsuarios() {
        datos.usuario("prueba");
        datos.admin("gestora");
        // Crear ingredientes exige el email confirmado (D-089)
        jdbc.update("UPDATE usuarios SET EMAIL_VERIFICADO = TRUE WHERE ID = 'prueba'");
    }

    @Test
    void laPrimeraPaginaTraeDieciseisOrdenadosPorNombreYLosTotales() {
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM ingredientes", Integer.class);
        List<String> primeros =
                jdbc.queryForList("SELECT NOMBRE FROM ingredientes ORDER BY NOMBRE, ID LIMIT 16", String.class);

        assertThat(get("/api/ingredientes"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].nombre", v -> v.assertThat().asArray().containsExactlyElementsOf(primeros))
                .hasPathSatisfying("$.pagina", v -> v.assertThat().isEqualTo(0))
                .hasPathSatisfying("$.tamano", v -> v.assertThat().isEqualTo(16))
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(total))
                .hasPathSatisfying("$.totalPaginas", v -> v.assertThat().isEqualTo((total + 15) / 16))
                .hasPathSatisfying(
                        "$.contenido[0].grupo.nombre",
                        v -> v.assertThat().asString().isNotBlank());
    }

    @Test
    void laUltimaPaginaTraeLosQueQuedan() {
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM ingredientes", Integer.class);
        int ultima = (total + 15) / 16 - 1;

        assertThat(get("/api/ingredientes?page=" + ultima))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.contenido", v -> v.assertThat().asArray().hasSize(total - 16 * ultima));
    }

    @Test
    void elFiltroPorNombreBuscaContieneSinMayusculasNiAcentos() {
        assertThat(get("/api/ingredientes?nombre=ACEITE"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].nombre",
                        v -> v.assertThat().asArray().containsExactly("Aceite de girasol", "Aceite de oliva"));
        assertThat(get("/api/ingredientes?nombre=limon"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.contenido[*].nombre", v -> v.assertThat().asArray().containsExactly("Limón"));
    }

    @Test
    void elFiltroPorGrupoSeCombinaConElDeNombre() {
        int fruta = idGrupo("Fruta");
        int frutas = jdbc.queryForObject("SELECT COUNT(*) FROM ingredientes WHERE GRUPO = ?", Integer.class, fruta);
        int frutasConA = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ingredientes WHERE GRUPO = ? AND NOMBRE LIKE '%a%'", Integer.class, fruta);

        assertThat(get("/api/ingredientes?grupo=" + fruta))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(frutas))
                .hasPathSatisfying(
                        "$.contenido[*].grupo.nombre",
                        v -> v.assertThat().asArray().containsOnly("Fruta"));
        assertThat(get("/api/ingredientes?nombre=a&grupo=" + fruta))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(frutasConA));
    }

    @Test
    void losComodinesDeLikeSeBuscanLiteralmente() {
        for (String comodin : List.of("%", "_", "a_e")) {
            assertThat(mvc.get()
                            .uri("/api/ingredientes")
                            .param("nombre", comodin)
                            .header(HttpHeaders.AUTHORIZATION, tokens.usuario("prueba")))
                    .bodyJson()
                    .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
        }
    }

    @Test
    void unTamanoDePaginaFueraDeRangoDevuelve400() {
        assertThat(get("/api/ingredientes?size=0"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("VALIDACION"))
                .hasPathSatisfying(
                        "$.errores['size']", v -> v.assertThat().asString().contains("entre 1 y 100"));
        assertThat(get("/api/ingredientes?size=101")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(get("/api/ingredientes?page=-1")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void lasSugerenciasEmpiezanPorLasQueComienzanPorElTextoYSonComoMuchoDiez() {
        assertThat(get("/api/ingredientes/sugerencias?q=ac"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$", v -> v.assertThat().asArray().hasSizeLessThanOrEqualTo(10))
                .hasPathSatisfying("$[0].nombre", v -> v.assertThat().isEqualTo("Aceite de girasol"))
                .hasPathSatisfying("$[1].nombre", v -> v.assertThat().isEqualTo("Aceite de oliva"));
        assertThat(get("/api/ingredientes/sugerencias?q=a"))
                .bodyJson()
                .hasPathSatisfying("$", v -> v.assertThat().asArray().hasSize(10));
        assertThat(get("/api/ingredientes/sugerencias?q="))
                .bodyJson()
                .hasPathSatisfying("$", v -> v.assertThat().asArray().isEmpty());
    }

    @Test
    void cualquierUsuarioPuedeCrearUnIngrediente() {
        int legumbres = idGrupo("Legumbres");

        MvcTestResult creado =
                enviar("POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo(" Quinoa ", legumbres));

        assertThat(creado)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.nombre", v -> v.assertThat().isEqualTo("Quinoa"))
                .hasPathSatisfying("$.grupo.nombre", v -> v.assertThat().isEqualTo("Legumbres"))
                .hasPathSatisfying("$.grupo.racionesSemana", v -> v.assertThat().isEqualTo(4));
        assertThat(creado.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("/api/ingredientes/");
        assertThat(get("/api/ingredientes?nombre=quinoa"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(1));
    }

    @Test
    void sinElEmailConfirmadoNoSeCreanIngredientesSalvoSiEresAdministrador() {
        datos.usuario("novata");
        int legumbres = idGrupo("Legumbres");

        assertThat(enviar("POST", "/api/ingredientes", tokens.usuario("novata"), cuerpo("Quinoa", legumbres)))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("EMAIL_SIN_CONFIRMAR"));
        // La administración mantiene el catálogo aunque no haya confirmado su email (el administrador inicial)
        assertThat(enviar("POST", "/api/ingredientes", tokens.admin("gestora"), cuerpo("Quinoa", legumbres)))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void cadaUsuarioCreaComoMucho30IngredientesAlDia() {
        int legumbres = idGrupo("Legumbres");
        for (int i = 1; i <= IngredienteService.NUEVOS_AL_DIA; i++) {
            assertThat(enviar(
                            "POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo("Legumbre " + i, legumbres)))
                    .hasStatus(HttpStatus.CREATED);
        }

        MvcTestResult otro =
                enviar("POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo("Legumbre de más", legumbres));
        assertThat(otro)
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("LIMITE_INGREDIENTES"));
        assertThat(otro.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(get("/api/ingredientes?nombre=Legumbre de m"))
                .bodyJson()
                .hasPathSatisfying("$.totalElementos", v -> v.assertThat().isEqualTo(0));
    }

    @Test
    void noSePuedeCrearUnIngredienteRepetidoAunqueCambienMayusculasOAcentos() {
        int grupo = idGrupo("Esenciales");

        assertThat(enviar("POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo("aceite DE OLIVA", grupo)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.errores.nombre", v -> v.assertThat().isEqualTo(IngredienteService.NOMBRE_EN_USO));
        assertThat(enviar("POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo("Limon", idGrupo("Fruta"))))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void elGrupoDebeExistirYLosCamposSonObligatorios() {
        assertThat(enviar("POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo("Quinoa", 999)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying(
                        "$.errores.grupo", v -> v.assertThat().isEqualTo(IngredienteService.GRUPO_INEXISTENTE));
        assertThat(enviar("POST", "/api/ingredientes", tokens.usuario("prueba"), cuerpo(" ", null)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores", v -> v.assertThat().asMap().containsOnlyKeys("nombre", "grupo"));
    }

    @Test
    void soloUnAdministradorPuedeEditarIngredientes() {
        int pera = idIngrediente("Pera");
        int fruta = idGrupo("Fruta");

        assertThat(enviar(
                        "PUT",
                        "/api/ingredientes/" + pera,
                        tokens.usuario("prueba"),
                        cuerpo("Pera conferencia", fruta)))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.codigo", v -> v.assertThat().isEqualTo("ACCESO_DENEGADO"));

        assertThat(enviar(
                        "PUT", "/api/ingredientes/" + pera, tokens.admin("gestora"), cuerpo("Pera conferencia", fruta)))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.id", v -> v.assertThat().isEqualTo(pera))
                .hasPathSatisfying("$.nombre", v -> v.assertThat().isEqualTo("Pera conferencia"));
    }

    @Test
    void editarSinCambiarElNombreNoChocaConsigoMismo() {
        int pera = idIngrediente("Pera");

        assertThat(enviar(
                        "PUT", "/api/ingredientes/" + pera, tokens.admin("gestora"), cuerpo("pera", idGrupo("Fruta"))))
                .hasStatusOk();
    }

    @Test
    void editarUnIngredienteInexistenteOConElNombreDeOtroFalla() {
        int fruta = idGrupo("Fruta");

        assertThat(enviar("PUT", "/api/ingredientes/999999", tokens.admin("gestora"), cuerpo("Pera", fruta)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(enviar(
                        "PUT",
                        "/api/ingredientes/" + idIngrediente("Pera"),
                        tokens.admin("gestora"),
                        cuerpo("manzana", fruta)))
                .hasStatus(HttpStatus.CONFLICT);
    }

    private MvcTestResult get(String uri) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, tokens.usuario("prueba"))
                .exchange();
    }

    private MvcTestResult enviar(String metodo, String uri, String token, Map<String, Object> cuerpo) {
        var peticion = "POST".equals(metodo) ? mvc.post() : mvc.put();
        return peticion.uri(uri)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))
                .exchange();
    }

    private static Map<String, Object> cuerpo(String nombre, Integer grupo) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("nombre", nombre);
        cuerpo.put("grupo", grupo);
        return cuerpo;
    }

    private int idGrupo(String nombre) {
        return jdbc.queryForObject("SELECT ID FROM grupo_ingrediente WHERE NOMBRE = ?", Integer.class, nombre);
    }

    private int idIngrediente(String nombre) {
        return jdbc.queryForObject("SELECT ID FROM ingredientes WHERE NOMBRE = ?", Integer.class, nombre);
    }
}
