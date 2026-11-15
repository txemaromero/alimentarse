package es.alimentarse.alimentacion;

import es.alimentarse.ingrediente.IngredienteDto;

/** Ingrediente y número de consumos del usuario cuya receta lo lleva. */
public record IngredienteUsadoDto(IngredienteDto ingrediente, long veces) {}
