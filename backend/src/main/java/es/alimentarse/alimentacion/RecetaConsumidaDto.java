package es.alimentarse.alimentacion;

import es.alimentarse.receta.RecetaResumenDto;

/** Receta y número de veces que el usuario la ha consumido. */
public record RecetaConsumidaDto(RecetaResumenDto receta, long veces) {}
