package es.alimentarse.comun.error;

import java.util.Map;

/**
 * Cuerpo JSON de todas las respuestas de error de la API (sección 6.9).
 *
 * @param codigo identificador estable del tipo de error, por ejemplo {@code VALIDACION}
 * @param mensaje texto para el usuario, en español
 * @param errores mensaje de error de cada campo del formulario; vacío si no aplica
 */
public record ErrorRespuesta(String codigo, String mensaje, Map<String, String> errores) {

    public ErrorRespuesta {
        errores = errores == null ? Map.of() : Map.copyOf(errores);
    }

    public static ErrorRespuesta de(String codigo, String mensaje) {
        return new ErrorRespuesta(codigo, mensaje, Map.of());
    }
}
