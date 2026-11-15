package es.alimentarse.config;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Completa el esquema OpenAPI de los {@code record} de la aplicación (D-073).
 *
 * <p>Jackson escribe siempre todos los campos de un record, también los nulos, así que todos son obligatorios, y solo
 * los anotados con {@link Nullable} admiten {@code null}. Sin esto, springdoc solo marca como obligatorios los campos
 * con {@code @NotNull} o {@code @NotBlank}, y en los tipos generados del frontend todo sería opcional.
 *
 * <p>springdoc registra este conversor por ser un bean.
 */
@Component
public class EsquemasDeRecords implements ModelConverter {

    private static final String PAQUETE = "es.alimentarse.";
    private static final String PREFIJO_REFERENCIA = "#/components/schemas/";
    private static final String NULL = "null";

    @Override
    public @Nullable Schema<?> resolve(
            AnnotatedType tipo, ModelConverterContext contexto, Iterator<ModelConverter> siguientes) {
        if (!siguientes.hasNext()) {
            return null;
        }
        Schema<?> esquema = siguientes.next().resolve(tipo, contexto, siguientes);
        Class<?> clase = Json.mapper().constructType(tipo.getType()).getRawClass();
        if (esquema == null || !clase.isRecord() || !clase.getName().startsWith(PAQUETE)) {
            return esquema;
        }
        Schema<?> modelo = esquema.get$ref() == null
                ? esquema
                : contexto.getDefinedModels().get(esquema.get$ref().substring(PREFIJO_REFERENCIA.length()));
        if (modelo != null && modelo.getProperties() != null) {
            completar(modelo, clase);
        }
        return esquema;
    }

    /** Marca como obligatorios todos los campos del record y deja que admitan null los anotados con {@link Nullable}. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void completar(Schema<?> modelo, Class<?> record) {
        Map<String, Schema> propiedades = modelo.getProperties();
        List<String> obligatorios = new ArrayList<>();
        campos(record, new LinkedHashMap<>()).forEach((nombre, admiteNull) -> {
            Schema propiedad = propiedades.get(nombre);
            if (propiedad != null) {
                obligatorios.add(nombre);
                if (admiteNull) {
                    propiedades.put(nombre, conNull(propiedad));
                }
            }
        });
        modelo.setRequired(obligatorios);
    }

    /** Campos que escribe Jackson (los de un {@code @JsonUnwrapped} van al mismo nivel), con si admiten null. */
    private static Map<String, Boolean> campos(Class<?> record, Map<String, Boolean> campos) {
        for (RecordComponent componente : record.getRecordComponents()) {
            if (componente.getAccessor().isAnnotationPresent(JsonUnwrapped.class)
                    && componente.getType().isRecord()) {
                campos(componente.getType(), campos);
            } else {
                campos.put(componente.getName(), componente.getAnnotatedType().isAnnotationPresent(Nullable.class));
            }
        }
        return campos;
    }

    /** OpenAPI 3.1: {@code null} es un tipo más; un objeto anidado pasa a ser «este objeto o null». */
    private static Schema<?> conNull(Schema<?> propiedad) {
        if (propiedad.get$ref() != null) {
            return new JsonSchema()
                    .anyOf(List.of(propiedad, new JsonSchema().types(new LinkedHashSet<>(List.of(NULL)))));
        }
        if (propiedad.getTypes() != null) {
            propiedad.addType(NULL);
        }
        return propiedad;
    }
}
