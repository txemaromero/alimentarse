package es.alimentarse.admin;

import jakarta.validation.constraints.NotNull;

/** Ocultar una valoración o volver a mostrarla (D-077). */
public record OcultarValoracionRequest(
        @NotNull(message = "Indica si se oculta o no") Boolean oculta) {}
