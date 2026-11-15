package es.alimentarse.alimentacion;

import es.alimentarse.receta.RecetaResumenDto;
import java.time.Instant;
import java.util.List;

/**
 * Una comida del planificador (D-068).
 *
 * @param disponible el usuario aún puede ver la receta; si no, la comida ni cuenta ni se puede consumir
 * @param excesos grupos de la receta que superarían su límite semanal ese día, contando esta comida
 */
public record ComidaPlanificadaDto(
        long id, Instant fecha, RecetaResumenDto receta, boolean disponible, List<IndicadorGrupoDto> excesos) {}
