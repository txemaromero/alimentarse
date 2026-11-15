package es.alimentarse.alimentacion;

import java.util.List;

/**
 * El planificador de un intervalo (D-068).
 *
 * @param comidas las comidas planificadas en el intervalo, por fecha
 * @param indicadores cómo quedarían los grupos al final del intervalo, con lo comido y lo planificado (6.4)
 */
public record PlanDto(List<ComidaPlanificadaDto> comidas, List<IndicadorGrupoDto> indicadores) {}
