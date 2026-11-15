package es.alimentarse.alimentacion;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Cómo cuenta un ingrediente en la estimación nutricional de la receta (D-079).
 *
 * @param cantidad la de la receta, tal cual
 * @param gramos los que se han contado, con un decimal; {@code null} si no cuenta
 * @param alimento el alimento de BEDCA con el que se calcula; {@code null} si no tiene composición
 */
public record IngredienteNutricionDto(
        int id,
        String nombre,
        String cantidad,
        @Nullable BigDecimal gramos,
        @Nullable String alimento,
        EstadoNutricion estado) {}
