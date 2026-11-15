package es.alimentarse.alimentacion;

/**
 * Lo que pide una receta de un ingrediente, en la lista de la compra (D-076).
 *
 * @param cantidad tal como la escribió su autor: texto libre («200 gramos», «al gusto»)
 * @param veces cuántas comidas del intervalo son de esta receta
 */
public record CantidadCompraDto(int receta, String titulo, String cantidad, int veces) {}
