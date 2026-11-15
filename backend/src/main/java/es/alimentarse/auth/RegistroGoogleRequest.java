package es.alimentarse.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Lo que falta para terminar el registro con Google (D-085): el nombre de usuario, que es público, y los datos
 * personales, con las mismas reglas que el alta normal (6.9). El email y la cuenta de Google vienen del token.
 */
public record RegistroGoogleRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres")
        @Pattern(
                regexp = "[a-zA-Z0-9._-]*",
                message = "Solo se permiten letras sin tilde, números, punto, guion y guion bajo")
        String id,

        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El primer apellido es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
        String apellido1,

        @Size(max = 100, message = "Máximo 100 caracteres") @Nullable
        String apellido2) {}
