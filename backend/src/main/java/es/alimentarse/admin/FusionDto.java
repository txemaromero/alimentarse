package es.alimentarse.admin;

import es.alimentarse.ingrediente.IngredienteDto;

/**
 * Resultado de fusionar dos ingredientes (D-065).
 *
 * @param ingrediente el que queda
 * @param recetas cuántas recetas llevaban el ingrediente fusionado
 */
public record FusionDto(IngredienteDto ingrediente, int recetas) {}
