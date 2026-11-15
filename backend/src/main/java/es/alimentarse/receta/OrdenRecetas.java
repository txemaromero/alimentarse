package es.alimentarse.receta;

/** Orden de los listados de recetas; el XML del mapper lo traduce a SQL con {@code <choose>} (nunca se concatena). */
public enum OrdenRecetas {
    /** Más recientes primero (D-031). */
    RECIENTES,
    /** Más «me gusta» primero y, a igualdad, más recientes. */
    MAS_VALORADAS,
    /** Por la fecha del «me gusta» del usuario actual, del más reciente al más antiguo (D-062). */
    ME_GUSTA_RECIENTES
}
