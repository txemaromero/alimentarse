package es.alimentarse.alimentacion;

import java.util.List;

/**
 * Lista de la compra de las comidas planificadas de un intervalo (D-076).
 *
 * @param comidas las comidas que entran en la lista, por fecha: las del intervalo que aún no han pasado y cuya receta
 *     sigue disponible
 * @param secciones los ingredientes, por grupo de alimentos; los grupos básicos (sal, aceite…), al final
 * @param noDisponibles comidas que no entran porque su receta ya no está disponible
 */
public record ListaCompraDto(List<ComidaCompraDto> comidas, List<SeccionCompraDto> secciones, int noDisponibles) {}
