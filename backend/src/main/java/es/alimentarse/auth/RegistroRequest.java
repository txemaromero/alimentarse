package es.alimentarse.auth;

import es.alimentarse.seguridad.CabeEnBcrypt;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Alta de usuario (6.9). La contraseña tiene un máximo de 72 caracteres porque BCrypt ignora el resto (D-024), y de 72
 * bytes, porque no cifra más (D-090).
 */
public record RegistroRequest(
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
        String apellido2,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @CabeEnBcrypt(
                message =
                        "La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter")
        String password) {}
