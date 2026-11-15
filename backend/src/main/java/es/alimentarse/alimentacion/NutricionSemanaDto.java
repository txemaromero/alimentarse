package es.alimentarse.alimentacion;

import org.jspecify.annotations.Nullable;

/**
 * Nutrientes estimados de lo que el usuario ha comido en los últimos 7 días (D-082): una ración de la receta de cada
 * consumo, con la estimación de D-079.
 *
 * @param consumos consumos registrados en los últimos 7 días
 * @param estimados de ellos, los de recetas con estimación (algún ingrediente cuenta)
 * @param total la suma de los 7 días; {@code null} si ningún consumo tiene estimación
 * @param mediaDiaria el total entre 7; {@code null} si ningún consumo tiene estimación
 * @param referencia la media diaria en porcentaje de la ingesta de referencia; {@code null} si no hay estimación
 */
public record NutricionSemanaDto(
        int consumos,
        int estimados,
        @Nullable ValoresNutricionalesDto total,
        @Nullable ValoresNutricionalesDto mediaDiaria,
        @Nullable IngestaReferenciaDto referencia) {}
