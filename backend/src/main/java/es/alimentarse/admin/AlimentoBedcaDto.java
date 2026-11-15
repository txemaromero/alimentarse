package es.alimentarse.admin;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Un alimento de BEDCA para asignarlo a un ingrediente (D-080): su composición por 100 g de porción comestible, con los
 * valores de BEDCA sin cambios (sin los ceros de relleno). {@code null} = BEDCA no da ese valor.
 *
 * @param id el identificador del alimento en BEDCA
 * @param energiaKcal la energía en kcal, calculada con la de kJ y redondeada
 */
public record AlimentoBedcaDto(
        int id,
        String nombre,
        @Nullable BigDecimal energiaKcal,
        @Nullable BigDecimal energiaKj,
        @Nullable BigDecimal grasas,
        @Nullable BigDecimal grasasSaturadas,
        @Nullable BigDecimal hidratos,
        @Nullable BigDecimal fibra,
        @Nullable BigDecimal proteinas,
        @Nullable BigDecimal sodioMg) {}
