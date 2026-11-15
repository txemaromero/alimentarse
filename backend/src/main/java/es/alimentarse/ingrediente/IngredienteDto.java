package es.alimentarse.ingrediente;

import es.alimentarse.catalogo.GrupoDto;
import org.jspecify.annotations.Nullable;

/**
 * Ingrediente con su grupo (y las raciones semanales del grupo, para la tarjeta del listado).
 *
 * @param alimento nombre del alimento de BEDCA con el que se calcula su información nutricional (D-079, D-080);
 *     {@code null} si no tiene
 */
public record IngredienteDto(
        int id, String nombre, GrupoDto grupo, @Nullable String alimento) {}
