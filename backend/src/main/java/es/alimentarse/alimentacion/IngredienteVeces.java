package es.alimentarse.alimentacion;

import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.ingrediente.IngredienteDto;
import org.jspecify.annotations.Nullable;

/**
 * Fila plana de «ingredientes más usados». Plana a propósito: un resultMap con un objeto anidado y sin {@code idArg}
 * propio hace que MyBatis fusione las filas con el mismo número de veces.
 */
record IngredienteVeces(
        int id,
        String nombre,
        int grupoId,
        String grupoNombre,
        Integer racionesSemana,
        @Nullable String alimento,
        long veces) {

    IngredienteUsadoDto aDto() {
        return new IngredienteUsadoDto(
                new IngredienteDto(id, nombre, new GrupoDto(grupoId, grupoNombre, racionesSemana), alimento), veces);
    }
}
