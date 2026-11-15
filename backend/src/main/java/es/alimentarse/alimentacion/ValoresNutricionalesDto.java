package es.alimentarse.alimentacion;

import java.math.BigDecimal;

/**
 * Valores nutricionales estimados (D-079), redondeados para mostrarlos: la energía sin decimales, la sal con dos y el
 * resto con uno.
 *
 * @param sal en gramos, calculada a partir del sodio (sal = sodio × 2,5)
 */
public record ValoresNutricionalesDto(
        BigDecimal energiaKcal,
        BigDecimal energiaKj,
        BigDecimal grasas,
        BigDecimal grasasSaturadas,
        BigDecimal hidratos,
        BigDecimal fibra,
        BigDecimal proteinas,
        BigDecimal sal) {}
