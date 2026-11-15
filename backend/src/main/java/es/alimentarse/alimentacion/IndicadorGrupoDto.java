package es.alimentarse.alimentacion;

import es.alimentarse.catalogo.GrupoDto;
import org.jspecify.annotations.Nullable;

/**
 * Indicador circular «Ingredientes en los últimos 7 días» de un grupo (6.4).
 *
 * @param consumidas raciones del grupo en los últimos 7 días
 * @param limite raciones semanales recomendadas; {@code null} = sin límite (∞)
 * @param porcentaje progreso del círculo, de 0 a 100 (100 si no hay límite)
 */
public record IndicadorGrupoDto(
        GrupoDto grupo, long consumidas, @Nullable Integer limite, int porcentaje, ColorIndicador color) {}
