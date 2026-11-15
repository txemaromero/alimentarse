package es.alimentarse.cuenta;

import jakarta.validation.constraints.NotBlank;

/** Confirmación de la baja con la contraseña actual (D-064). */
public record BajaRequest(
        @NotBlank(message = "Escribe tu contraseña para confirmar")
        String password) {}
