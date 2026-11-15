package es.alimentarse.receta;

/**
 * Media (con un decimal, o {@code null} si no hay ninguna) y número de las valoraciones visibles de una receta.
 */
public record NotaMedia(Double media, long total) {}
