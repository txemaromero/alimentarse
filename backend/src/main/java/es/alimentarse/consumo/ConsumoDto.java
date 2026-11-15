package es.alimentarse.consumo;

import es.alimentarse.receta.RecetaResumenDto;
import java.time.Instant;

/** Consumo del histórico, con la tarjeta de la receta. */
public record ConsumoDto(long id, Instant fecha, RecetaResumenDto receta) {}
