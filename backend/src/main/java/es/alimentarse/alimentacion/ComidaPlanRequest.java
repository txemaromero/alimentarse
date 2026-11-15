package es.alimentarse.alimentacion;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Añadir una comida al planificador.
 *
 * @param receta id de la receta
 * @param fecha momento de la comida (ISO-8601 con zona); no puede haber pasado
 */
public record ComidaPlanRequest(
        @NotNull(message = "Elige la receta") Integer receta,
        @NotNull(message = "Elige el momento de la comida") Instant fecha) {}
