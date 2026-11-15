package es.alimentarse.receta;

import es.alimentarse.imagen.ImagenService;
import org.jspecify.annotations.Nullable;

/** Fila de {@code foto_receta} (D-083): el fichero, su posición en la galería (0, la principal) y su descripción. */
record FotoReceta(String nombre, int orden, @Nullable String descripcion) {

    FotoDto dto() {
        return new FotoDto(nombre, ImagenService.url(nombre), descripcion);
    }
}
