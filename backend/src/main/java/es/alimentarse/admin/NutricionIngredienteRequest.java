package es.alimentarse.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Los datos nutricionales de un ingrediente (D-080, D-081): su alimento de BEDCA y los pesos de sus piezas. Sustituye a
 * los que tenía.
 *
 * @param alimento id del alimento en BEDCA; {@code null} para dejarlo sin alimento
 * @param piezas todas sus piezas; una lista vacía las quita
 */
public record NutricionIngredienteRequest(
        @Nullable Integer alimento,

        @NotNull(message = "Indica las piezas (puede ser una lista vacía)")
        @Size(max = 10, message = "Como mucho 10 piezas")
        List<@Valid @NotNull(message = "Falta la pieza") PiezaDto> piezas) {}
