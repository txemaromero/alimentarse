package es.alimentarse.alimentacion;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import es.alimentarse.receta.RecetaResumenDto;
import java.util.List;

/**
 * Tarjeta de una receta recomendada: los datos de siempre, en el mismo nivel del JSON, y cómo van esta semana sus
 * grupos de alimentos, para explicar por qué se recomienda (D-067).
 *
 * @param grupos los grupos de la receta, sin los básicos sin límite; primero los que tienen límite, de más a menos
 *     llenos
 */
public record RecetaRecomendadaDto(@JsonUnwrapped RecetaResumenDto receta, List<IndicadorGrupoDto> grupos) {}
