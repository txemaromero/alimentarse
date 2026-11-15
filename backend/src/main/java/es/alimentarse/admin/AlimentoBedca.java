package es.alimentarse.admin;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Fila de {@code alimento_bedca}: un alimento de BEDCA con su composición por 100 g de porción comestible, sin cambios
 * (D-080). {@code null} = BEDCA no da ese valor.
 */
record AlimentoBedca(
        int id,
        String nombre,
        @Nullable BigDecimal energiaKj,
        @Nullable BigDecimal grasas,
        @Nullable BigDecimal grasasSaturadas,
        @Nullable BigDecimal hidratos,
        @Nullable BigDecimal fibra,
        @Nullable BigDecimal proteinas,
        @Nullable BigDecimal sodioMg) {}
