package es.alimentarse.admin;

import jakarta.validation.constraints.NotNull;

/** @param admin {@code true} para dar el rol ADMIN, {@code false} para quitarlo */
public record CambioAdminRequest(
        @NotNull(message = "Indica si el usuario debe ser administrador")
        Boolean admin) {}
