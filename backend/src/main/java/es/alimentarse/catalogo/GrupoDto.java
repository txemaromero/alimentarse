package es.alimentarse.catalogo;

import org.jspecify.annotations.Nullable;

/**
 * Grupo de alimentos.
 *
 * @param racionesSemana raciones recomendadas por semana; {@code null} = sin límite (∞)
 */
public record GrupoDto(int id, String nombre, @Nullable Integer racionesSemana) {}
