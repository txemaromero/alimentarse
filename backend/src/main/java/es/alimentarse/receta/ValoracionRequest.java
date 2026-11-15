package es.alimentarse.receta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * La valoración propia de una receta (D-077): una nota de 1 a 5 y un comentario opcional, en texto plano.
 *
 * @param comentario {@code null} o en blanco si no hay
 */
public record ValoracionRequest(
        @NotNull(message = "Elige una nota")
        @Min(value = 1, message = "La nota va de 1 a 5")
        @Max(value = 5, message = "La nota va de 1 a 5")
        Integer nota,

        @Nullable @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
        String comentario) {}
