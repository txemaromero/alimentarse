package es.alimentarse.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El primer apellido es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
        String apellido1,

        @Size(max = 100, message = "Máximo 100 caracteres") @Nullable
        String apellido2,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String email,

        /** La contraseña actual: obligatoria solo si cambia el email (D-086). */
        @Nullable String password) {}
