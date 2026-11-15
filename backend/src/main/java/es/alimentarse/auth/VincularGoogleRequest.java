package es.alimentarse.auth;

import jakarta.validation.constraints.NotBlank;

/** La contraseña de la cuenta de Toca que se vincula con Google (D-086). */
public record VincularGoogleRequest(
        @NotBlank(message = "Escribe tu contraseña de Toca") String password) {}
