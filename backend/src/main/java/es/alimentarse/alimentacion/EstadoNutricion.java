package es.alimentarse.alimentacion;

/** Si un ingrediente cuenta en la estimación nutricional de la receta (D-079). */
public enum EstadoNutricion {
    /** Tiene composición y su cantidad se ha convertido a gramos. */
    CALCULADO,
    /** No tiene composición: BEDCA no tiene un alimento adecuado o es un ingrediente nuevo. */
    SIN_DATOS,
    /** Su cantidad no se puede convertir a gramos («al gusto», «1 pizca», «2-3 hojas»…). */
    SIN_PESO
}
