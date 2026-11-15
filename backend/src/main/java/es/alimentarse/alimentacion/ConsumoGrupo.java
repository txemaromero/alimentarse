package es.alimentarse.alimentacion;

import java.time.Instant;

/**
 * Un consumo y uno de los grupos de alimentos de su receta. Un consumo de una receta con ingredientes de tres grupos
 * produce tres filas.
 */
public record ConsumoGrupo(long consumo, Instant fecha, int grupo) {}
