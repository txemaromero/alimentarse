package es.alimentarse.alimentacion;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Fila plana: un ingrediente de una receta con su cantidad y, si la tiene, su composición de BEDCA por 100 g de
 * porción comestible (D-079). Un valor {@code null} es un dato que BEDCA no da.
 *
 * @param alimento nombre del alimento de BEDCA; {@code null} si el ingrediente no tiene composición
 */
record IngredienteNutricion(
        int receta,
        int ingrediente,
        String nombre,
        String cantidad,
        @Nullable String alimento,
        @Nullable BigDecimal energiaKj,
        @Nullable BigDecimal grasas,
        @Nullable BigDecimal grasasSaturadas,
        @Nullable BigDecimal hidratos,
        @Nullable BigDecimal fibra,
        @Nullable BigDecimal proteinas,
        @Nullable BigDecimal sodioMg) {}
