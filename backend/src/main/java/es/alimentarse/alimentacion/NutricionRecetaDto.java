package es.alimentarse.alimentacion;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Estimación nutricional de una receta con los datos de BEDCA (D-079).
 *
 * @param total de la receta entera; {@code null} si ningún ingrediente cuenta
 * @param porRacion el total entre los comensales; {@code null} si ningún ingrediente cuenta
 * @param ingredientes todos los de la receta, con lo que aporta cada uno a la estimación
 */
public record NutricionRecetaDto(
        int comensales,
        @Nullable ValoresNutricionalesDto total,
        @Nullable ValoresNutricionalesDto porRacion,
        List<IngredienteNutricionDto> ingredientes) {}
