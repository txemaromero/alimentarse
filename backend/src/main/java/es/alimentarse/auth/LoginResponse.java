package es.alimentarse.auth;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta del login: los datos mínimos para la cabecera de la aplicación. El JWT va en una cookie {@code HttpOnly}
 * (D-070).
 *
 * @param caduca cuándo termina la sesión
 */
public record LoginResponse(Instant caduca, UsuarioSesion usuario) {

    public record UsuarioSesion(
            String id,
            String nombre,

            @ArraySchema(schema = @Schema(allowableValues = {"USER", "ADMIN"}))
            List<String> roles,

            /** Si ha confirmado su email: sin confirmar, la interfaz lo recuerda (D-088). */
            boolean emailVerificado) {}
}
