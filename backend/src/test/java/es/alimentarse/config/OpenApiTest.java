package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import es.alimentarse.support.TestIntegracion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code docs/openapi.json} tiene que coincidir con la API: de él salen los tipos del frontend (D-073). Si no coincide,
 * el test lo regenera y falla, para que el cambio se revise y se suba junto con los tipos regenerados.
 */
@TestIntegracion
class OpenApiTest {

    /** Maven ejecuta los tests desde backend/. */
    private static final Path FICHERO = Path.of("..", "docs", "openapi.json");

    @Autowired
    MockMvcTester mvc;

    @Test
    void docsOpenapiJsonEstaAlDia() throws IOException {
        String actual = descripcion();
        String guardado = Files.exists(FICHERO) ? Files.readString(FICHERO).replace("\r\n", "\n") : "";
        if (!actual.equals(guardado)) {
            Files.writeString(FICHERO, actual);
            fail("docs/openapi.json no estaba al día y se ha regenerado. Revisa el cambio, regenera los tipos del"
                    + " frontend (npm run api, en frontend/) y vuelve a pasar los tests.");
        }
    }

    @Test
    void losOperationIdNoDependenDelOrdenDeLosControladores() throws IOException {
        // springdoc numera los repetidos (listar_1) según el orden en que encuentra los controladores
        assertThat(descripcion()).doesNotContainPattern("\"operationId\" : \"\\w+_\\d+\"");
    }

    @Test
    void todosLosCamposSonObligatoriosYLosQuePuedenSerNullLoDicen() throws IOException {
        JsonNode esquemas =
                new JsonMapper().readTree(descripcion()).path("components").path("schemas");

        // Todos los esquemas son record de la aplicación: Jackson escribe siempre todos sus campos (EsquemasDeRecords)
        for (Map.Entry<String, JsonNode> esquema : esquemas.properties()) {
            List<String> campos = esquema.getValue().path("properties").properties().stream()
                    .map(Map.Entry::getKey)
                    .toList();
            List<String> obligatorios = new ArrayList<>();
            esquema.getValue().path("required").forEach(campo -> obligatorios.add(campo.asString()));
            assertThat(obligatorios).as(esquema.getKey()).containsExactlyInAnyOrderElementsOf(campos);
        }
        JsonNode tarjeta = esquemas.path("RecetaResumenDto").path("properties");
        assertThat(tarjeta.path("imagen").path("type").toString()).isEqualTo("[\"string\",\"null\"]");
        assertThat(tarjeta.path("titulo").path("type").toString()).isEqualTo("\"string\"");
    }

    private String descripcion() throws IOException {
        MvcTestResult resultado = mvc.get().uri("/api/docs/openapi").exchange();
        assertThat(resultado).hasStatusOk();
        return resultado
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8)
                        .replace("\r\n", "\n")
                        .strip() + "\n";
    }
}
