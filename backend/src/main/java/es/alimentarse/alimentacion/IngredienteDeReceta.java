package es.alimentarse.alimentacion;

import es.alimentarse.catalogo.GrupoDto;

/**
 * Fila plana: un ingrediente de una receta, con su cantidad y su grupo de alimentos, para la lista de la compra (D-076).
 *
 * @param basico grupo de ingredientes presentes en casi todas las recetas: sal, aceite, especias… (D-054)
 */
record IngredienteDeReceta(
        int receta,
        int ingrediente,
        String nombre,
        String cantidad,
        int grupoId,
        String grupoNombre,
        Integer racionesSemana,
        boolean basico) {

    GrupoDto grupo() {
        return new GrupoDto(grupoId, grupoNombre, racionesSemana);
    }
}
