package es.alimentarse.receta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ingrediente de una receta con su cantidad en texto libre ("200 gramos", "1 cucharada").
 *
 * @param ingrediente id del ingrediente
 */
public record IngredienteCantidad(
        @NotNull(message = "Falta el ingrediente") Integer ingrediente,

        @NotBlank(message = "Indica la cantidad") @Size(max = 100, message = "Máximo 100 caracteres")
        String cantidad) {}
