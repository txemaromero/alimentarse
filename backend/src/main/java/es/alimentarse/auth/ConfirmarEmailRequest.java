package es.alimentarse.auth;

import jakarta.validation.constraints.NotBlank;

/** El token del enlace para confirmar el email (D-088). */
public record ConfirmarEmailRequest(
        @NotBlank(message = "Falta el token del enlace") String token) {}
