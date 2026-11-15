package es.alimentarse.receta;

import jakarta.validation.constraints.NotNull;

public record PublicacionRequest(
        @NotNull(message = "Indica si la receta queda publicada")
        Boolean publicada) {}
