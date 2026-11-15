package es.alimentarse.consumo;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * «Añadir a consumidos» (6.7).
 *
 * @param receta id de la receta
 * @param fecha momento del consumo (ISO-8601 con zona); si no se indica, ahora. No puede ser futura
 */
public record ConsumoRequest(
        @NotNull(message = "Elige la receta") Integer receta,
        @Nullable Instant fecha) {}
