package es.alimentarse.alimentacion;

import es.alimentarse.catalogo.GrupoDto;

/**
 * Fila plana: un grupo de alimentos presente en una receta (D-040).
 *
 * @param basico grupo de ingredientes presentes en casi todas las recetas: sal, aceite, especias… (D-054)
 */
record GrupoDeReceta(int receta, int grupoId, String grupoNombre, Integer racionesSemana, boolean basico) {

    GrupoDto grupo() {
        return new GrupoDto(grupoId, grupoNombre, racionesSemana);
    }

    /** ¿Dice algo de la alimentación de la semana? Un grupo básico sin límite, no. */
    boolean relevante() {
        return racionesSemana != null || !basico;
    }
}
