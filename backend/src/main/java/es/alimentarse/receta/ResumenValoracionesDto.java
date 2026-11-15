package es.alimentarse.receta;

import org.jspecify.annotations.Nullable;

/**
 * Cómo queda la valoración de una receta después de cambiar la propia (D-077).
 *
 * @param nota la media de las valoraciones visibles, con un decimal, o {@code null} si no hay ninguna
 * @param valoraciones cuántas valoraciones visibles tiene
 * @param mia la del usuario actual, o {@code null}
 */
public record ResumenValoracionesDto(
        @Nullable Double nota, long valoraciones, @Nullable ValoracionDto mia) {}
