package es.alimentarse.admin;

import jakarta.validation.constraints.NotNull;

/** Bloquear o desbloquear a un usuario (D-063). */
public record CambioBloqueoRequest(
        @NotNull(message = "Indica si el usuario queda bloqueado")
        Boolean bloqueado) {}
