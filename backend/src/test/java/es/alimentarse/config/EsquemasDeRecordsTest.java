package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class EsquemasDeRecordsTest {

    record Grupo(int id, String nombre, @Nullable Integer raciones) {}

    record Tarjeta(
            int id,
            @Nullable String imagen,
            Grupo grupo,
            @Nullable Grupo otro,
            List<String> etiquetas) {}

    record Recomendada(
            @JsonUnwrapped Tarjeta tarjeta, @Nullable String motivo) {}

    @Test
    void todosLosCamposDeUnRecordSonObligatorios() {
        Map<String, Schema> esquemas = esquemas(Grupo.class);

        assertThat(esquemas.get("Grupo").getRequired()).containsExactlyInAnyOrder("id", "nombre", "raciones");
    }

    @Test
    void losAnotadosConNullableAdmitenNull() {
        Map<String, Schema> propiedades = esquemas(Grupo.class).get("Grupo").getProperties();

        assertThat(propiedades.get("raciones").getTypes()).containsExactlyInAnyOrder("integer", "null");
        assertThat(propiedades.get("nombre").getTypes()).containsExactly("string");
    }

    @Test
    void unObjetoNullableEsEseObjetoONull() {
        Map<String, Schema> esquemas = esquemas(Tarjeta.class);
        Map<String, Schema> propiedades = esquemas.get("Tarjeta").getProperties();

        assertThat(propiedades.get("grupo").get$ref()).isEqualTo("#/components/schemas/Grupo");
        List<Schema> alternativas = propiedades.get("otro").getAnyOf();
        assertThat(alternativas).hasSize(2);
        assertThat(alternativas.get(0).get$ref()).isEqualTo("#/components/schemas/Grupo");
        assertThat(alternativas.get(1).getTypes()).containsExactly("null");
        // El record anidado también se completa
        assertThat(esquemas.get("Grupo").getRequired()).containsExactlyInAnyOrder("id", "nombre", "raciones");
    }

    @Test
    void losCamposDeUnJsonUnwrappedVanAlMismoNivel() {
        Schema<?> recomendada = esquemas(Recomendada.class).get("Recomendada");

        assertThat(recomendada.getRequired())
                .containsExactlyInAnyOrder("id", "imagen", "grupo", "otro", "etiquetas", "motivo");
        assertThat(((Schema<?>) recomendada.getProperties().get("imagen")).getTypes())
                .containsExactlyInAnyOrder("string", "null");
    }

    @Test
    void esIdempotente() {
        ModelConverters convertidores = convertidores();
        convertidores.readAll(new AnnotatedType(Grupo.class));
        Map<String, Schema> otraVez = convertidores.readAll(new AnnotatedType(Grupo.class));

        assertThat(otraVez.get("Grupo").getRequired()).containsExactlyInAnyOrder("id", "nombre", "raciones");
        assertThat(((Schema<?>) otraVez.get("Grupo").getProperties().get("raciones")).getTypes())
                .containsExactlyInAnyOrder("integer", "null");
    }

    private static Map<String, Schema> esquemas(Class<?> clase) {
        return convertidores().readAll(new AnnotatedType(clase));
    }

    /** Como springdoc: OpenAPI 3.1 y este conversor delante de los de swagger-core. */
    private static ModelConverters convertidores() {
        ModelConverters convertidores = new ModelConverters(true);
        convertidores.addConverter(new EsquemasDeRecords());
        return convertidores;
    }
}
