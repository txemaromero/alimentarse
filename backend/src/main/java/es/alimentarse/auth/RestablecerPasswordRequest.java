package es.alimentarse.auth;

import es.alimentarse.seguridad.CabeEnBcrypt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param token el que llega en el enlace del correo de recuperación */
public record RestablecerPasswordRequest(
        @NotBlank(message = "Falta el token del enlace") String token,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @CabeEnBcrypt(
                message =
                        "La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter")
        String password) {}
