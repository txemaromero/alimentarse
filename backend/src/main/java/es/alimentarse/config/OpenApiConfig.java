package es.alimentarse.config;

import es.alimentarse.comun.error.ErrorRespuesta;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Descripción OpenAPI de la API: Swagger UI ({@code /api/docs}, solo en dev) y {@code docs/openapi.json}, del que se
 * generan los tipos del frontend (D-073). Los esquemas de los record los completa {@link EsquemasDeRecords}.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    OpenAPI documentacionApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Alimentarse (Toca, ¿Qué toca hoy?)")
                        .version("v1")
                        .description("Para probar los endpoints protegidos, inicia sesión con POST /api/auth/login."
                                + " Las lecturas ya funcionan con la cookie de sesión. Para escribir, copia el valor"
                                + " de la cookie toca_sesion (herramientas de desarrollo del navegador → Aplicación →"
                                + " Cookies) y pégalo en «Authorize»: con la cabecera Authorization no hace falta el"
                                + " token CSRF."))
                // Fijo: si no, springdoc pone la URL de la petición y docs/openapi.json cambiaría según el entorno
                .servers(List.of(new Server().url("/").description("Este servidor")))
                .components(new Components()
                        .addSecuritySchemes(
                                ESQUEMA,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }

    /**
     * {@code operationId} único y estable, con el recurso delante ({@code recetaListar}). Con el nombre del método
     * solo, springdoc numera los repetidos ({@code listar_1}) según el orden en que encuentra los controladores.
     */
    @Bean
    OperationCustomizer operacionesConRecurso() {
        return (operacion, metodo) -> {
            String recurso = metodo.getBeanType().getSimpleName().replace("Controller", "");
            operacion.setOperationId(StringUtils.uncapitalize(recurso)
                    + StringUtils.capitalize(metodo.getMethod().getName()));
            return operacion;
        };
    }

    /** El cuerpo de los errores no sale en ninguna operación (lo escribe el manejador de errores), pero el frontend lo usa. */
    @Bean
    OpenApiCustomizer esquemaDeLosErrores() {
        return api -> ModelConverters.getInstance(true)
                .readAll(new AnnotatedType(ErrorRespuesta.class))
                .forEach(api.getComponents()::addSchemas);
    }
}
