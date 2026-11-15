package es.alimentarse.receta;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Una foto de la galería de la receta, en el orden en que debe quedar (D-083): una que ya tenía o un fichero nuevo de
 * la petición. Una de las dos, no las dos.
 *
 * @param existente el {@code nombre} de una foto que ya tiene la receta
 * @param nueva la posición del fichero entre las partes {@code fotos} de la petición, desde 0
 * @param descripcion el texto alternativo de la foto; {@code null} o vacía, ninguna
 */
public record FotoRequest(
        @Nullable String existente,

        @Nullable @Min(value = 0, message = "La posición del fichero no puede ser negativa")
        Integer nueva,

        @Nullable @Size(max = 150, message = "Como mucho 150 caracteres")
        String descripcion) {}
