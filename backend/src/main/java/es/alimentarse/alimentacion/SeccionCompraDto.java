package es.alimentarse.alimentacion;

import es.alimentarse.catalogo.GrupoDto;
import java.util.List;

/**
 * Los ingredientes de un grupo de alimentos en la lista de la compra (D-076).
 *
 * @param basico grupo de ingredientes que suele haber en casa: sal, aceite, especias… (D-054)
 */
public record SeccionCompraDto(GrupoDto grupo, boolean basico, List<ArticuloCompraDto> articulos) {}
