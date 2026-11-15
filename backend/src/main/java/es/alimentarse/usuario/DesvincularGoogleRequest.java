package es.alimentarse.usuario;

import jakarta.validation.constraints.NotBlank;

/** Confirmación con la contraseña actual para dejar de entrar con Google (D-086). */
public record DesvincularGoogleRequest(
        @NotBlank(message = "Escribe tu contraseña para confirmar")
        String password) {}
