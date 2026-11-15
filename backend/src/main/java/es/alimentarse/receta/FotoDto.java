package es.alimentarse.receta;

import org.jspecify.annotations.Nullable;

/**
 * Una foto de la galería de una receta (D-083).
 *
 * @param nombre su nombre, para conservarla al editar la receta ({@code existente} de {@link FotoRequest})
 * @param url la URL de la imagen
 * @param descripcion su texto alternativo, o {@code null}
 */
public record FotoDto(String nombre, String url, @Nullable String descripcion) {}
