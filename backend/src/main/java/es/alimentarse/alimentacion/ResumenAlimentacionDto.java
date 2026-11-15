package es.alimentarse.alimentacion;

import es.alimentarse.consumo.ConsumoDto;
import java.util.List;

/**
 * Pantalla «Mi alimentación» (7).
 *
 * @param ultimas los 3 últimos consumos
 * @param masConsumidas las 3 recetas más consumidas (todo el historial)
 * @param ingredientesMasUsados los 4 ingredientes más usados (todo el historial)
 * @param grupos7Dias indicadores de los grupos consumidos en los últimos 7 días (6.4)
 */
public record ResumenAlimentacionDto(
        List<ConsumoDto> ultimas,
        List<RecetaConsumidaDto> masConsumidas,
        List<IngredienteUsadoDto> ingredientesMasUsados,
        List<IndicadorGrupoDto> grupos7Dias) {}
