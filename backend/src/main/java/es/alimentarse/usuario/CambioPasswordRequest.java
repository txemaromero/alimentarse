package es.alimentarse.usuario;

import es.alimentarse.seguridad.CabeEnBcrypt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        @NotBlank(message = "Introduce tu contraseña actual")
        String actual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @CabeEnBcrypt(
                message =
                        "La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter")
        String nueva) {}
