package es.alimentarse.admin;

import org.jspecify.annotations.Nullable;

/**
 * Grupo de alimentos en la administración (D-065).
 *
 * @param racionesSemana raciones recomendadas por semana; {@code null} = sin límite
 * @param basico grupo básico (sal, aceite, especias…): no cuenta en «Ingredientes más usados» (D-054)
 * @param ingredientes cuántos ingredientes lo usan; solo se puede borrar si es 0
 */
public record GrupoAdminDto(
        int id, String nombre, @Nullable Integer racionesSemana, boolean basico, long ingredientes) {}
