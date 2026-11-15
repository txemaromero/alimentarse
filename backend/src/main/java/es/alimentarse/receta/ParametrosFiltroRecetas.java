package es.alimentarse.receta;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Filtros del listado de recetas tal como llegan en la URL (6.5). {@code ingredientes} admite {@code 1,2} o el
 * parámetro repetido. Los textos y la lista tienen tope (D-090): son {@code LIKE} y un {@code IN (…)} en cada consulta.
 *
 * @param titulo como mucho el largo de un título
 * @param usuario como mucho el largo de un nombre de usuario
 * @param comensales recetas para al menos estos comensales
 * @param tiempoMax recetas de como mucho estos minutos
 * @param ingredientes ids de ingredientes; la receta debe contenerlos todos, y no tiene más de 50
 */
public record ParametrosFiltroRecetas(
        @Size(max = 100, message = "Busca con 100 caracteres como mucho")
        String titulo,

        Integer tipo,

        @Size(max = 30, message = "Un nombre de usuario tiene como mucho 30 caracteres")
        String usuario,

        String dificultad,

        @Min(value = 1, message = "Indica al menos 1 comensal")
        Integer comensales,

        @Min(value = 1, message = "El tiempo máximo debe ser de al menos 1 minuto")
        Integer tiempoMax,

        @Size(max = 50, message = "Filtra por 50 ingredientes como mucho")
        List<Integer> ingredientes) {}
