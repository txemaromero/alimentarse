package es.alimentarse.admin;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Los datos nutricionales de un ingrediente, para editarlos (D-080, D-081).
 *
 * @param alimento su alimento de BEDCA con los valores por 100 g; {@code null} si no tiene
 * @param piezas los pesos de sus piezas, por orden alfabético
 */
public record NutricionIngredienteDto(@Nullable AlimentoBedcaDto alimento, List<PiezaDto> piezas) {}
