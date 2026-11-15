package es.alimentarse.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OlvidoPasswordRequest(
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido")
        String email) {}
