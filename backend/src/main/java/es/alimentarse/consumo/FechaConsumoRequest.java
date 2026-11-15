package es.alimentarse.consumo;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Cambio de fecha de un consumo del histórico (6.7). */
public record FechaConsumoRequest(
        @NotNull(message = "Indica la fecha y la hora") Instant fecha) {}
