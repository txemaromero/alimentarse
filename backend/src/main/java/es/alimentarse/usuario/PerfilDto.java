package es.alimentarse.usuario;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Datos del usuario que se devuelven al propio usuario (nunca incluye la contraseña). */
public record PerfilDto(
        String id,
        String nombre,
        String apellido1,
        @Nullable String apellido2,
        String email,

        @ArraySchema(schema = @Schema(allowableValues = {"USER", "ADMIN"}))
        List<String> roles,

        /** Si puede entrar también con su cuenta de Google (D-085). */
        boolean google,

        /** Si ha confirmado su email (D-088). */
        boolean emailVerificado,

        /** El email nuevo que espera su confirmación; mientras, sigue valiendo {@code email} (D-088). */
        @Nullable String emailPendiente) {

    public static PerfilDto de(Usuario usuario, List<String> roles) {
        return new PerfilDto(
                usuario.id(),
                usuario.nombre(),
                usuario.apellido1(),
                usuario.apellido2(),
                usuario.email(),
                roles,
                usuario.googleId() != null,
                usuario.emailVerificado(),
                usuario.emailPendiente());
    }
}
