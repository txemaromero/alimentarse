package es.alimentarse.auth;

import jakarta.validation.constraints.NotBlank;

/** @param usuario nombre de usuario o email */
public record LoginRequest(
        @NotBlank(message = "Introduce tu usuario o email") String usuario,
        @NotBlank(message = "Introduce la contraseña") String password) {}
