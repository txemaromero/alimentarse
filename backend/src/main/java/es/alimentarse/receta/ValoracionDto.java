package es.alimentarse.receta;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Una valoración de una receta (D-077).
 *
 * @param usuario id de quien la escribió
 * @param comentario texto plano, o {@code null}
 * @param fecha la de la última vez que se guardó
 * @param oculta la ha ocultado la administración: solo la ven su autor y los administradores, y no cuenta en la media
 */
public record ValoracionDto(
        long id, String usuario, int nota, @Nullable String comentario, Instant fecha, boolean oculta) {}
