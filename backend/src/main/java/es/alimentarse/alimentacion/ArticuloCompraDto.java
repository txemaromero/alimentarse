package es.alimentarse.alimentacion;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Un ingrediente de la lista de la compra (D-076).
 *
 * @param total lo que hay que comprar: la suma de las cantidades si todas son un número con la misma unidad, o la
 *     cantidad tal cual si solo la pide una comida; {@code null} si no se puede sumar
 * @param cantidades lo que pide cada receta, por título
 */
public record ArticuloCompraDto(
        int ingrediente, String nombre, @Nullable String total, List<CantidadCompraDto> cantidades) {}
