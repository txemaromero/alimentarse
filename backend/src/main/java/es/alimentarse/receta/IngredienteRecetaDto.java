package es.alimentarse.receta;

import es.alimentarse.catalogo.GrupoDto;

/** Ingrediente de una receta en el detalle: nombre, cantidad y grupo. */
public record IngredienteRecetaDto(int id, String nombre, String cantidad, GrupoDto grupo) {}
