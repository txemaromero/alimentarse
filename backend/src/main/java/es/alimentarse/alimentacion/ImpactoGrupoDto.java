package es.alimentarse.alimentacion;

/**
 * Cómo cambiaría un grupo con límite si el usuario comiera una receta ahora (D-067).
 *
 * @param antes raciones de los últimos 7 días
 * @param despues raciones contando esa receta
 */
public record ImpactoGrupoDto(IndicadorGrupoDto antes, IndicadorGrupoDto despues) {}
