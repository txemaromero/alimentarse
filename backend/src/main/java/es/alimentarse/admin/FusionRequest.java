package es.alimentarse.admin;

import jakarta.validation.constraints.NotNull;

/** Fusionar un ingrediente duplicado con otro (D-065). */
public record FusionRequest(
        @NotNull(message = "Elige el ingrediente con el que se fusiona")
        Integer destino) {}
